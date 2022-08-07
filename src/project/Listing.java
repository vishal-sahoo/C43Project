package project;

import java.sql.SQLException;
import java.util.Locale;

public class Listing {
    private int lid;
    private String type;
    private double latitude;
    private double longitude;
    private Address address;

    private String aux;

    public Listing(int lid, String type, double latitude, double longitude, Address address, String aux) {
        this.lid = lid;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.aux = aux;
    }

    public Listing(int lid, String type, double latitude, double longitude, Address address) {
        this.lid = lid;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public static int createListing(DAO dao, int hid, String type, double latitude, double longitude, String address,
                                    String city, String country, String postalCode)
            throws IllegalArgumentException, SQLException {

        Listing.validateParameters(type, latitude, longitude, address, city, country, postalCode);

        int aid = dao.createAddress(address.toLowerCase(Locale.ROOT).trim(), city.toLowerCase(Locale.ROOT).trim(),
                country.toLowerCase(Locale.ROOT).trim(), postalCode.toLowerCase(Locale.ROOT).trim());
        int lid = dao.createListing(hid, type, latitude, longitude, aid);
        return lid;
    }

    public static void validateParameters(String type, double latitude, double longitude, String address, String city,
                                          String country, String postalCode) throws IllegalArgumentException {
        if (type.isEmpty() || address.isEmpty() || city.isEmpty() || country.isEmpty() || postalCode.isEmpty() ||
                (!type.equals("house") && !type.equals("apartment") && !type.equals("guesthouse") && !type.equals("hotel"))) {
            throw new IllegalArgumentException();
        }
    }

    public int getLid() {
        return lid;
    }

    @Override
    public String toString() {
        return type + " at " + address + (aux == null ? "" : " " + aux);
    }
}
