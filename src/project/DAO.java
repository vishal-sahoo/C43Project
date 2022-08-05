package project;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
            String status = rs.getString("Status");
            user = new User(uid, sin, email, occupation, password, dob, name, aid, status);
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

    public ArrayList<Calendar> getAvailabilitiesInRange(int lid, String start, String end) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Calendars WHERE lid=? AND Day BETWEEN ? AND ?");
        stmt.setInt(1, lid);
        stmt.setString(2, start);
        stmt.setString(3, end);
        ResultSet rs = stmt.executeQuery();
        ArrayList<Calendar> result = new ArrayList<>();
        while (rs.next()) {
            String day = rs.getString("Day");
            double price = rs.getDouble("Price");
            String status = rs.getString("Status");

            result.add(new Calendar(lid, day, price, status));
        }
        return result;
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
    public boolean checkSingleAvailability(int lid, String day, String status) throws SQLException {
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
                if (checkSingleAvailability(lid, curDate.toString(), "UNAVAILABLE")) {
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

    public Booking getBooking(int lid, String startDate, String endDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Bookings " +
                "WHERE LID = ? AND StartDate = ? AND endDate = ?");
        stmt.setInt(1, lid);
        stmt.setString(2, startDate);
        stmt.setString(3, endDate);

        ResultSet rs = stmt.executeQuery();
        Booking booking = null;
        if (rs.next()) {
            int bid = rs.getInt("BID");
            int rid = rs.getInt("RID");
            Double cost = rs.getDouble("Cost");
            String status = rs.getString("Status");
            String review = rs.getString("Review");
            int rating = rs.getInt("Rating");
            booking = new Booking(bid, rid, lid, startDate, endDate, cost, status, review, rating);
        }
        return booking;
    }

    public boolean checkAvailability(int lid, String startDate, String endDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT LID FROM Calendars C " +
                "WHERE Status='AVAILABLE' AND LID = ? AND Day BETWEEN ? AND ? " +
                "GROUP BY LID HAVING COUNT(*)=DATEDIFF(?, ?)+1");
        stmt.setInt(1, lid);
        stmt.setString(2, startDate);
        stmt.setString(3, endDate);
        stmt.setString(4, endDate);
        stmt.setString(5, startDate);
        return stmt.executeQuery().next();
    }

    public double getCost(int lid, String startDate, String endDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT SUM(Price) as Cost FROM Calendars " +
                "WHERE LID = ? AND Day BETWEEN ? AND ?");
        stmt.setInt(1, lid);
        stmt.setString(2, startDate);
        stmt.setString(3, endDate);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getDouble("Cost");
        }
        return -1;
    }

    public void updateCalendar(int lid, String startDate, String endDate, String status) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE Calendars SET Status = ? " +
                "WHERE LID = ? AND Day BETWEEN ? AND ?");
        stmt.setString(1, status);
        stmt.setInt(2, lid);
        stmt.setString(3, startDate);
        stmt.setString(4, endDate);
        stmt.executeUpdate();
    }

    public Booking createBooking(int rid, int lid, String startDate,
                                 String endDate, double cost) throws SQLException {
        // update calendar
        // create a new booking given listing and date range if available
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO " +
                "Bookings(RID, LID, StartDate, EndDate, Cost, Status) VALUES(?, ?, ?, ?, ?, 'UPCOMING')");
        stmt.setInt(1, rid);
        stmt.setInt(2, lid);
        stmt.setString(3, startDate);
        stmt.setString(4, endDate);
        stmt.setDouble(5, cost);
        stmt.executeUpdate();

        return getBooking(lid, startDate, endDate);
    }

    public List<Booking> getRentersBookings(String status, int rid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Bookings WHERE Status = ? AND RID = ?");
        stmt.setString(1, status);
        stmt.setInt(2, rid);
        ResultSet rs = stmt.executeQuery();

        List<Booking> bookings = new ArrayList<Booking>();
        if (rs.next()) {
            int bid = rs.getInt("BID");
            int lid = rs.getInt("LID");
            String startDate = rs.getString("StartDate");
            String endDate = rs.getString("EndDate");
            Double cost = rs.getDouble("Cost");
            String review = rs.getString("Review");
            int rating = rs.getInt("Rating");
            bookings.add(new Booking(bid, rid, lid, startDate, endDate, cost, status, review, rating));
        }
        return bookings;
    }

    public List<Booking> getHostsBookings(String status, int hid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT B.* FROM Bookings B, Listings L " +
                "WHERE B.LID=L.LID AND B.Status = ? AND L.UID = ?");
        stmt.setString(1, status);
        stmt.setInt(2, hid);
        ResultSet rs = stmt.executeQuery();

        List<Booking> bookings = new ArrayList<Booking>();
        if (rs.next()) {
            int bid = rs.getInt("BID");
            int rid = rs.getInt("RID");
            int lid = rs.getInt("LID");
            String startDate = rs.getString("StartDate");
            String endDate = rs.getString("EndDate");
            Double cost = rs.getDouble("Cost");
            String review = rs.getString("Review");
            int rating = rs.getInt("Rating");
            bookings.add(new Booking(bid, rid, lid, startDate, endDate, cost, status, review, rating));
        }
        return bookings;
    }

    public Listing getListingFromID(int lid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Listings " +
                "NATURAL JOIN Addresses WHERE LID=?");
        stmt.setInt(1, lid);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            String type = rs.getString("Type");
            double latitude = rs.getDouble("Latitude");
            double longitude = rs.getDouble("Longitude");

            int aid = rs.getInt("AID");
            String address = rs.getString("Address");
            String city = rs.getString("City");
            String country = rs.getString("Country");
            String postalCode = rs.getString("PostalCode");

            Address newAddress = new Address(aid, address, city, country, postalCode);
            return new Listing(lid, type, latitude, longitude, newAddress);
        }
        return null;
    }

    public void updateBookingsStatus() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE Bookings SET Status='PAST' " +
                "WHERE EndDate < CURDATE()");
        stmt.executeUpdate();
    }

    public void updateBooking(int bid) throws SQLException {
        // update calendar
        PreparedStatement stmt = conn.prepareStatement("UPDATE Bookings SET Status='CANCELED' " +
                "WHERE BID = ?");
        stmt.setInt(1, bid);
        stmt.executeUpdate();
    }

    public void reviewBooking(int bid, String review, int rating) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE bookings SET Review = ?, Rating = ? WHERE BID = ?");
        stmt.setString(1, review);
        stmt.setInt(2, rating);
        stmt.setInt(3, bid);
        stmt.executeUpdate();
    }

    public List<User> getHostsOfRenter(int rid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT U.* FROM Bookings B, Listings L, Users U " +
                "WHERE B.LID = L.LID AND L.UID = U.UID AND B.RID = ? AND B.Status = 'PAST'");
        stmt.setInt(1, rid);
        ResultSet rs = stmt.executeQuery();
        List<User> users = new ArrayList<>();
        while (rs.next()) {
            int uid = rs.getInt("UID");
            String sin = rs.getString("SIN");
            String email = rs.getString("Email");
            String occupation = rs.getString("Occupation");
            String password = rs.getString("Password");
            String dob = rs.getString("DOB");
            String name = rs.getString("Name");
            int aid = rs.getInt("AID");
            String status = rs.getString("Status");
            users.add(new User(uid, sin, email, occupation, password, dob, name, aid, status));
        }
        return users;
    }

    public List<User> getRentersOfHost(int hid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT U.* FROM Bookings B, Listings L, Users U " +
                "WHERE B.LID = L.LID AND B.RID = U.UID AND L.UID = ? AND B.Status = 'PAST'");
        stmt.setInt(1, hid);
        ResultSet rs = stmt.executeQuery();
        List<User> users = new ArrayList<>();
        while (rs.next()) {
            int uid = rs.getInt("UID");
            String sin = rs.getString("SIN");
            String email = rs.getString("Email");
            String occupation = rs.getString("Occupation");
            String password = rs.getString("Password");
            String dob = rs.getString("DOB");
            String name = rs.getString("Name");
            int aid = rs.getInt("AID");
            String status = rs.getString("Status");
            users.add(new User(uid, sin, email, occupation, password, dob, name, aid, status));
        }
        return users;
    }

    public void reviewUser(int reviewer, int reviewee, String review, int rating) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Reviews" +
                "(Reviewer, Reviewee, Comment, Rating) VALUES (?, ?, ?, ?)");
        stmt.setInt(1, reviewer);
        stmt.setInt(2, reviewee);
        stmt.setString(3, review);
        stmt.setInt(4, rating);
        stmt.executeUpdate();
    }

    public void deleteRenter(int uid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Bookings " +
                "WHERE Status = 'UPCOMING' AND RID = ?");
        stmt.setInt(1, uid);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            int lid = rs.getInt("LID");
            int bid = rs.getInt("BID");
            String startDate = rs.getString("StartDate");
            String endDate = rs.getString("EndDate");
            updateCalendar(lid, startDate, endDate, "AVAILABLE");
            updateBooking(bid);
        }
        PreparedStatement stmt1 = conn.prepareStatement("UPDATE Users SET Status = 'INACTIVE' WHERE UID = ?");
        stmt1.setInt(1, uid);
        stmt1.executeUpdate();
        // remove listings
        // cancel relevant bookings
    }

    public void removeListing(int lid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Bookings " +
                "WHERE Status = 'UPCOMING' AND LID = ?");
        stmt.setInt(1, lid);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            int bid = rs.getInt("BID");
            String startDate = rs.getString("StartDate");
            String endDate = rs.getString("EndDate");
            updateCalendar(lid, startDate, endDate, "AVAILABLE");
            updateBooking(bid);
        }
        PreparedStatement stmt1 = conn.prepareStatement("UPDATE Listings SET Status = 'INACTIVE' WHERE LID = ?");
        stmt1.setInt(1, lid);
        stmt1.executeUpdate();
        // cancel relevant bookings
    }

    public void deleteHost(int uid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Listings " +
                "WHERE Status = 'ACTIVE' AND UID = ?");
        stmt.setInt(1, uid);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            int lid = rs.getInt("LID");
            removeListing(lid);
        }
        PreparedStatement stmt1 = conn.prepareStatement("UPDATE Users SET Status = 'INACTIVE' WHERE UID = ?");
        stmt1.setInt(1, uid);
        stmt1.executeUpdate();
    }

    public void close() throws SQLException {
        conn.close();
    }
}
