package project;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    /* Returns the number of availabilities created */
    public int createAvailabilitiesInRange(int lid, String start, String end, double price) throws SQLException {
        LocalDate curDate = LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDate = LocalDate.parse(end, DateTimeFormatter.ISO_LOCAL_DATE);

        endDate = endDate.plusDays(1); // add 1 day to include range's endpoints
        int count = 0;

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
                count++;
            }
            curDate = curDate.plusDays(1);
        }
        return count;
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

    public ArrayList<Listing> getListingsFromView(String view, String str,
                                                  boolean coordinateSearch) throws SQLException {
        PreparedStatement stmt;

        if (str.equals("ASC") || str.equals("DESC")) {
            stmt = conn.prepareStatement("SELECT L.*, AVG(Price) AS Price FROM "+view+" L, Calendars C " +
                    "WHERE L.LID=C.LID GROUP BY L.LID ORDER BY Price " + str);
        } else {
            stmt = conn.prepareStatement("(SELECT L.*, AVG(Price) AS Price FROM "+view+" L, Calendars C " +
                    "WHERE L.LID=C.LID GROUP BY L.LID) UNION (SELECT L.*, -1 AS Price FROM "+view+" L, Calendars C " +
                    "WHERE L.LID NOT IN (SELECT LID FROM Calendars))");
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

            double price = rs.getDouble("Price");
            String aux = price == -1 ? "" : "Price: " + price;

            if (coordinateSearch) {
                aux += " Distance: " + rs.getDouble("Distance");
            }

            result.add(new Listing(lid, type, latitude, longitude, newAddress, aux));
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
        while (rs.next()) {
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
        while (rs.next()) {
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
                "WHERE EndDate < CURDATE() AND Status='UPCOMING'");
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
        // remove listings
        // cancel relevant bookings
    }

    // Reports to support
    public int reportNumBookings(String startDate, String endDate, String postalCode) throws SQLException {
        // return num of bookings given a date range
        // group by city or postal code within a city
        PreparedStatement stmt;
        if (postalCode.equals("y")){
            stmt = conn.prepareStatement("SELECT City, PostalCode, COUNT(*) AS NumBookings " +
                    "FROM Bookings b, Listings l, Addresses a " +
                    "WHERE b.LID=l.LID AND l.AID=a.AID AND StartDate >= ? AND EndDate <= ? " +
                    "AND b.Status != 'CANCELLED' GROUP BY City, PostalCode");
        } else {
            stmt = conn.prepareStatement("SELECT City, COUNT(*) AS NumBookings " +
                    "FROM Bookings b, Listings l, Addresses a " +
                    "WHERE b.LID=l.LID AND l.AID=a.AID AND StartDate >= ? AND EndDate <= ? " +
                    "AND b.Status != 'CANCELLED' GROUP BY City");
        }
        stmt.setString(1, startDate);
        stmt.setString(2, endDate);
        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {
            String city = rs.getString("City");
            int num = rs.getInt("NumBookings");
            if (postalCode.equals("y")) {
                String code = rs.getString("PostalCode");
                System.out.println(city + ", " + code + ", " + num);
            }else {
                System.out.println(city + ", " + num);
            }
        }
        return 0;
    }

    public void reportNumListings(String includeCity, String includeCode) throws SQLException {
        // return num of listings by country, by country and city, by country, city, and postal code
        PreparedStatement stmt;
        if (includeCity.equals("y")){
            if (includeCode.equals(("y"))) {
                stmt = conn.prepareStatement("SELECT Country, City, PostalCode, COUNT(*) AS NumListings " +
                        "FROM Listings NATURAL JOIN Addresses WHERE Status = 'ACTIVE' " +
                        "GROUP BY Country, City, PostalCode");
            } else {
                stmt = conn.prepareStatement("SELECT Country, City, COUNT(*) AS NumListings " +
                        "FROM Listings NATURAL JOIN Addresses WHERE Status = 'ACTIVE' " +
                        "GROUP BY Country, City");
            }
        } else {
            stmt = conn.prepareStatement("SELECT Country, COUNT(*) AS NumListings " +
                    "FROM Listings NATURAL JOIN Addresses WHERE Status = 'ACTIVE' " +
                    "GROUP BY Country");
        }
        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {
            String country = rs.getString("Country");
            int num = rs.getInt("NumListings");
            if (includeCity.equals("y")) {
                String city = rs.getString("City");
                if(includeCode.equals("y")) {
                    String code = rs.getString("PostalCode");
                    System.out.println(country + ", " + city + ", " + code + ", " + num);
                } else {
                    System.out.println(country + ", " + city + ", " + num);
                }
            }else {
                System.out.println(country + ", " + num);
            }
        }
    }

    public void rankHosts(String input) throws SQLException {
        // rank hosts by total number of listings by country (optionally by city)
        PreparedStatement stmt;
        if (input.equals("y")) {
            stmt = conn.prepareStatement("SELECT Country, City, Name, Count(*) as Num FROM " +
                    "Listings l, Addresses a, Users u WHERE l.AID=a.AID and l.UID=u.UID and l.Status='ACTIVE' " +
                    "GROUP BY Country, City, l.UID ORDER BY count(*) DESC, Country DESC, City DESC");
        } else {
            stmt = conn.prepareStatement("SELECT Country, Name, Count(*) as Num FROM " +
                    "Listings l, Addresses a, Users u WHERE l.AID=a.AID and l.UID=u.UID and l.Status='ACTIVE' " +
                    "GROUP BY Country, l.UID ORDER BY count(*) DESC, Country DESC");
        }
        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {
            String country = rs.getString("Country");
            String name = rs.getString("Name");
            int num = rs.getInt("Num");
            if (input.equals("y")) {
                String city = rs.getString("City");
                System.out.println(country + ", " + city + ", " + name + ", " + num);
            } else {
                System.out.println(country + ", " + name + ", " + num);
            }
        }
    }

    /* Reports the hosts that have a num of listings more than 10% the num of listings in that city, country */
    public void reportHost() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS NumListing, City, Country, u.Name " +
                "FROM Listings l NATURAL JOIN Addresses a1, Users u " +
                "WHERE l.Status='ACTIVE' AND u.UID=l.UID " +
                "GROUP BY Country, City, l.UID " +
                "HAVING NumListing >= 0.1 * ( " +
                "    SELECT COUNT(*) " +
                "    FROM Listings c NATURAL JOIN Addresses a2 " +
                "    WHERE Status='ACTIVE' AND a2.Country=a1.Country AND a1.City=a2.City) " +
                "ORDER BY City, Country");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String name = rs.getString("Name");
            String city = rs.getString("City");
            String country = rs.getString("Country");
            int numListing = rs.getInt("NumListing");
            if (numListing == 1) {
                System.out.println(name + " with " + numListing + " listing in " + city + ", " + country);
            } else {
                System.out.println(name + " with " + numListing + " listings in " + city + ", " + country);
            }
        }
    }

    /* Ranks renters by num of bookings */
    public void rankRenters(String startDate, String endDate, String input) throws SQLException {
        PreparedStatement stmt;
        if (input.equals("y")) {
            stmt = conn.prepareStatement("SELECT Name, COUNT(b.RID) AS Num, City, Country " +
                    "FROM Bookings b, Users u, Addresses a, Listings l " +
                    "WHERE b.StartDate >= ? AND b.EndDate <= ? AND b.RID=u.UID AND b.LID=l.LID AND l.AID=a.AID AND b.Status!='CANCELLED' " +
                    "GROUP BY b.RID, City, Country " +
                    "HAVING COUNT(b.RID) >= 2 " +
                    "ORDER BY Num DESC, Country DESC, City DESC");
        } else {
            stmt = conn.prepareStatement("SELECT Name, COUNT(b.RID) AS Num FROM Bookings b, Users u " +
                    "WHERE b.StartDate >= ? AND b.EndDate <= ? AND b.RID=u.UID AND b.Status!='CANCELLED' " +
                    "GROUP BY b.RID " +
                    "ORDER BY Num DESC");
        }
        stmt.setString(1, startDate);
        stmt.setString(2, endDate);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String name = rs.getString("Name");
            int num = rs.getInt("Num");
            if (input.equals("y")) {
                String country = rs.getString("Country");
                String city = rs.getString("City");
                System.out.println(name + ", " + city + ", " + country + ", " + num);
            } else {
                System.out.println(name + ", " + num);
            }
        }
    }

    public void reportCancellations(String hostRenter, String year) throws SQLException {
        PreparedStatement stmt;
        if (hostRenter.equals("host")) {
            stmt = conn.prepareStatement("SELECT Name, COUNT(*) AS Num " +
                    "FROM Users u, Bookings b, Listings l " +
                    "WHERE b.Status='CANCELED' AND b.lid=l.LID AND l.uid=u.UID AND (YEAR(b.StartDate)=? OR YEAR(b.EndDate)=?) " +
                    "GROUP BY u.UID " +
                    "ORDER BY Num DESC " +
                    "LIMIT 5");
        } else if (hostRenter.equals("renter")) {
            stmt = conn.prepareStatement("SELECT Name, COUNT(*) AS Num " +
                    "FROM Users u, Bookings b " +
                    "WHERE u.UID=b.RID AND b.Status='CANCELED' AND (YEAR(b.StartDate)=? OR YEAR(b.EndDate)=?) " +
                    "GROUP BY u.UID " +
                    "ORDER BY Num DESC " +
                    "LIMIT 5");
        } else {
            stmt = conn.prepareStatement("(SELECT Name, COUNT(*) AS Num " +
                    "FROM Users u, Bookings b " +
                    "WHERE u.UID=b.RID AND b.Status='CANCELED' AND (YEAR(b.StartDate)=? OR YEAR(b.EndDate)=?) " +
                    "GROUP BY u.UID) " +
                    "UNION  " +
                    "(SELECT Name, COUNT(*) AS Num " +
                    "FROM Users u, Bookings b, Listings l " +
                    "WHERE b.Status='CANCELED' AND b.lid=l.LID AND l.uid=u.UID AND (YEAR(b.StartDate)=? OR YEAR(b.EndDate)=?) " +
                    "GROUP BY u.UID) " +
                    "ORDER BY Num DESC " +
                    "LIMIT 5");
            stmt.setString(3, year);
            stmt.setString(4, year);
        }
        stmt.setString(1, year);
        stmt.setString(2, year);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String name = rs.getString("Name");
            int num = rs.getInt("Num");
            System.out.println(name + ", " + num);
        }
    }

    /* Returns a ResultSet that contains reviews for all active listings */
    public ResultSet getAllListingReviews() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT l.LID, b.Review " +
                "FROM Listings l, Bookings b " +
                "WHERE b.LID=l.LID AND l.Status='ACTIVE' AND b.review IS NOT NULL " +
                "ORDER BY l.LID");
        return stmt.executeQuery();
    }

    /* Returns a linkedhashmap of listings to number of reviews that listing has */
    public LinkedHashMap<Integer, Integer> getListingReviewNum() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT l.lid, COUNT(*) AS ReviewNum " +
                "FROM Listings l, Bookings b " +
                "WHERE b.LID=l.LID AND b.review IS NOT NULL " +
                "GROUP BY l.LID " +
                "ORDER BY l.LID");
        ResultSet rs = stmt.executeQuery();
        LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>();
        while (rs.next()) {
            int lid = rs.getInt("lid");
            int reviewNum = rs.getInt("ReviewNum");
            result.put(lid, reviewNum);
        }
        return result;
    }

    public List<Amenity> getAmenitiesListByLID(int lid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT Category, Description FROM Offers " +
                "NATURAL JOIN Amenities WHERE LID = ?");
        stmt.setInt(1, lid);
        ResultSet rs = stmt.executeQuery();
        List<Amenity> amenities = new ArrayList<>();
        while(rs.next()) {
            String description = rs.getString("Description");
            String category = rs.getString("Category");
            amenities.add(new Amenity(description, category));
        }
        return amenities;
    }

    public String amenitiesListToString(List<Amenity> amenities) {
        StringBuilder set = new StringBuilder();
        set.append("(");
        for (int i=0; i<amenities.size(); i++) {
            if (i==0) {
                set.append("'" + amenities.get(i).getDescription() + "'");
            } else {
                set.append("," + "'" + amenities.get(i).getDescription() + "'");
            }
        }
        set.append(")");
        return set.toString();
    }

    public double avgPriceOfListings(String type, List<Amenity> amenities, String country) throws SQLException{
        String set = amenitiesListToString(amenities);
        PreparedStatement stmt = null;
        if (amenities.isEmpty()) {
            stmt = conn.prepareStatement("WITH Filter1 AS " +
                    "(SELECT LID FROM Listings NATURAL JOIN Addresses WHERE Type = ? AND Country = ?), " +
                    "temp AS (SELECT AVG(Price) AS Price FROM Calendars " +
                    "WHERE LID IN (SELECT * FROM Filter1) " +
                    "GROUP BY LID) SELECT AVG(Price) AS RESULT FROM temp");
        } else {
            stmt = conn.prepareStatement("WITH Filter1 AS " +
                    "(SELECT LID FROM Listings NATURAL JOIN Addresses WHERE Type = ? AND Country = ?), " +
                    "Filter2 AS (SELECT LID FROM Listings NATURAL JOIN Offers " +
                    "WHERE Description IN " + set + " GROUP BY LID HAVING COUNT(*)=" + amenities.size() + "), " +
                    "temp AS (SELECT AVG(Price) AS Price FROM Calendars " +
                    "WHERE LID IN (SELECT * FROM Filter1) AND LID IN (SELECT * FROM Filter2) " +
                    "GROUP BY LID) SELECT AVG(Price) AS RESULT FROM temp");
        }
        stmt.setString(1, type);
        stmt.setString(2, country);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getDouble("RESULT");
        }
        return -1;
    }

    public double avgPriceOfListings(String type, List<Amenity> amenities, String country,
                                     String city) throws SQLException{
        String set = amenitiesListToString(amenities);
        PreparedStatement stmt = null;
        if (amenities.isEmpty()) {
            stmt = conn.prepareStatement("WITH Filter1 AS " +
                    "(SELECT LID FROM Listings NATURAL JOIN Addresses " +
                    "WHERE Type = ? AND Country = ? AND City = ?), " +
                    "temp AS (SELECT AVG(Price) AS Price FROM Calendars " +
                    "WHERE LID IN (SELECT * FROM Filter1) " +
                    "GROUP BY LID) SELECT AVG(Price) AS RESULT FROM temp");
        } else {
            stmt = conn.prepareStatement("WITH Filter1 AS " +
                    "(SELECT LID FROM Listings NATURAL JOIN Addresses " +
                    "WHERE Type = ? AND Country = ? AND City = ?), " +
                    "Filter2 AS (SELECT LID FROM Listings NATURAL JOIN Offers " +
                    "WHERE Description IN " + set + " GROUP BY LID HAVING COUNT(*)=" + amenities.size() + "), " +
                    "temp AS (SELECT AVG(Price) AS Price FROM Calendars " +
                    "WHERE LID IN (SELECT * FROM Filter1) AND LID IN (SELECT * FROM Filter2) " +
                    "GROUP BY LID) SELECT AVG(Price) AS RESULT FROM temp");
        }
        stmt.setString(1, type);
        stmt.setString(2, country);
        stmt.setString(3, city);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getDouble("RESULT");
        }
        return -1;
    }

    public double avgPriceOfListings(String type, List<Amenity> amenities, String country,
                                     String city, String postalCode) throws SQLException{
        String set = amenitiesListToString(amenities);
        PreparedStatement stmt = null;
        if (amenities.isEmpty()) {
            stmt = conn.prepareStatement("WITH Filter1 AS " +
                    "(SELECT LID FROM Listings NATURAL JOIN Addresses " +
                    "WHERE Type = ? AND Country = ? AND City = ? AND PostalCode = ?), " +
                    "temp AS (SELECT AVG(Price) AS Price FROM Calendars " +
                    "WHERE LID IN (SELECT * FROM Filter1) " +
                    "GROUP BY LID) SELECT AVG(Price) AS RESULT FROM temp");
        } else {
            stmt = conn.prepareStatement("WITH Filter1 AS " +
                    "(SELECT LID FROM Listings NATURAL JOIN Addresses " +
                    "WHERE Type = ? AND Country = ? AND City = ? AND PostalCode = ?), " +
                    "Filter2 AS (SELECT LID FROM Listings NATURAL JOIN Offers " +
                    "WHERE Description IN " + set + " GROUP BY LID HAVING COUNT(*)=" + amenities.size() + "), " +
                    "temp AS (SELECT AVG(Price) AS Price FROM Calendars " +
                    "WHERE LID IN (SELECT * FROM Filter1) AND LID IN (SELECT * FROM Filter2) " +
                    "GROUP BY LID) SELECT AVG(Price) AS RESULT FROM temp");
        }
        stmt.setString(1, type);
        stmt.setString(2, country);
        stmt.setString(3, city);
        stmt.setString(4, postalCode);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getDouble("RESULT");
        }
        return -1;
    }

    public List<Amenity> getCommonEssentials(List<Amenity> offered) throws SQLException {
//        Map<Amenity, Double> result = new HashMap<>();
        String set = amenitiesListToString(offered);
        List<Amenity> result = new ArrayList<>();
        PreparedStatement stmt = null;
        if (offered.isEmpty()) {
            stmt = conn.prepareStatement("SELECT Amenities.*, COUNT(*)/" +
                    "(SELECT COUNT(*) FROM LISTINGS) as Prop FROM Offers " +
                    "NATURAL JOIN Amenities WHERE Category IN ('essentials', 'safety') " +
                    "GROUP BY Description ORDER BY Prop DESC");
        } else {
            stmt = conn.prepareStatement("SELECT Amenities.*, COUNT(*)/" +
                    "(SELECT COUNT(*) FROM LISTINGS) as Prop FROM Offers " +
                    "NATURAL JOIN Amenities WHERE Category IN ('essentials', 'safety') " +
                    "AND Description NOT IN " + set +
                    " GROUP BY Description ORDER BY Prop DESC");
        }

        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {
            String description = rs.getString("Description");
            String category = rs.getString("Category");
            Double ratio = rs.getDouble("Prop");
//            result.put(new Amenity(description, category), ratio);
            result.add(new Amenity(description, category));
        }
        return result;
    }

    public List<Amenity> getUncommonFeatures(List<Amenity> offered) throws SQLException {
//        Map<Amenity, Double> result = new HashMap<>();
        String set = amenitiesListToString(offered);
        List<Amenity> result = new ArrayList<>();
        PreparedStatement stmt = null;
        if (offered.isEmpty()) {
            stmt = conn.prepareStatement("SELECT Amenities.*, COUNT(*)/" +
                    "(SELECT COUNT(*) FROM LISTINGS) as Prop FROM Offers " +
                    "NATURAL JOIN Amenities WHERE Category='features' " +
                    "GROUP BY Description ORDER BY Prop ASC");
        } else {
            stmt = conn.prepareStatement("SELECT Amenities.*, COUNT(*)/" +
                    "(SELECT COUNT(*) FROM LISTINGS) as Prop FROM Offers " +
                    "NATURAL JOIN Amenities WHERE Category='features' " +
                    "AND Description NOT IN " + set +
                    "GROUP BY Description ORDER BY Prop ASC");
        }

        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {
            String description = rs.getString("Description");
            String category = rs.getString("Category");
            Double ratio = rs.getDouble("Prop");
//            result.put(new Amenity(description, category), ratio);
            result.add(new Amenity(description, category));
        }
        return result;
    }

    public boolean isLegalAge(String dob) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT TIMESTAMPDIFF(YEAR, ?, CURDATE()) >= 18 AS RESULT");
        stmt.setString(1, dob);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("RESULT") == 1;
        }
        return false;
    }

    public void updateUserStatus(int uid, String status) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE Users SET Status = ? WHERE UID = ?");
        stmt.setString(1, status);
        stmt.setInt(2, uid);
        stmt.executeUpdate();
    }

    public void updateCalendarsStatus() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE Calendars SET Status='UNAVAILABLE' " +
                "WHERE Day < CURDATE() AND Status = 'AVAILABLE'");
        stmt.executeUpdate();
    }

    public void close() throws SQLException {
        conn.close();
    }
}
