package project;

import java.sql.SQLException;
import java.util.Locale;

public class Listing {
    private int lid;
    private String type;
    private double latitude;
    private double longitude;

    public static void createListing(DAO dao, int hid, String type, double latitude, double longitude, String address,
                                        String city, String country, String postalCode)
            throws IllegalArgumentException, SQLException {

        Listing.validateParameters(type, latitude, longitude, address, city, country, postalCode);

        int aid = dao.createAddress(address.toLowerCase(Locale.ROOT).trim(), city.toLowerCase(Locale.ROOT).trim(),
                country.toLowerCase(Locale.ROOT).trim(), postalCode.toLowerCase(Locale.ROOT).trim());
        dao.createListing(hid, type, latitude, longitude, aid);
        //return true;
    }

    public static void validateParameters(String type, double latitude, double longitude, String address, String city,
                                          String country, String postalCode) throws IllegalArgumentException {
        if (type.isEmpty() || address.isEmpty() || city.isEmpty() || country.isEmpty() || postalCode.isEmpty() ||
                (!type.equals("house") && !type.equals("apartment") && !type.equals("guesthouse") && !type.equals("hotel"))) {
            throw new IllegalArgumentException();
        }
    }
}
