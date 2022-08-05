package project;

import java.sql.SQLException;

public class Booking {
    private int bid;
    private int rid;
    private int lid;
    private String startDate;
    private String endDate;
    private double cost;
    private String status;
    private String review;
    private int rating;

    public Booking(int bid, int rid, int lid, String startDate, String endDate,
                   double cost, String status, String review, int rating) {
        this.bid = bid;
        this.rid = rid;
        this.lid = lid;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cost = cost;
        this.status = status;
        this.review = review;
        this.rating = rating;
    }

    public static Booking create(DAO dao, int uid, int lid, String startDate, String endDate) throws SQLException {

        if (dao.checkAvailability(lid, startDate, endDate)) {
            Double cost = dao.getCost(lid, startDate, endDate);
            if (cost == -1) {
                System.out.println("Something went wrong computing the cost");
                return null;
            } else {
                dao.updateCalendar(lid, startDate, endDate, "BOOKED");
                return dao.createBooking(uid, lid, startDate, endDate, cost);
            }
        } else {
            System.out.println("Booking Failed: Listing is not available for the given date range");
            return null;
        }
    }

    public String display(DAO dao) {
        String str;
        try {
            Listing listing = dao.getListingFromID(lid);
            str =  listing.toString();
        } catch (SQLException e) {
            e.printStackTrace();
            str = "listing id: " + lid;
        }
        if (status.equals("PAST") && review != null) {
            return str + " from " + startDate + " to " + endDate + " for cost of " + cost +
                    "\nreview: " + review + "\nrating: " + rating;
        } else {
            return str + " from " + startDate + " to " + endDate + " for cost of " + cost;
        }
    }

    public double getCost() {
        return cost;
    }

    public int getLid() { return lid; }

    public int getBid() { return bid; }

    public String getStartDate() { return startDate; }

    public String getEndDate() { return endDate; }

    public String getReview() { return review; }
}
