package project;

import java.sql.SQLException;
import java.util.*;

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

        //scanner.nextLine();
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

    public static List<Listing> displayListings() throws SQLException {
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

        return listings;
    }

    /* Displays all host's listings and allows host to select which they want to update */
    public static void viewHostListings() {
        System.out.println("===== All active listings =====");
        try {
            boolean exit = false;
            while (!exit) {
                ArrayList<Listing> hostListings = dao.getListingsFromHost(loggedInUser.getUid());

                // print out the listings
                for (int i = 0; i < hostListings.size(); i++) {
                    System.out.println(i + 1 + ": " + hostListings.get(i));
                }

                // get input for updating listings
                System.out.print("Would you like to update a listing? (y/n): ");
                String input = scanner.next();

                if (input.equals("y")) {
                    System.out.print("Enter listing number to update: ");
                    int listing = scanner.nextInt();
                    if (listing - 1 >= 0 && listing - 1 < hostListings.size()) {
                        updateListing(hostListings.get(listing - 1).getLid());
                    } else {
                        System.out.println("Invalid input.");
                    }
                } else if (input.equals("n")) {
                    exit = true;
                } else {
                    System.out.println("Invalid input.");
                }
            }

        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("There was a problem getting listings.");
        }

    }

    /* Allows host to update listing with id lid */
    public static void updateListing(int lid) {
        System.out.println("LID IS: " + lid);
        System.out.println("Possible operations: ");
        System.out.println("1. Add availability");
        System.out.println("2. Modify availability");

        System.out.print("Select operation: ");
        int input = scanner.nextInt();

        if (input == 1) {
            addAvailability(lid);
        } else if (input == 2) {
            System.out.println("modify an availability (including price), only if it hasn't been booked");
        }
    }

    public static void addAvailability(int lid) {
        System.out.print("Enter start date for availability (YYYY-MM-DD): ");
        String startDate = scanner.next();

        System.out.print("Enter end date (YYYY-MM-DD): ");
        String endDate = scanner.next();

        System.out.print("Enter price: ");
        double price = scanner.nextDouble();

        // check that there are no availabilities there already
        try {
            if (dao.checkAvailabilitiesInRange(lid, startDate, endDate)) {
                System.out.println("Availabilities already exist within this range. New availabilities will " +
                        "be created around these old ones and old ones will remain unmodified.");
            }
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue accessing availabilities in database.");
        }

        // add the availabilities
        try {
            dao.createAvailabilitiesInRange(lid, startDate, endDate, price);
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue adding availabilities");
        }
    }

    public static void createListing() {
        System.out.print("Type of listing (House, Apartment, Guesthouse, Hotel): ");
        String type = scanner.next().toLowerCase(Locale.ROOT);

        System.out.print("Latitude (-90 to 90): ");
        double latitude = scanner.nextDouble();

        System.out.print("Longitude (-180 to 180): ");
        double longitude = scanner.nextDouble();

        scanner.nextLine(); // flush

        System.out.print("Address: ");
        String address = scanner.nextLine();

        System.out.print("City: ");
        String city = scanner.nextLine();

        System.out.print("Country: ");
        String country = scanner.nextLine();

        System.out.print("Postal Code: ");
        String postalCode = scanner.next();

        try {
            int lid = Listing.createListing(dao, loggedInUser.getUid(), type, latitude, longitude, address, city, country, postalCode);
            System.out.println("Listing Created Successfully!");

            addAmenities(lid);
        } catch (IllegalArgumentException iae) {
            System.out.println("Invalid input. Please insure fields are non-empty or type matches types listed.");
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("There was a problem adding that listing");
        }

    }

    public static void addAmenities(int lid) {
        System.out.println("===== Adding amenities to listing =====");

        try {
            // get lists of amenities of each category
            ArrayList<String>[] categories = new ArrayList[4];
            categories[0] = dao.getAmenitiesListByCategory("essentials");
            categories[1] = dao.getAmenitiesListByCategory("features");
            categories[2] = dao.getAmenitiesListByCategory("location");
            categories[3] = dao.getAmenitiesListByCategory("safety");

            // loop to get user input
            while (true) {
                System.out.println("Select category of amenity (-1 to exit):");
                System.out.println("1. Essentials");
                System.out.println("2. Features");
                System.out.println("3. Location");
                System.out.println("4. Safety");

                int input = scanner.nextInt();
                scanner.nextLine(); // flush

                if (input == -1) {
                    break;
                }

                // loop to add multiple amenities from category
                while (true) {
                    // print out amenities in that category
                    System.out.println("Available amenities to add in selected category: ");
                    System.out.println(categories[input-1]);

                    System.out.print("Enter amenity to add (q to quit): ");
                    String amenity = scanner.nextLine();

                    if (amenity.equals("q")) {
                        break;
                    }

                    // add amenity to the listing
                    if (categories[input-1].contains(amenity.toLowerCase(Locale.ROOT).trim())) {
                        categories[input-1].remove(amenity.toLowerCase(Locale.ROOT).trim());
                        dao.offerAmenity(lid, amenity);
                    } else {
                        System.out.println("Invalid amenity.");
                    }
                }
            }
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("There was a problem adding the amenity.");
        } catch (InputMismatchException ime) {
            System.out.printf("Invalid input");
        }

        System.out.println("Finished adding amenities");
    }

    public static void createBooking(List<Listing> listings) throws SQLException {
        System.out.print("Select a listing you would like to book: ");
        int input = scanner.nextInt();
        if (input < 0 || input > listings.size()) {
            System.out.println("Invalid Listing");
            return;
        }
        System.out.print("Enter start-date end-date (YYYY-MM-DD): ");
        String startDate = scanner.next();
        String endDate = scanner.next();
        Booking booking = Booking.create(dao, loggedInUser.getUid(),
                listings.get(input).getLid(), startDate, endDate);
        if (booking != null) {
            System.out.println("New booking was created for cost of " + booking.getCost());
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
                createListing();

                break;
            case 2:
//                displayListings(host)
                viewHostListings();
                //System.out.println("Select a listing you would like to update: ");
//                handleUpdateListing()
                break;
            case 3:
                try {
                    List<Booking> bookings = displayBookings("UPCOMING");
                    if (!bookings.isEmpty()) {
                        cancelBooking(bookings);
                    }
                } catch (Exception e) {
                    System.out.println("Something went wrong trying to retrieve bookings");
                }
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

    public static List<Booking> displayBookings(String status) throws SQLException {
        dao.updateBookingStatus();
        List<Booking> bookings;
        if (loggedInUser.getClass().equals(Renter.class)) {
            bookings = dao.getRentersBookings(status, loggedInUser.getUid());
        } else {
            bookings = dao.getHostsBookings(status, loggedInUser.getUid());
        }
        for (int i=0; i<bookings.size(); i++) {
            System.out.println(i + ") " + bookings.get(i).print(dao));
        }
        return bookings;
    }

    public static void cancelBooking(List<Booking> bookings) {
        System.out.println("Select a booking you would like to cancel: ");
    }

    public static void reviewBooking(List<Booking> bookings) {
        System.out.println("Select a booking you would like to review: ");
    }

    public static boolean handleRenterInput(int choice) {
        boolean isLoggedIn = true;
        switch (choice) {
            case 1:
                try{
                    List<Listing> listings = displayListings();
                    if (!listings.isEmpty()) {
                        createBooking(listings);
                    }
                } catch (SQLException sql) {
                    sql.printStackTrace();
                    System.out.println("Something went wrong trying to retrieve listings or create a booking");
                }
                break;
            case 2:
                try {
                    List<Booking> bookings = displayBookings("UPCOMING");
                    if (!bookings.isEmpty()) {
                        cancelBooking(bookings);
                    }
                } catch (Exception e) {
                    System.out.println("Something went wrong trying to retrieve bookings");
                }
                break;
            case 3:
                try {
                    List<Booking> bookings = displayBookings("PAST");
                    if (!bookings.isEmpty()) {
                        reviewBooking(bookings);
                    }
                } catch (Exception e) {
                    System.out.println("Something went wrong trying to retrieve bookings");
                }
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
