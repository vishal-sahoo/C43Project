package project;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class Driver {

    public static final String dbName = "C43Project";
    public static final String user = "root";
    public static final String password = "HAVISHU19";

    private static Scanner scanner;
    private static DAO dao;
    private static User loggedInUser = null;

    public static void displayDefaultMenu() {
        System.out.println("1: Sign Up");
        System.out.println("2: Log In");
        System.out.println("3: Delete User");
        System.out.println("4: View Reports");
        System.out.println("5: Exit");
    }

    public static void displayRenterMenu() {
        System.out.println("1: View Listings"); // can select a listing to proceed to booking
        System.out.println("2: View Upcoming Bookings"); // -> can cancel a booking
        System.out.println("3: View Past Bookings"); // -> can select a booking to leave a review
        System.out.println("4: Leave A Review About A Host");
        System.out.println("5: Log Out");
    }

    public static void displayHostMenu() {
        System.out.println("1: Create A Listing"); // -> take necessary input
        System.out.println("2: View Your Listings"); // -> can select a listing and then update price or availability
        System.out.println("3: View Upcoming Bookings"); // -> can cancel a booking
        System.out.println("4: Leave A Review About A Renter"); // -> take text and rating input
        System.out.println("5: Log Out");
    }

    public static boolean signup() {
        System.out.print("Enter 1 for Renter or 2 for Host: ");
        int choice = scanner.nextInt();

        scanner.nextLine();
        System.out.print("Name: ");
        String name = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.next();

        System.out.print("Password: ");
        String password = scanner.next();

        System.out.print("SIN: ");
        String sin = scanner.next();

        System.out.print("DOB (YYYY-MM-DD): ");
        String dob = scanner.next();

        scanner.nextLine();
        System.out.print("Occupation: ");
        String occupation = scanner.nextLine();

        System.out.print("Address: ");
        String address = scanner.nextLine();

        System.out.print("City: ");
        String city = scanner.nextLine();

        System.out.print("Country: ");
        String country = scanner.nextLine();

        System.out.print("Postal Code: ");
        String postalCode = scanner.next();

        try {
            if (choice == 1) {
                System.out.print("Credit Card: ");
                String creditCard = scanner.next();
                if (!Renter.signup(dao, name, email, password, sin, dob, occupation,
                        address, city, country, postalCode, creditCard)) {
                    System.out.println("Email already exists!");
                } else {
                    return true;
                }
            } else if (choice == 2){
                if (!Host.signup(dao, name, email, password, sin, dob, occupation,
                        address, city, country, postalCode)) {
                    System.out.println("Email already exists!");
                } else {
                    return true;
                }
            } else {
                System.out.println("Invalid option");
            }
        } catch (IllegalArgumentException iae) {
            System.out.println("Please ensure all fields are non-empty");
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("There was a problem signing you up");
        }
        return false;
    }

    public static boolean login() {
        System.out.print("Enter 1 for Renter or 2 for Host: ");
        int choice = scanner.nextInt();

        System.out.print("Email: ");
        String email = scanner.next();

        System.out.print("Password: ");
        String password = scanner.next();

        try {
            if (choice == 1) {
                loggedInUser = Renter.login(dao, email, password);
                return loggedInUser!=null;
            } else if (choice == 2) {
                loggedInUser = Host.login(dao, email, password);
                return loggedInUser!=null;
            } else {
                System.out.println("Invalid option");
                return false;
            }
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("There was a problem with login");
        }
        return false;
    }

    public static void displayListings() throws SQLException {
        System.out.println("Choose base search option: ");
        System.out.println("1: All listings");
        System.out.println("2: Listings near a coordinate");
        System.out.println("3: Listings near a postal code");
        System.out.println("4: Listings at an address");

        System.out.print("Enter Input: ");
        int input = scanner.nextInt();

        boolean coordinateSearch = false;
//        String attributes = "L.LID as LID, UID, Type, Latitude, Longitude, L.Status as Status, " +
//                "Address, City, Country, PostalCode";
        StringBuilder query = new StringBuilder();

        query.append("SELECT * FROM Listings NATURAL JOIN Addresses WHERE Status='ACTIVE'");
        switch (input) {
            case 2:
                System.out.print("Enter latitude (-90 to 90) longitude (-180 to 180): ");
                double latitude = scanner.nextDouble();
                double longitude = scanner.nextDouble();
                System.out.print("Specify distance(km) or enter -1 for default: ");
                double distance = scanner.nextDouble()*1000;
                if (distance < 0) {
                    distance = 5000;
                }
                // "SELECT * FROM Listings NATURAL JOIN ADDRESSES WHERE "
                query.setLength(0);
                query.append("WITH temp AS (SELECT *, ST_Distance_Sphere(point("+latitude+", "+longitude+"), " +
                        "point(Latitude, Longitude)) as Distance FROM Listings NATURAL JOIN Addresses " +
                        "WHERE Status='ACTIVE') " +
                        "SELECT * FROM temp WHERE Distance <= " +distance+ " ORDER BY Distance");
//                query.append(" AND ST_Distance_Sphere(point("+latitude+", "+longitude+"), " +
//                        "point(Latitude, Longitude)) <= "+distance);
                coordinateSearch = true;
                break;
            case 3:
                System.out.print("Enter postal code (length >3): ");
                String postalCode = scanner.next().toLowerCase(Locale.ROOT).substring(0, 3);
                // "SELECT * FROM Listings NATURAL JOIN ADDRESSES " +
                //                        "WHERE
                query.append(" AND SUBSTRING(PostalCode, 1, 3)='" + postalCode + "'");
                break;
            case 4:
                scanner.nextLine();
                System.out.print("Enter address line 1: ");
                String address = scanner.nextLine().toLowerCase(Locale.ROOT).trim();
                System.out.print("City: ");
                String city = scanner.nextLine().toLowerCase(Locale.ROOT).trim();
                System.out.print("Country: ");
                String country = scanner.nextLine().toLowerCase(Locale.ROOT).trim();
//                SELECT * FROM Listings NATURAL JOIN Addresses " +
//                "WHERE
                query.append((" AND Address='%s' AND City='%s' AND Country='%s'").formatted(address, city, country));
                break;
            default:
                break;
        }

//        System.out.println(query);

        dao.deleteView("Base");
        dao.deleteView("Filter1");
        dao.deleteView("Filter2");
        dao.deleteView("Filter3");

        dao.createView("Base", query.toString());

        StringBuilder query1 = new StringBuilder();

        System.out.print("Would you like to filter by date range? (y/n): ");
        String response = scanner.next();
        if (response.toLowerCase().equals("y")) {
            System.out.print("Enter date range YYY-MM-DD YYY-MM-DD: ");
            String startDate = scanner.next();
            String endDate = scanner.next();
            query1.append("SELECT * FROM Base WHERE LID IN (SELECT L.LID FROM Listings L, Calendars C " +
                    "WHERE L.LID=C.LID AND C.Status='AVAILABLE' AND " +
                    "Day BETWEEN '%s' AND '%s' ".formatted(startDate, endDate) +
                    "GROUP BY L.LID HAVING COUNT(*)=DATEDIFF('%s', '%s')+1)".formatted(endDate, startDate));
            dao.createView("Filter1", query1.toString());
        } else {
            dao.createView("Filter1", "SELECT * FROM BASE");
        }

        StringBuilder query2 = new StringBuilder();

        System.out.print("Would you like to filter by price range? (y/n): ");
        response = scanner.next();
        if (response.toLowerCase().equals("y")) {
            System.out.print("Enter price range xx yy: ");
            Double min = scanner.nextDouble();
            Double max = scanner.nextDouble();
            query2.append("SELECT * FROM Filter1 WHERE LID IN (SELECT L.LID FROM LISTINGS L, CALENDARS C "+
                    "WHERE L.LID=C.LID GROUP BY L.LID HAVING AVG(Price) BETWEEN "+min+" AND "+max+")");
            dao.createView("Filter2", query2.toString());
        } else {
            dao.createView("Filter2", "SELECT * FROM Filter1");
        }

        StringBuilder query3 = new StringBuilder();

        System.out.print("Would you like to filter by amenities offered? (y/n): ");
        response = scanner.next();
        if (response.toLowerCase().equals("y")) {
            scanner.nextLine();
            System.out.print("Enter amenities (comma separated): ");
            String str = scanner.nextLine();
            String [] amenities = str.split(",");
            StringBuilder set = new StringBuilder();
            set.append("(");
            for (int i=0; i<amenities.length; i++) {
                if (i==0) {
                    set.append("'" + amenities[i] + "'");
                } else {
                    set.append("," + "'" + amenities[i] + "'");
                }
            }
            set.append(")");
            query3.append("SELECT * FROM Filter2 WHERE LID IN (SELECT LID FROM Listings NATURAL JOIN Offers " +
                    "WHERE Description IN " + set + " GROUP BY LID HAVING COUNT(*)=" + amenities.length + ")");
            dao.createView("Filter3", query3.toString());
        } else {
            dao.createView("Filter3", "SELECT * FROM Filter2");
        }

        ArrayList<Listing> listings;
        if (!coordinateSearch) {
            listings = dao.getListingsFromFilter3("");
        } else {
            System.out.print("Would you like to rank by price? (asc/desc/n): ");
            String str = scanner.next().trim().toUpperCase(Locale.ROOT);
            listings = dao.getListingsFromFilter3(str);
        }

        dao.deleteView("Base");
        dao.deleteView("Filter1");
        dao.deleteView("Filter2");
        dao.deleteView("Filter3");

        for (int j=0; j<listings.size(); j++) {
            System.out.println(j + ") " + listings.get(j));
        }
    }
    public static boolean handleDefaultInput(int choice) {
        boolean isLoggedIn = false;
        switch (choice) {
            case 1:
                if (signup()) {
                    System.out.println("Sign up successful!");
                }else {
                    System.out.println("Please try again");
                }
                break;
            case 2:
                boolean loginSuccessful = login();
                if (loginSuccessful) {
                    System.out.println("Log In Successful!");
                    isLoggedIn = true;
                } else {
                    System.out.println("Log In Unsuccessful");
                }
                break;
            case 3:
//                deleteUser()
                System.out.println("User Deleted Successfully");
                break;
            case 4:
//                handleReports()
                break;
            default:
                System.out.println("Invalid Choice");
                break;
        }
        return isLoggedIn;
    }

    public static boolean handleHostInput(int choice) {
        boolean isLoggedIn = true;
        switch (choice) {
            case 1:
//                getListingInput()
//                hostToolKit()
//                createListing()
                System.out.println("Listing Created Successfully!");
                break;
            case 2:
//                displayListings(host)
                System.out.println("Select a listing you would like to update: ");
//                handleUpdateListing()
                break;
            case 3:
//                displayUpcomingBookings(host)
                System.out.println("Select a booking you would like to cancel: ");
//                handleCancelBooking()
                break;
            case 4:
//                displayRenters(host)
                System.out.println("Select a renter you would like to review: ");
//                handleLeaveReview()
                break;
            case 5:
                System.out.println("Thank you for using MyBnB!");
                isLoggedIn = false;
                loggedInUser = null;
                break;
            default:
                System.out.println("Invalid Choice");
                break;
        }
        return isLoggedIn;
    }

    public static boolean handleRenterInput(int choice) {
        boolean isLoggedIn = true;
        switch (choice) {
            case 1:
                try{
                    displayListings();
                    System.out.println("Select a listing you would like to book: ");
//                    handleBooking()
                } catch (SQLException sql) {
                    sql.printStackTrace();
                    System.out.println("Something went wrong");
                }
                break;
            case 2:
//                displayUpcomingBookings(renter)
                System.out.println("Select a booking you would like to cancel: ");
//                handleCancelBooking()
                break;
            case 3:
//                displayPastBookings(renter)
                System.out.println("Select a booking you would like to review: ");
//                handleReviewBooking()
                break;
            case 4:
//                displayHosts(renter)
                System.out.println("Select a host you would like to review: ");
//                handleReviewHost()
                break;
            case 5:
                System.out.println("Thank you for using MyBnB!");
                isLoggedIn = false;
                break;
            default:
                System.out.println("Invalid Choice");
                break;
        }
        return isLoggedIn;
    }

    public static void main(String [] args) {
        try {
            boolean isLoggedIn = false;

            dao = new DAO(dbName, user, password);

            scanner = new Scanner(System.in);
            System.out.println("Welcome to MyBnB!");

            while (true) {
                System.out.println("Choose one of the following options:");

                if (isLoggedIn) {
                    if (loggedInUser.getClass() == Renter.class) {
                        displayRenterMenu();
                        System.out.print("Enter Input: ");
                        isLoggedIn = handleRenterInput(scanner.nextInt());
                    } else {
                        displayHostMenu();
                        System.out.print("Enter Input: ");
                        isLoggedIn = handleHostInput(scanner.nextInt());
                    }
                } else {
                    displayDefaultMenu();

                    System.out.print("Enter Input: ");
                    int choice = scanner.nextInt();
                    if (choice == 5) {
                        break;
                    }
                    isLoggedIn = handleDefaultInput(choice);
                }

                String separator = "-";
                System.out.println(separator.repeat(100));
            }

            dao.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
