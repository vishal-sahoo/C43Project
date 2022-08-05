package project;

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

    @Override
    public String toString() {
        return date + ": " + status + " for $" + price;
    }
}
