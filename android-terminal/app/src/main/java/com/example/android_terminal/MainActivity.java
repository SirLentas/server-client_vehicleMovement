package com.example.android_terminal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.isabsent.filepicker.SimpleFilePickerDialog;

import java.io.IOException;

import static com.github.isabsent.filepicker.SimpleFilePickerDialog.CompositeMode.FILE_ONLY_SINGLE_CHOICE;

public class MainActivity extends AppCompatActivity implements
        SimpleFilePickerDialog.InteractionListenerString {

    final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    String ip, port, time, id, path;
    Button start, exit, browse;
    TextView textFile;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 5000;                   // Interval between internet connection check

    private static final String         // If you need to show a few different mode pickers in one activity,
            PICK_DIALOG = "PICK_DIALOG";

    @SuppressLint({"ResourceAsColor", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        EditText edText1 = findViewById(R.id.editText1);
        edText1.setInputType(InputType.TYPE_CLASS_TEXT);

        EditText edText2 = findViewById(R.id.editText2);
        edText2.setInputType(InputType.TYPE_CLASS_TEXT);

        EditText edText3 = findViewById(R.id.editText3);
        edText3.setInputType(InputType.TYPE_CLASS_TEXT);

        EditText edText4 = findViewById(R.id.editText4);
        edText4.setInputType(InputType.TYPE_CLASS_TEXT);

        textFile = findViewById(R.id.textfile);
        start = findViewById(R.id.start);
        exit = findViewById(R.id.exit);
        browse = findViewById(R.id.browsebtn);

        // Check for READ_EXTERNAL_STORAGE Permission
        if (!check(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST_CODE);
        }

        browse.setOnClickListener(v -> {
            // Check for READ_EXTERNAL_STORAGE Permission
            if (!check(Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST_CODE);
            }
            // If Permission is granted, open file explorer
            else{
                showListItemDialog("Select a .csv", rootPath, FILE_ONLY_SINGLE_CHOICE, PICK_DIALOG);
            }
        });

        // Check inputs before starting RealActivity
        start.setOnClickListener(v -> {
            ip = edText1.getText().toString();
            if (TextUtils.isEmpty(ip)) {
                edText1.setError("Give an IP address");
                return;
            }
            port = edText2.getText().toString();
            if (TextUtils.isEmpty(port)) {
                edText2.setError("Give a port for connection");
                return;
            }
            time = edText3.getText().toString();
            if (TextUtils.isEmpty(time)) {      // If simulation time is not given it is initialised with -1.0
                time = "-1.0";
            }
            id = edText4.getText().toString();
            if (TextUtils.isEmpty(id)) {
                edText4.setError("Type your ID");
                return;
            }
            if (TextUtils.isEmpty(path) || !path.endsWith(".csv")) {
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, "Select a .csv file", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            // Start RealActivity
            openNewActivity(ip, port, time, id, path);
        });

        // Exit Confirmation Message
        exit.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_baseline_warning_24)
                .setTitle("Closing Application")
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> finish())
                .setNegativeButton("No", null)
                .show());
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
                        .setNegativeButton("Exit the app", (dialog1, i) -> finish())
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
                                .setNegativeButton("Exit the app", (dialog1, i) -> finish())
                                .create();
                        dialog.show();
                    }
                } catch (InterruptedException | IOException ignored) {

                }
            }
        }, delay);

        super.onResume();
    }

    // Stop handler when activity not visible
    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    // Start RealActivity
    public void openNewActivity(String ip, String port, String time, String id, String path) {
        Intent intent = new Intent(this, RealActivity.class);
        intent.putExtra("ip", ip);
        intent.putExtra("port", port);
        intent.putExtra("time", time);
        intent.putExtra("id", id);
        intent.putExtra("path", path);
        startActivity(intent);
    }

    // Exit Confirmation Message when back is pressed
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_baseline_warning_24)
                .setTitle("Closing Application")
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> MainActivity.super.onBackPressed())
                .setNegativeButton("No", null)
                .show();
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

    // Function to show File Explorer
    @Override
    public void showListItemDialog(String title, String folderPath, SimpleFilePickerDialog.CompositeMode mode, String dialogTag){
        SimpleFilePickerDialog.build(folderPath, mode)
                .title(title)
                .show(this, dialogTag);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (PICK_DIALOG.equals(dialogTag)) {
            if (extras.containsKey(SimpleFilePickerDialog.SELECTED_SINGLE_PATH)) {
                path=extras.getString(SimpleFilePickerDialog.SELECTED_SINGLE_PATH);
                textFile.setText("File Path:\n" + path);
            }
        }
        return false;
    }

    // Function to check if a permission is granted or not
    public boolean check(String permission)
    {
        int check = ContextCompat.checkSelfPermission(this, permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }

}
