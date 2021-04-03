package custom_types;

public class estimations { // estimated position class
    private double lat;
    private double lng;
    private double rssi;
    private double thr;

    // Constructor
    public estimations(double lat, double lng, double rssi, double thr) {
        this.lat = lat;
        this.lng = lng;
        this.rssi = rssi;
        this.thr = thr;
    }

    //getters
    public double get_lat() {
        return this.lat;
    }

    public double get_lng() {
        return this.lng;
    }

    public double get_rssi() {
        return this.rssi;
    }

    public double get_thr() {
        return this.thr;
    }
}