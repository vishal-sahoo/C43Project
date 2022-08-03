package project;

public class Listing {
    private int lid;
    private String type;
    private double latitude;
    private double longitude;
    private Address address;

    public Listing(int lid, String type, double latitude, double longitude, Address address) {
        this.lid = lid;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    @Override
    public String toString() {
        return type + " at " + address;
    }
}
