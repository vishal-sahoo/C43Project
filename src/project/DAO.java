package project;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    public int getListingID(int aid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Listings WHERE aid=?");
        stmt.setInt(1, aid);
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

    /* Returns true if there are already availabilities in the given date range. */
    public boolean checkAvailabilitiesInRange(int lid, String start, String end) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Calendars WHERE lid=? AND Day BETWEEN ? AND ? AND Status!=?");
        stmt.setInt(1, lid);
        stmt.setString(2, start);
        stmt.setString(3, end);
        stmt.setString(4, "UNAVAILABLE");
        ResultSet rs = stmt.executeQuery();
        return rs.next();
    }

    /* Returns true if there is an availability with given lid, day, and status. */
    public boolean checkAvailability(int lid, String day, String status) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Calendars WHERE lid=? AND Day=? AND Status=?");
        stmt.setInt(1, lid);
        stmt.setString(2, day);
        stmt.setString(3, status);
        ResultSet rs = stmt.executeQuery();
        return rs.next();
    }

    /* Returns true if an availability has been booked in the range. */
    public boolean checkBookedInRange(int lid, String start, String end) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Bookings WHERE lid=? AND Status!='CANCELLED' AND (" +
                        "(StartDate BETWEEN ? AND ?) OR (EndDate BETWEEN ? AND ?)" +
                        "OR (? BETWEEN StartDate AND EndDate) OR (? BETWEEN StartDate AND EndDate))");
        stmt.setInt(1, lid);
        stmt.setString(2, start);
        stmt.setString(3, end);
        stmt.setString(4, start);
        stmt.setString(5, end);
        stmt.setString(6, start);
        stmt.setString(7, end);
        ResultSet rs = stmt.executeQuery();
        return rs.next();
    }

    /* Sets all availabilities in range to "UNAVAILABLE", assuming there are no booked availabilities in range.
    * Returns the number of availabilities cancelled in the date range. */
    public int cancelAvailabilitiesInRange(int lid, String start, String end) throws SQLException {
        LocalDate curDate = LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDate = LocalDate.parse(end, DateTimeFormatter.ISO_LOCAL_DATE);

        endDate = endDate.plusDays(1); // add 1 day to include range's endpoints
        int count = 0;

        while (!curDate.equals(endDate)) {
            if (checkAvailabilitiesInRange(lid, curDate.toString(), curDate.toString())) {
                cancelAvailability(lid, curDate.toString());
                count++;
            }
            curDate = curDate.plusDays(1);
        }
        return count;
    }

    /* Sets the status of an availability to "UNAVAILABLE". */
    public void cancelAvailability(int lid, String day) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE Calendars SET Status='UNAVAILABLE' WHERE lid=? AND Day=?");
        stmt.setInt(1, lid);
        stmt.setString(2, day);
        stmt.executeUpdate();
    }

    /* Updates the price of availabilities in a given date range. Returns the number of availabilities modified. */
    public int updateAvailabilityInRange(int lid, String start, String end, double price) throws SQLException {
        LocalDate curDate = LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDate = LocalDate.parse(end, DateTimeFormatter.ISO_LOCAL_DATE);

        endDate = endDate.plusDays(1); // add 1 day to include range's endpoints
        int count = 0;

        while (!curDate.equals(endDate)) {
            if (checkAvailabilitiesInRange(lid, curDate.toString(), curDate.toString())) {
                updateAvailabilityPrice(lid, curDate.toString(), price); // availability exists, so update the price
                count++;
            }
            curDate = curDate.plusDays(1);
        }
        return count;
    }

    public void updateAvailabilityPrice(int lid, String day, double price) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE Calendars SET Price=? WHERE lid=? AND Day=?");
        stmt.setDouble(1, price);
        stmt.setInt(2, lid);
        stmt.setString(3, day);
        stmt.executeUpdate();
    }

    public void updateAvailabilityStatus(int lid, String day, String status) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE Calendars SET Status=? WHERE lid=? AND Day=?");
        stmt.setString(1, status);
        stmt.setInt(2, lid);
        stmt.setString(3, day);
        stmt.executeUpdate();
    }

    public void createAvailabilitiesInRange(int lid, String start, String end, double price) throws SQLException {
        LocalDate curDate = LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDate = LocalDate.parse(end, DateTimeFormatter.ISO_LOCAL_DATE);

        endDate = endDate.plusDays(1); // add 1 day to include range's endpoints

        while (!curDate.equals(endDate)) {
            if (!checkAvailabilitiesInRange(lid, curDate.toString(), curDate.toString())) {
                // check if there's a cancelled availability
                if (checkAvailability(lid, curDate.toString(), "UNAVAILABLE")) {
                    // there's an availability here, so make "AVAILABLE" and update price accordingly
                    updateAvailabilityStatus(lid, curDate.toString(), "AVAILABLE");
                    updateAvailabilityPrice(lid, curDate.toString(), price);
                } else {
                    // no availability on this day, so create one
                    createAvailability(lid, curDate.toString(), price, "AVAILABLE");
                }
            }
            curDate = curDate.plusDays(1);
        }
    }

    public void createAvailability(int lid, String day, double price, String status) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Calendars VALUES (?, ?, ?, ?)");
        stmt.setInt(1, lid);
        stmt.setString(2, day);
        stmt.setDouble(3, price);
        stmt.setString(4, status);
        stmt.executeUpdate();
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

    public void createView(String view, String query) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("CREATE VIEW "+view+" AS ("+query+")");
        stmt.execute();
    }

    public void deleteView(String view) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DROP VIEW IF EXISTS "+view);
        stmt.execute();
    }

    public ArrayList<Listing> getListingsFromFilter3(String str) throws SQLException {
        PreparedStatement stmt;

        if (str.equals("ASC") || str.equals("DESC")) {
            stmt = conn.prepareStatement("SELECT L.*, AVG(Price) as Price FROM Filter3 L, Calendars C " +
                    "WHERE L.LID=C.LID GROUP BY L.LID ORDER BY Price " + str);
        } else {
            stmt = conn.prepareStatement("SELECT * FROM Filter3");
        }

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
        return getListingID(aid);
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
