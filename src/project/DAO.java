package project;

import java.sql.*;
import java.util.ArrayList;

public class DAO {

    public final Connection conn;

    public DAO(String dbName, String user, String password) throws SQLException {
        String url = "jdbc:mysql://localhost/" + dbName;
        conn = DriverManager.getConnection(url, user, password);
    }

    public User getUserOnEmail(String email) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Users WHERE Email=?");
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        User user = null;
        if(rs.next()) {
            int uid = rs.getInt("UID");
            String sin = rs.getString("SIN");
            String occupation = rs.getString("Occupation");
            String password = rs.getString("Password");
            String dob = rs.getString("DOB");
            String name = rs.getString("Name");
            int aid = rs.getInt("AID");
            user = new User(uid, sin, email, occupation, password, dob, name, aid);
        }
        return user;
    }
    public Renter getRenterFromUser(User user) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Renters where UID=?");
        stmt.setInt(1, user.getUid());
        ResultSet rs = stmt.executeQuery();
        Renter renter = null;
        if(rs.next()) {
            String creditCard = rs.getString("CreditCard");
            renter = new Renter(user, creditCard);
        }
        return renter;
    }
    public Host getHostFromUser(User user) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Hosts where UID=?");
        stmt.setInt(1, user.getUid());
        ResultSet rs = stmt.executeQuery();
        Host host = null;
        if(rs.next()) {
            host = new Host(user);
        }
        return host;
    }
    // Operations to Support
    public int getAddressID(String address, String city, String country, String postalCode) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Addresses WHERE Address=? AND City=? AND Country=? AND PostalCode=?");
        stmt.setString(1, address);
        stmt.setString(2, city);
        stmt.setString(3, country);
        stmt.setString(4, postalCode);
        ResultSet rs = stmt.executeQuery();
        int result = -1;
        if(rs.next()) {
            result = rs.getInt("AID");
        }
        return result;
    }

    public int getListingID(int hid, String type, double latitude, double longitude, int aid, String status) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Listings WHERE uid=? AND type=? AND latitude=? AND longitude=? AND aid=? AND status=?");
        stmt.setInt(1, hid);
        stmt.setString(2, type);
        stmt.setDouble(3, latitude);
        stmt.setDouble(4, longitude);
        stmt.setInt(5, aid);
        stmt.setString(6, status);
        ResultSet rs = stmt.executeQuery();
        int result = -1;
        if (rs.next()) {
            result = rs.getInt("LID");
        }
        return result;
    }

    public ArrayList<String> getAmenitiesListByCategory(String category) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Amenities WHERE category=?");
        stmt.setString(1, category);
        ResultSet rs = stmt.executeQuery();
        ArrayList<String> amenities = new ArrayList<>();
        while (rs.next()) {
            amenities.add(rs.getString("Description"));
        }

        return amenities;
    }

    public int createAddress(String address, String city, String country, String postalCode) throws SQLException {
        int aid = getAddressID(address, city, country, postalCode);
        if ( aid != -1) {
            return aid;
        }
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Addresses(Address, City, Country, PostalCode) VALUES (?, ?, ?, ?)");
        stmt.setString(1, address);
        stmt.setString(2, city);
        stmt.setString(3, country);
        stmt.setString(4, postalCode);
        stmt.executeUpdate();
        return getAddressID(address, city, country, postalCode);
    }
    public int createUser(String sin, String name, String dob, String occupation, String email,
                              String password, int aid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Users(SIN, Name, DOB, Occupation, Email, Password, AID, Status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        stmt.setString(1, sin);
        stmt.setString(2, name);
        stmt.setString(3, dob);
        stmt.setString(4, occupation);
        stmt.setString(5, email);
        stmt.setString(6, password);
        stmt.setInt(7, aid);
        stmt.setString(8, "ACTIVE");
        stmt.executeUpdate();
        return getUserOnEmail(email).getUid();
    }

    public void createRenter(int uid, String creditCard) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Renters VALUES (?, ?)");
        stmt.setInt(1, uid);
        stmt.setString(2, creditCard);
        stmt.executeUpdate();
    }

    public void createHost(int uid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Hosts VALUES (?)");
        stmt.setInt(1, uid);
        stmt.executeUpdate();
    }

    public ArrayList<Listing> getListingsFromHost(int hid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Listings NATURAL JOIN Addresses WHERE uid=? AND status='ACTIVE'");
        stmt.setInt(1, hid);
        ResultSet rs = stmt.executeQuery();
        ArrayList<Listing> result = new ArrayList<>();
        while(rs.next()) {
            int lid = rs.getInt("LID");
            String type = rs.getString("Type");
            double latitude = rs.getDouble("Latitude");
            double longitude = rs.getDouble("Longitude");

            int aid = rs.getInt("AID");
            String address = rs.getString("Address");
            String city = rs.getString("City");
            String country = rs.getString("Country");
            String postalCode = rs.getString("PostalCode");

            Address newAddress = new Address(aid, address, city, country, postalCode);
            result.add(new Listing(lid, type, latitude, longitude, newAddress));
        }
        return result;
    }

    public boolean deleteUser() {
        return false;
        // remove listings
        // cancel relevant bookings
    }

    public boolean createBooking() {
        return false;
        // update calendar
        // create a new booking given listing and date range if available
    }

    public int createListing(int hid, String type, double latitude, double longitude, int aid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Listings(UID, Type, Latitude, Longitude, AID, Status) " +
                        "VALUES (?, ?, ?, ?, ?, ?)");
        stmt.setInt(1, hid);
        stmt.setString(2, type);
        stmt.setDouble(3, latitude);
        stmt.setDouble(4, longitude);
        stmt.setInt(5, aid);
        stmt.setString(6, "ACTIVE");
        stmt.executeUpdate();
        //System.out.println("LISTING CREATED");
        return getListingID(hid, type, latitude, longitude, aid, "ACTIVE");
    }

    public void offerAmenity(int lid, String description) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Offers VALUES (?, ?)");
        stmt.setInt(1, lid);
        stmt.setString(2, description);
        stmt.executeUpdate();
    }

    public boolean cancelBooking() {
        return false;
        // update calendar
    }

    public boolean removeListing() {
        return false;
        // cancel relevant bookings
    }

    public boolean updatePrice() {
        return false;
        // not allowed if already booked
        // host can change the price of a listing given a date range
    }

    public boolean changeAvailability() {
        return false;
        // host can make an available listing unavailable on a given date
    }

    public boolean reviewBooking() {
        return false;
    }

    public boolean reviewUser() {
        return false;
        // renter or host can leave a rating and/or comment about the other
    }


    // Queries to support
    public void getListingsWithinRadius(){
        // given lat and long, return listings within a given radius (default if none provided)
        // rank by distance
        // option to rank by price (ascending or descending)
    }

    public void getListingsNearPostalCode(){
        // return listings in given postal code or in adjacent postal codes
    }

    public void getListingsByAddress() {
        // return listings given an address
    }


    // all filtering should be supported


    // Reports to support
    public void getNumOfBookings() {
        // return num of bookings given a date range
        // group by city or postal code within a city
    }

    public void getNumofListings() {
        // return num of listings by country, by country and city, by country, city, and postal code
    }

    public void rankHosts() {
        // rank hosts by total number of listings by country (optionally by city)
    }

    // more reports to be added

    public void close() throws SQLException {
        conn.close();
    }
}
