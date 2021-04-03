package com.example.android_terminal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RealActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    Button back,stop;
    Switch rr_sw, er_sw;
    Boolean sim_run=true;
    Boolean stop_pressed=false;
    double sim_time=5;
    String finish_msg ="Not Available";

    private GoogleMap mMap;
    List<Marker> Real_route=new ArrayList<>(); //list with all the  markers of real route
    List<Marker> Est_route=new ArrayList<>(); //list with all the  markers of estimated route

    String ip;
    String port;
    String time;
    String id;
    String path;
    String vector;

    Thread myThread=null;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 2000;
    MqttAndroidClient client;

    String url;
    String topic;
    boolean given_sim_time;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real);
        back = findViewById(R.id.back);
        stop = findViewById(R.id.stop);
        rr_sw = findViewById(R.id.rr_sw);
        er_sw = findViewById(R.id.er_sw);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Data from MainActivity
        Intent intent=getIntent();
        ip=intent.getStringExtra("ip");
        port=intent.getStringExtra("port");
        time=intent.getStringExtra("time");
        id=intent.getStringExtra("id");
        path=intent.getStringExtra("path");
        sim_time=Double.parseDouble(time);
        given_sim_time=!(sim_time<=0.0);

        //create the url for connection to broker and topic
        url="tcp://"+ip+":"+port;
        topic="vehicles/"+id;


        // Stop simulation and go back to MainActivity
        back.setOnClickListener(v -> {
            sim_run=false;
            try { // Joins thread for data transfer and stops the activity
                myThread.join();
                //set finish message to "Not available" if not already set
                if(!finish_msg.equals("Not available")){
                    client.publish(topic, finish_msg.getBytes(),0,false);
                    finish_msg ="Not available";
                }
                client.disconnect();
                finish();
            } catch (InterruptedException | MqttException e) {
                e.printStackTrace();
            }
        });

        // Stop simulation
        stop.setOnClickListener(v -> {
            stop_pressed=true;
            sim_run=false;
            Context context = getApplicationContext();
            Toast toast = Toast.makeText(context, "Data Transfer Stopped", Toast.LENGTH_SHORT);
            toast.show();
            try {
                myThread.join();
                //set finish message to "Not available" if not already set
                if(!finish_msg.equals("Not available")){
                    client.publish(topic, finish_msg.getBytes(),0,false);
                    finish_msg ="Not available";
                }
            } catch (InterruptedException | MqttException e) {
                e.printStackTrace();
            }
            stop.setClickable(false);
            stop.setText("Stopped");
            stop.setBackgroundResource(R.drawable.red_disabled);
        });

        //make markers of the real route visible or not with the switch
        rr_sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // The toggle is enabled
                for(int i=0;i<Real_route.size();i++){
                    Real_route.get(i).setVisible(true);
                }
            } else {
                // The toggle is disabled
                for(int i=0;i<Real_route.size();i++){
                    Real_route.get(i).setVisible(false);
                }
            }
        });

        //make markers of the estimated route visible or not with the switch
        er_sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // The toggle is enabled
                for(int i=0;i<Est_route.size();i++){
                    Est_route.get(i).setVisible(true);
                }
            } else {
                // The toggle is disabled
                for(int i=0;i<Est_route.size();i++){
                    Est_route.get(i).setVisible(false);
                }
            }
        });
    }


    // Check for Internet Connection Every 5s
    @Override
    protected void onResume() {
        handler.postDelayed(runnable = () -> {
            AlertDialog dialog;
            // Connect to a network message
            if(!isNetworkConnected()) {
                dialog = new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_wifi_off_24)
                        .setTitle("Not Connected to a Network")
                        .setMessage("Turn on Wifi or Mobile Data")
                        .setCancelable(false)
                        .setPositiveButton("Turn on", (dialog1, i) -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)) )
                        .setNegativeButton("Go back to settings", (dialog1, i) -> finish())
                        .create();
                dialog.show();
            }
            else {
                try {
                    // No Internet connection Message
                    if (!isConnected()){
                        dialog = new AlertDialog.Builder(this)
                                .setIcon(R.drawable.ic_baseline_wifi_off_24)
                                .setTitle("No Internet Connection")
                                .setMessage("You are connected to a network but you do not have Internet connection. Check or Change your network")
                                .setCancelable(false)
                                .setPositiveButton("Change", (dialog1, i) -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)) )
                                .setNegativeButton("Go back to settings", (dialog1, i) -> finish())
                                .create();
                        dialog.show();
                    }
                } catch (InterruptedException | IOException ignored) {

                }
            }
        }, delay);

        super.onResume();
    }

    @Override
    public void onBackPressed() {
        sim_run=false;
        try { // Joins thread for data transfer and stops the activity
            myThread.join();
            //set finish message to "Not available" if not already set
            if(!finish_msg.equals("Not available")){
                client.publish(topic, finish_msg.getBytes(),0,false);
                finish_msg ="Not available";
            }
            client.disconnect();
            finish();
        } catch (InterruptedException | MqttException e) {
            e.printStackTrace();
        }
    }

    // Stop handler when activity not visible
    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    // Function to Check if the user is connected to a Network
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
    // Function to Check if there is Internet Connection
    public boolean isConnected() throws InterruptedException, IOException {
        final String command = "ping -c 1 google.com";
        return Runtime.getRuntime().exec(command).waitFor() == 0;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //set new info window style
        mMap=googleMap;
        mMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));
        mMap.setOnInfoWindowClickListener(this);

        // Creates Mqtt client for Android
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), url,
                clientId);
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                onResume();
            }

            //when a message arrives
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String inc_msg= new String(message.getPayload());
                String[] msg_parts = inc_msg.split("/");
                Log.i("DataMovement", "topic: " + topic + ",from: "+msg_parts[0]+", msg: " +msg_parts[1]);
                //creation of markers and info windows
                if(msg_parts[0].equals("EdgeServer")){ //estimation route marker
                    //code for estimation route points
                    String[] msg_data = msg_parts[1].split(",");
                    //code for real route points
                    double Lat=Double.parseDouble(msg_data[1]);
                    double Lng=Double.parseDouble(msg_data[2]);
                    double Rssi=Double.parseDouble(msg_data[3]);
                    double Thr=Double.parseDouble(msg_data[4]);
                    LatLng pin = new LatLng(Lat,Lng);
                    Marker marker=mMap.addMarker(new MarkerOptions()
                            .position(pin)
                            .rotation(180f)
                            .title("Estimated Position")
                            .snippet("Timestamp: "+msg_data[0]+"s\nLatitude: "+Lat+"\nLongitude: "+Lng+"\nRSSI: "+Rssi+"\nThroughput: "+Thr)
                            .icon(BitmapDescriptorFactory.defaultMarker(230)));
                    Est_route.add(marker);
                }else if(msg_parts[0].equals(id)){ //real route marker
                    String[] msg_data = msg_parts[1].split(",");
                    //code for real route points
                    double Lat=Double.parseDouble(msg_data[2]);
                    double Lng=Double.parseDouble(msg_data[3]);
                    double Rssi=Double.parseDouble(msg_data[6]);
                    double Thr=Double.parseDouble(msg_data[7]);
                    LatLng pin = new LatLng(Lat,Lng);
                    Marker marker=mMap.addMarker(new MarkerOptions()
                            .position(pin)
                            .title("Real Position")
                            .snippet("Timestamp: "+msg_data[0]+"s\nLatitude: "+Lat+"\nLongitude: "+Lng+"\nRSSI: "+Rssi+"\nThroughput: "+Thr)
                            .icon(BitmapDescriptorFactory.defaultMarker(0)));
                    Real_route.add(marker);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(pin));
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(18));
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }

        });


        try {
            // Connects to broker
            IMqttToken token = client.connect();

            token.setActionCallback(new IMqttActionListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("TAG", "onSuccess");

                    try {

                        // Subscribes to a topic
                        client.subscribe(topic,0, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Log.i("DataMovement", "Subscribed to "+topic);
                                Context context = getApplicationContext();
                                Toast toast = Toast.makeText(context, "Subscribed to "+topic, Toast.LENGTH_SHORT);
                                toast.show();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.i("DataMovement", "Subscribe failed");
                                sim_run=false;
                                try {   // Joins thread for data transfer and stops the activity
                                    myThread.join();
                                    if(!finish_msg.equals("Not available")){
                                        client.publish(topic, finish_msg.getBytes(),0,false);
                                        finish_msg ="Not available";
                                    }
                                    client.disconnect();
                                    finish();
                                } catch (InterruptedException | MqttException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    // Creates a new thread for data transfer
                    myThread=new Thread(() -> {
                        File file = new File(path);
                        try {
                            // File open and read
                            BufferedReader br = new BufferedReader(new FileReader(file));

                            int i=0;
                            while ((vector=br.readLine()) != null && sim_run) {             // Read until file is over and simulation is not over

                                String[] v_parts = vector.split(",");
                                finish_msg =id+"/done/"+v_parts[1];                         // recreate the finishing message with the latest timestamp

                                if(i==0) {
                                    String fresh = id + "/fresh/" + v_parts[1];
                                    client.publish(topic, fresh.getBytes(), 0, false);
                                }

                                String str = id + "/" + vector;
                                byte[] payload = str.getBytes();

                                try {
                                    client.publish(topic,payload,0,false);
                                    TimeUnit.SECONDS.sleep(1);                      // Sleeps for 1 second
                                } catch (MqttException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                                i++;                                                    // Increment of seconds
                                if(given_sim_time) {                                    // If a simulation time was given
                                    if (i > sim_time) {                                 // If simulation time is over
                                        sim_run = false;
                                    }
                                }
                            }
                            //set finish message to "Not available" if not already set
                            if(!finish_msg.equals("Not available")){
                                client.publish(topic, finish_msg.getBytes(),0,false);
                                finish_msg ="Not available";
                            }
                        } catch (IOException | MqttException e) {
                            e.printStackTrace();
                        }
                    });
                    myThread.start();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("TAG", "onFailure");
                    finish();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(this, marker.getTitle(), Toast.LENGTH_LONG).show();
    }
}
