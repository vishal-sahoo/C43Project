package project;

import java.sql.SQLException;

public class Calendar {
    private int lid;
    private String date;
    private double price;
    private String status;

    public Calendar(int lid, String date, double price, String status) {
        this.lid = lid;
        this.date = date;
        this.price = price;
        this.status = status;
    }

    public static boolean checkAvailabilitiesInRange(DAO dao, int lid, String startDate, String endDate) throws SQLException {
        return dao.checkAvailabilitiesInRange(lid, startDate, endDate);
    }

    public static boolean checkBookedInRange(DAO dao, int lid, String startDate, String endDate) throws SQLException {
        return dao.checkBookedInRange(lid, startDate, endDate);
    }

    /* Returns the number of availabilities created */
    public static int createAvailability(DAO dao, int lid, String startDate, String endDate, double price) throws SQLException {
        return dao.createAvailabilitiesInRange(lid, startDate, endDate, price);
    }

    public static int updateAvailabilityInRange(DAO dao, int lid, String startDate, String endDate, double price) throws SQLException {
        return dao.updateAvailabilityInRange(lid, startDate, endDate, price);
    }

    public static int cancelAvailabilitiesInRange(DAO dao, int lid, String startDate, String endDate) throws SQLException {
        return dao.cancelAvailabilitiesInRange(lid, startDate, endDate);
    }

    @Override
    public String toString() {
        return date + ": " + status + " for $" + price;
    }
}
