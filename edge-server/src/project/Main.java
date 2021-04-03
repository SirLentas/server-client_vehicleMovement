package project;

import custom_types.estimations;
import fault_graph.fault_graph;
import heatmaps.rssi_heatmap;
import heatmaps.throughput_heatmap;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.jfree.ui.RefineryUtilities;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static fault_graph.distance.distance;
import static rssi_throughput_values.average_rssi.average_rssi;
import static rssi_throughput_values.average_throughput.average_throughput;
import static xmltocsv.XMLtoCSV.XMLtoCSV;

public class Main {

    static IMqttClient EdgeServer;

    //given data
    static double lat_min_y = 37.9668800;
    static double lat_max_y = 37.9686200;
    static double long_min_x = 23.7647600;
    static double long_max_x = 23.7753900;
    static BigDecimal lat_distance = BigDecimal.valueOf(lat_max_y).subtract(BigDecimal.valueOf(lat_min_y));
    static BigDecimal long_distance = BigDecimal.valueOf(long_max_x).subtract(BigDecimal.valueOf(long_min_x));
    static BigDecimal lat_grid_size = lat_distance.divide(BigDecimal.valueOf(4), 7, RoundingMode.CEILING);
    static BigDecimal long_grid_size = long_distance.divide(BigDecimal.valueOf(10), 7, RoundingMode.CEILING);
    static double R=6.371*1000000;

    public static void main(String[] args) throws MqttException, SQLException, ClassNotFoundException, InterruptedException {

        XMLtoCSV("all_vehicles");   //create all_vehicles.csv
        XMLtoCSV("vehicle_26");     //create vehicle_26.csv
        XMLtoCSV("vehicle_27");     //create vehicle_27.csv
        System.out.println("CSVs created ..!!");

        //average values of rssi & throughput
        double[][] thr_values=average_throughput("all_vehicles.csv", lat_min_y, lat_max_y, long_min_x, long_max_x); // rssi & throughput values for the file you give as string
        double[][] rssi_values=average_rssi("all_vehicles.csv", lat_min_y, lat_max_y, long_min_x, long_max_x); // average rssi values for the file you give as string

        //hashmap to store estimated position of a vehicle until we receive the real position
        HashMap<String, estimations> map = new HashMap<>();

        //RSSI Heatmap
        JFrame rssi_frame = new JFrame(); //add JFrame for the window
        JPanel wrapperPanel_rssi = new JPanel(new GridBagLayout()); //add new JPanel with Gridlayout option
        //edit the new JPanel to add our grid lines and fill them with colors
        wrapperPanel_rssi.add(new rssi_heatmap.RSSI_HeatMap(rssi_values));
        //add options for the visualization
        rssi_frame.add(wrapperPanel_rssi);
        rssi_frame.setTitle("RSSI Heatmap");
        //if this option is active terminate th server process with th closing of the frame window
        //rssi_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        rssi_frame.pack();
        rssi_frame.setLocationRelativeTo(null);
        rssi_frame.setVisible(true);

        //Throughput Heatmap
        JFrame thr_frame = new JFrame();//add new JFrame for the window
        JPanel wrapperPanel_thr = new JPanel(new GridBagLayout()); //add new JPanel with Gridlayout option
        //edit the new JPanel to add our grid lines and fill them with colors
        wrapperPanel_thr.add(new throughput_heatmap.THROUGHPUT_HeatMap(thr_values));
        thr_frame.add(wrapperPanel_thr);
        thr_frame.setTitle("THROUGHPUT Heatmap");
        //if this option is active terminate th server process with th closing of the frame window
        //thr_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        thr_frame.pack();
        thr_frame.setLocationRelativeTo(null);
        thr_frame.setVisible(true);

        //Connect to Database
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/androidDB","nancy","12345678");
        Statement stmt=con.createStatement();

        //delete everything from the database
        stmt.executeUpdate("DELETE FROM car_data");

        //check if the database is empty
        ResultSet rs=stmt.executeQuery("SELECT * FROM car_data");
        int data=0;
        while(rs.next()) {
            data = data + 1;
        }
        if (data==0) System.out.println("The database is empty!");

        //MQTT
        String serverId = UUID.randomUUID().toString();
        String MqttServerUrl="tcp://"+args[0]+":"+args[1]; //url for the local broker
        //create a client and put the files that broker created on the /tmp
        EdgeServer = new MqttClient(MqttServerUrl,serverId,new MqttDefaultFilePersistence("/tmp"));

        EdgeServer.connect();

        //subscribe to all vehicle's subtopics
        EdgeServer.subscribe("vehicles/#",(topic,rec)->{
            byte[] payload = rec.getPayload();
            String s = new String(payload);

            //find publisherID
            String[] msg_parts = s.split("/");
            //check if the sender is the server and print the suitable message
            if(!msg_parts[0].equals("EdgeServer")) { //if message is send from another client

                System.out.println("\nFrom: " + msg_parts[0]);
                    
                if (msg_parts[1].equals("done")) { //if message received is a movement termination message it has this form "client id"/done/"id"
                    System.out.println("Time to calculate fault for " + msg_parts[2]);

                    //get all data about the vehicle with this id from the database
                    ResultSet points = stmt.executeQuery("SELECT * FROM car_data WHERE device_id=" + Integer.parseInt(msg_parts[2]));

                    List<Double> faults = new ArrayList<>();
                    double last_t = 0;
                    while (points.next()) {
                        System.out.println(points.getFloat("timestep") + " | " + points.getInt("device_id") + " | " + points.getDouble("real_lat") + " | " + points.getDouble("real_long") + " | " + points.getDouble("predicted_lat") + " | " + points.getDouble("predicted_long"));
                        last_t = points.getFloat("timestep"); //renew last timestep

                        //estimate distance fault for every point retrieved from the database
                        double est_distance = distance(points.getDouble("real_lat"), points.getDouble("real_long"), points.getDouble("predicted_lat"), points.getDouble("predicted_long"));
                        System.out.println("Estimation fault: " + est_distance + " m");
                        faults.add(est_distance); //add the calculated fault in a list
                    }

                    final fault_graph g = new fault_graph("Fault Graph " + msg_parts[2], faults, last_t); //create graph about faults
                    g.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); //change default close operation
                    g.pack();
                    RefineryUtilities.centerFrameOnScreen(g);
                    g.setVisible(true);
                }else if(msg_parts[1].equals("fresh")){
                    System.out.println("New session for " + msg_parts[0]);
                    //delete data for this vehicle from the database, from previous sessions
                    //this way the same vehicle can send new data again and again
                    stmt.executeUpdate("DELETE FROM car_data WHERE device_id=" + Integer.parseInt(msg_parts[2]));
                }else { //if message if not for movement termination or clear
                    String[] msg_data = msg_parts[1].split(",");
                    double timestamp = Double.parseDouble(msg_data[0]);
                    int v_id=Integer.parseInt(msg_data[1]);
                    double lat_s = Double.parseDouble(msg_data[2]);
                    double long_s = Double.parseDouble(msg_data[3]);
                    double angle = Double.parseDouble(msg_data[4]);
                    double speed = Double.parseDouble(msg_data[5]);
                    double rssi = Double.parseDouble(msg_data[6]);
                    double throughput = Double.parseDouble(msg_data[7]);
                    System.out.println("REAL DATA "+timestamp+" "+msg_parts[1]);

                    //if point is outside selected area we don't make an estimation about next position
                    if (lat_s < lat_min_y || lat_s > lat_max_y || long_s < long_min_x || long_s > long_max_x) {
                        System.out.println("Vehicle is outside of the selected area");
                    } else {
                        // Hashmap search key has the name of client sending the message and the timestamp of it, so it's unique if clients have different IDs
                        String search_key = msg_data[1] + "/" + timestamp;
                        if (map.containsKey(search_key)) {
                            //find data in hash map and add it in db
                            stmt.executeUpdate("INSERT INTO car_data VALUES ("+timestamp+","+v_id+","+lat_s+","+long_s+","+map.get(search_key).get_lat()+","+map.get(search_key).get_lng()+","+rssi+","+throughput+","+map.get(search_key).get_rssi()+","+map.get(search_key).get_thr()+");");
                            //then remove data from hashmap
                            map.remove(search_key);
                        }

                        // we calculate estimated position based on given formula
                        lat_s = Math.toRadians(lat_s);
                        long_s = Math.toRadians(long_s);
                        angle = Math.toRadians(angle);
                        double delta = speed / R;

                        double lat_e = Math.asin(Math.sin(lat_s) * Math.cos(delta) + Math.cos(lat_s) * Math.sin(delta) * Math.cos(angle));
                        double long_e = long_s + Math.atan2(Math.sin(angle) * Math.sin(delta) * Math.cos(lat_s), Math.cos(delta) - Math.sin(lat_s) * Math.sin(lat_e));
                        lat_e = Math.toDegrees(lat_e);
                        long_e = Math.toDegrees(long_e);

                        double rssi_est, throughput_est;

                        //we find the cell where the estimated position is in
                        BigDecimal y = BigDecimal.valueOf(lat_e).subtract(BigDecimal.valueOf(lat_min_y)).divide(lat_grid_size, 0, RoundingMode.FLOOR);//0to4
                        BigDecimal x = BigDecimal.valueOf(long_e).subtract(BigDecimal.valueOf(long_min_x)).divide(long_grid_size, 0, RoundingMode.FLOOR);//0to10

                        if (y.intValue()<0 || y.intValue()>=4 || x.intValue()<0 || x.intValue()>=10){
                            System.out.println("Estimated to be outside selected area");
                        }else{

                            //we get the estimated rssi and throughput based on average values for this cell
                            rssi_est = rssi_values[y.intValue()][x.intValue()];
                            throughput_est = thr_values[y.intValue()][x.intValue()];

                            String Topic = "vehicles/" + msg_parts[0];
                            String Estimation = "EdgeServer/" + (timestamp + 1) + "," + lat_e + "," + long_e + "," + rssi_est + "," + throughput_est;
                            String store_key = msg_data[1] + "/" + (timestamp + 1);


                            //we create a new estimation object and store it in the hashmap
                            estimations est = new estimations(lat_e, long_e, rssi_est, throughput_est);
                            map.put(store_key, est);
                            Thread thread = new Thread(() -> { //we open a new thread and we send the estimated position/data back to the same topic
                                try {
                                    EdgeServer.publish(Topic, Estimation.getBytes(), 0, false);
                                    System.out.println("ESTIMATED DATA "+(timestamp+1)+" (Published in " + Topic + ") " + Estimation);
                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }
                            });
                            thread.start();
                        }
                    }
                }
            }

        });
        System.out.println("Connected to broker");
        System.out.println("-----------------------------\n");

        //handle the ctrl+c for the server's termination
        CountDownLatch doneSignal = new CountDownLatch(1);
        //This handler will be called on Control-C pressed
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Decrement counter.
            // It will became 0 and main thread who waits for this barrier could continue run (and fulfill all proper shutdown steps)
            System.out.println("\n-----------------------------");
            System.out.println("Ctrl+C caught");
            try {
                //disconnect from broker and database
                EdgeServer.disconnect();
                con.close();
            } catch (MqttException | SQLException e) {
                e.printStackTrace();
            }
            System.out.println("Disconnected from MQTT Broker and DB");
            doneSignal.countDown();
        }));
        doneSignal.await();
    }
}

