package project;

import java.sql.*;

public class DAO {

    public final Connection conn;

    public DAO(String dbName, String user, String password) throws SQLException {
        String url = "jdbc:mysql://localhost/" + dbName;
        conn = DriverManager.getConnection(url, user, password);
    }

    // Operations to Support
    public boolean createUser() throws SQLException {
        /* Execute a query and iterate through the resulting  tuples. */
            PreparedStatement execStat = conn.prepareStatement(
                    "SELECT * FROM student");
            ResultSet rs = execStat.executeQuery();
        /* Extract data from result set*/
//            while (rs.next()) {
//                int sid = rs.getInt("sid");
//                String sname = rs.getString("firstName");
//                String rating = rs.getString("campus");
//                String age = rs.getString("email");
//                System.out.println(sname + ", " + rating + ", " + age);
//                /* Continued ... */
//            }

//            rs.close();
//            execStat.close();
        return false;
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
