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

    public boolean createListing() {
        return false;
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
            int num = rs.getInt("NumBokings");
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
                    "GROUP BY Country, City, l.UID ORDER BY count(*) DESC");
        } else {
            stmt = conn.prepareStatement("SELECT Country, Name, Count(*) as Num FROM " +
                    "Listings l, Addresses a, Users u WHERE l.AID=a.AID and l.UID=u.UID and l.Status='ACTIVE' " +
                    "GROUP BY Country, l.UID ORDER BY count(*) DESC");
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

    // more reports to be added

    public void close() throws SQLException {
        conn.close();
    }
}
