package fault_graph;

public class distance {
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) { //if the coordinates for lat and long are the same
            return 0;
        }
        else { // we calculate distance based in code from https://www.geodatasource.com/developers/java
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515; //we calculate distance to miles
            dist = dist * 1.609344; //we transform distance to kilometers
            dist = dist*1000; //we transform distance to meters
            return (dist);
        }
    }
}
