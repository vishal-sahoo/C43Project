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
        System.out.println("3: View Reports");
        System.out.println("4: Exit");
    }

    public static void displayRenterMenu() {
        System.out.println("1: View Listings"); // can select a listing to proceed to booking
        System.out.println("2: View Upcoming Bookings"); // -> can cancel a booking
        System.out.println("3: View Past Bookings"); // -> can select a booking to leave a review
        System.out.println("4: Leave A Review About A Host");
        System.out.println("5: Delete Account");
        System.out.println("6: Log Out");
    }

    public static void displayHostMenu() {
        System.out.println("1: Create A Listing"); // -> take necessary input
        System.out.println("2: View Your Listings"); // -> can select a listing and then update price or availability
        System.out.println("3: View Upcoming Bookings"); // -> can cancel a booking
        System.out.println("4: Leave A Review About A Renter"); // -> take text and rating input
        System.out.println("5: Delete Account");
        System.out.println("6: Log Out");
    }

    public static boolean signup() {
        System.out.print("Enter 1 for Renter or 2 for Host: ");
        int choice = scanner.nextInt();

        System.out.print("Email: ");
        String email = scanner.next();

        System.out.print("Password: ");
        String password = scanner.next();

        boolean exists = false;
        try {
            String creditCard = null;
            if (choice == 1) {
                System.out.print("Credit Card: ");
                creditCard = scanner.next();
                exists = Renter.exists(dao, email, password, creditCard);
            } else if (choice == 2){
                exists = Host.exists(dao, email, password);
            } else {
                System.out.println("Invalid option");
                return false;
            }

            if (exists) {
                return true;
            }

            scanner.nextLine();
            System.out.print("Name: ");
            String name = scanner.nextLine();

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

            if (choice == 1) {
//                System.out.print("Credit Card: ");
//                String creditCard = scanner.next();
                return Renter.signup(dao, name, email, password, sin, dob, occupation,
                        address, city, country, postalCode, creditCard);
            } else if (choice == 2){
                return Host.signup(dao, name, email, password, sin, dob, occupation,
                        address, city, country, postalCode);
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
//        String password = String.valueOf(System.console().readPassword());

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
//        dao.updateCalendarsStatus();
        System.out.println("Choose base search option: ");
        System.out.println("1: All listings");
        System.out.println("2: Listings near a coordinate");
        System.out.println("3: Listings near a postal code");
        System.out.println("4: Listings at an address");

        System.out.print("Enter Input: ");
        int input = scanner.nextInt();

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

        String [] views = {"Base", "Filter1", "Filter2", "Filter3", "Filter4"};
        for (String s: views) {
            dao.deleteView(s);
        }

        dao.createView("Base", query.toString());

        StringBuilder query1 = new StringBuilder();

        System.out.print("Would you like to filter by date range? (y/n): ");
        String response = scanner.next();
        if (response.equalsIgnoreCase("y")) {
            System.out.print("Enter date range YYYY-MM-DD YYYY-MM-DD: ");
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
        if (response.equalsIgnoreCase("y")) {
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
        if (response.equalsIgnoreCase("y")) {
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

        StringBuilder query4 = new StringBuilder();

        System.out.print("Would you like to filter by type? (y/n): ");
        response = scanner.next();
        if (response.equalsIgnoreCase("y")) {
            System.out.print("Enter type: ");
            String type = scanner.next().trim().toLowerCase(Locale.ROOT);
            query4.append("SELECT * FROM Filter3 WHERE Type = '%s'".formatted(type));
            dao.createView("Filter4", query4.toString());
        } else {
            dao.createView("Filter4", "SELECT * FROM Filter3");
        }

        ArrayList<Listing> listings;
        System.out.print("Would you like to rank by price? (asc/desc/n): ");
        String str = scanner.next().trim().toUpperCase(Locale.ROOT);
        listings = dao.getListingsFromView("Filter4", str);

        for (String s: views) {
            dao.deleteView(s);
        }

        for (int j=0; j<listings.size(); j++) {
            System.out.println(j + ") " + listings.get(j));
        }

        return listings;
    }

    /* Displays all host's listings and allows host to select which they want to update */
    public static void viewHostListings() {
        try {
            boolean exit = false;
            while (!exit) {
                System.out.println("===== All active listings =====");
                ArrayList<Listing> hostListings = dao.getListingsFromHost(loggedInUser.getUid());

                // print out the listings
                for (int i = 0; i < hostListings.size(); i++) {
                    System.out.println(i + 1 + ": " + hostListings.get(i));
                }

                // get input for updating or viewing listings
                System.out.println("Options:");
                System.out.println("a. View availabilities for a listing");
                System.out.println("b. Update a listing");
                System.out.println("c. Exit");
                System.out.print("Select an option: ");
                String input = scanner.next();

                if (input.equals("a")) {
                    System.out.print("Enter listing number to view availabilities: ");
                    int listing = scanner.nextInt();
                    if (listing - 1 >= 0 && listing - 1 < hostListings.size()) {
                        viewAvailabilities(hostListings.get(listing - 1).getLid());
                    } else {
                        System.out.println("Invalid input.");
                    }
                } else if (input.equals("b")) {
                    System.out.print("Enter listing number to update: ");
                    int listing = scanner.nextInt();
                    if (listing - 1 >= 0 && listing - 1 < hostListings.size()) {
                        updateListing(hostListings.get(listing - 1).getLid());
                    } else {
                        System.out.println("Invalid input.");
                    }
                } else if (input.equals("c")) {
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

    /* Allows host to view availabilities for a listing within a date range. */
    public static void viewAvailabilities(int lid) {
        System.out.print("Enter start date (YYYY-MM-DD): ");
        String startDate = scanner.next();

        System.out.print("Enter end date (YYYY-MM-DD): ");
        String endDate = scanner.next();

        // display the availabilities
        try {
            ArrayList<Calendar> availabilities = dao.getAvailabilitiesInRange(lid, startDate, endDate);

            for (int i = 0; i < availabilities.size(); i++) {
                System.out.println(availabilities.get(i));
            }
            System.out.println("Total number of availabilities in this range: " + availabilities.size());
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue retrieving availabilities");
        }
    }

    /* Allows host to update listing with id lid */
    public static void updateListing(int lid) {
        //System.out.println("LID IS: " + lid);
        System.out.println("Possible operations: ");
        System.out.println("1. Add availability");
        System.out.println("2. Modify availability price");
        System.out.println("3. Remove availability");

        System.out.print("Select operation (-1 to exit): ");
        int input = scanner.nextInt();

        if (input == 1) {
            addAvailability(lid);
        } else if (input == 2) {
            modifyAvailability(lid);
        } else if (input == 3) {
            cancelAvailability(lid);
        }
    }

    public static void addAvailability(int lid) {
        System.out.print("Enter start date for availability (YYYY-MM-DD): ");
        String startDate = scanner.next();

        System.out.print("Enter end date (YYYY-MM-DD): ");
        String endDate = scanner.next();

        System.out.print("Enter price: ");
        double price = scanner.nextDouble();

        try {
            if (Calendar.checkAvailabilitiesInRange(dao, lid, startDate, endDate)) {
                System.out.println("Availabilities already exist within this range. New availabilities will " +
                        "be created around these old ones and old ones will remain unmodified.");
            }
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue accessing availabilities in database.");
        }

        try {
            int created = Calendar.createAvailability(dao, lid, startDate, endDate, price);
            System.out.println("Number of availabilities made available: " + created);
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue adding availabilities");
        }
    }

    /* Deals with inputs for modifying an availability's price */
    public static void modifyAvailability(int lid) {
        System.out.print("Enter start date for modification (YYYY-MM-DD): ");
        String startDate = scanner.next();

        System.out.print("Enter end date for modification (YYYY-MM-DD): ");
        String endDate = scanner.next();

        System.out.print("Enter new price: ");
        double price = scanner.nextDouble();

        // check if availability in range has been booked
        try {
            if (Calendar.checkBookedInRange(dao, lid, startDate, endDate)) {
                System.out.println("Price could not be changed as an availability within this " +
                        "date range has already been booked.");
                return;
            }
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue accessing database");
        }

        // if not, make the modification
        try {
            int modified = Calendar.updateAvailabilityInRange(dao, lid, startDate, endDate, price);
            System.out.println("Availabilities modified.");
            System.out.println("Total number of availabilities changed: " + modified);
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue accessing database");
        }
    }

    public static void cancelAvailability(int lid) {
        System.out.print("Enter start date to cancel (YYYY-MM-DD): ");
        String startDate = scanner.next();

        System.out.print("Enter end date for modification (YYYY-MM-DD): ");
        String endDate = scanner.next();

        // check if an availability in range has been booked
        try {
            if (Calendar.checkBookedInRange(dao, lid, startDate, endDate)) {
                System.out.println("An availability within this date range has already been booked.");
                return;
            }
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue accessing database");
        }

        // if not, cancel all availabilities in range
        try {
            int modified = Calendar.cancelAvailabilitiesInRange(dao, lid, startDate, endDate);
            System.out.println("Availabilities cancelled.");
            System.out.println("Total number of availabilities cancelled: " + modified);
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue accessing database");
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
        String city = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

        System.out.print("Country: ");
        String country = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

        System.out.print("Postal Code: ");
        String postalCode = scanner.next().trim().toLowerCase(Locale.ROOT);

        try {
            int lid = Listing.createListing(dao, loggedInUser.getUid(), type, latitude, longitude, address, city, country, postalCode);
            System.out.println("Listing Created Successfully!");

            addAmenities(lid, type, country, city, postalCode);
        } catch (IllegalArgumentException iae) {
            System.out.println("Invalid input. Please insure fields are non-empty or type matches types listed.");
        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("There was a problem adding that listing");
        }

    }

    public static void addAmenities(int lid, String type, String country, String city, String postalCode) {
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
                System.out.println("5. View Host Toolkit");

                int input = scanner.nextInt();
                scanner.nextLine(); // flush

                if (input == -1) {
                    break;
                }

                if (input == 5) {
                    hostToolkit(lid, type, country, city, postalCode);
                    continue;
                }

                // loop to add multiple amenities from category
                while (true) {
                    // print out amenities in that category
                    System.out.println("Available amenities to add in selected category: ");
                    System.out.println(categories[input-1]);

                    System.out.print("Enter amenity to add (q to quit): ");
                    String amenity = scanner.nextLine().toLowerCase(Locale.ROOT).trim();

                    if (amenity.equals("q")) {
                        break;
                    }

                    // add amenity to the listing
                    if (categories[input-1].contains(amenity)) {
                        categories[input-1].remove(amenity);
                        dao.offerAmenity(lid, amenity);
                        System.out.println("Amenity added.");
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

    public static double getAvgPriceOfListings(String type, List<Amenity> offered,
                                                String country, String city, String postalCode) throws SQLException {
        double price = dao.avgPriceOfListings(type, offered, country, city, postalCode);
        if (price <= 0) {
            price = dao.avgPriceOfListings(type, offered, country, city);
            if (price <= 0) {
                price = dao.avgPriceOfListings(type, offered, country);
            }
        }
        return price;
    }

    public static void recommendAmenities(List<Amenity> amenities, List<Amenity> offered, String type, String country,
                                        String city, String postalCode, double price) throws SQLException {
        int i = 0;
        while (i < amenities.size() && i < 2) {
            List<Amenity> temp = new ArrayList<>();
            temp.addAll(offered);
            temp.add(amenities.get(i));
            double newPrice = getAvgPriceOfListings(type, temp, country, city, postalCode);
            if (newPrice <= 0) {
                System.out.println(i + ") " + amenities.get(i).getDescription());
            } else {
                double increase = newPrice - price;
                System.out.println(i + ") " + amenities.get(i).getDescription() +
                        " with price increase of " + increase);
            }
            i++;
        }
    }

    public static void hostToolkit(int lid, String type, String country, String city, String postalCode) {
        try {
            // ----------- recommend price -------------
            String star = "*";
            System.out.println(star.repeat(50));
            List<Amenity> offered = dao.getAmenitiesListByLID(lid);
//            double price = dao.avgPriceOfListings(type, offered, country, city, postalCode);
//            if (price <= 0) {
//                price = dao.avgPriceOfListings(type, offered, country, city);
//                if (price <= 0) {
//                    price = dao.avgPriceOfListings(type, offered, country);
//                    if (price <= 0) {
//                        System.out.println("Not enough data to recommend price of listing");
//                        return;
//                    }
//                }
//            }
            double price = getAvgPriceOfListings(type, offered, country, city, postalCode);
            if (price <= 0) {
                System.out.println("Not enough data to recommend price of listing");
            } else {
                System.out.println("Recommended price: " + price);
            }
            // ----------- recommend amenities -------------
            List<Amenity> essentials = dao.getCommonEssentials(offered);
            List<Amenity> features = dao.getUncommonFeatures(offered);
            if (essentials.isEmpty()) {
                System.out.println("No essentials to recommend");
            } else {
                System.out.println("Consider adding the following essentials/safety: ");
                recommendAmenities(essentials, offered, type, country, city, postalCode, price);
            }
            if (features.isEmpty()) {
                System.out.println("No features to recommend");
            } else {
                System.out.println("Consider adding the following features: ");
                recommendAmenities(features, offered, type, country, city, postalCode, price);
            }
            System.out.println(star.repeat(50));
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Something went wrong trying to recommend listings");
        }
    }

    public static void createBooking(List<Listing> listings) throws SQLException {
        System.out.print("Select a listing you would like to book: ");
        int input = scanner.nextInt();
        if (input < 0 || input > listings.size()) {
            System.out.println("Invalid Listing");
            return;
        }

        int lid = listings.get(input).getLid();

        System.out.print("Would you like to see availabilities within a date range for this listing? (y/n): ");
        String in = scanner.next().trim();
        while (in.equalsIgnoreCase("y")) {
            viewAvailabilities(lid);
            System.out.print("Would you like to see more availabilities? (y/n): ");
            in = scanner.next().trim();
        }

        System.out.print("Enter date range to book YYYY-MM-DD YYYY-MM-DD: ");
        String startDate = scanner.next();
        String endDate = scanner.next();
        Booking booking = Booking.create(dao, loggedInUser.getUid(),
                lid, startDate, endDate);
        if (booking != null) {
            System.out.println("New booking was created for cost of " + booking.getCost());
        }
    }

    public static boolean handleDefaultInput(int choice) {
        boolean isLoggedIn = false;
        switch (choice) {
            case 1:
                if (!signup()) {
                    System.out.println("Sign up unsuccessful!");
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
                createListing();
                break;
            case 2:
                viewHostListings();
                break;
            case 3:
                try {
                    List<Booking> bookings = displayBookings("UPCOMING");
                    if (!bookings.isEmpty()) {
                        cancelBooking(bookings);
                    } else {
                        System.out.println("No bookings to display");
                    }
                } catch (Exception e) {
                    System.out.println("Something went wrong trying to retrieve bookings");
                }
                break;
            case 4:
                try {
                    List<User> renters = displayUsers();
                    if (!renters.isEmpty()) {
                        reviewUser(renters);
                    } else {
                        System.out.println("No renters to display");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 5:
                try {
                    dao.deleteHost(loggedInUser.getUid());
                    System.out.println("User deleted successfully");
                    isLoggedIn = false;
                    loggedInUser = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 6:
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
//        dao.updateBookingsStatus();
        List<Booking> bookings;
        if (loggedInUser.getClass().equals(Renter.class)) {
            bookings = dao.getRentersBookings(status, loggedInUser.getUid());
        } else {
            bookings = dao.getHostsBookings(status, loggedInUser.getUid());
        }
        for (int i=0; i<bookings.size(); i++) {
            System.out.println(i + ") " + bookings.get(i).display(dao));
        }
        return bookings;
    }

    public static void cancelBooking(List<Booking> bookings) throws SQLException {
        System.out.print("Select a booking you would like to cancel: ");
        int input = scanner.nextInt();
        if (input < 0 || input > bookings.size()) {
            System.out.println("Invalid Booking");
            return;
        }
        Booking booking = bookings.get(input);
        dao.updateCalendar(booking.getLid(), booking.getStartDate(),
                booking.getEndDate(), "AVAILABLE");
        dao.updateBooking(booking.getBid());
        System.out.println("Booking canceled successfully");
    }

    public static void reviewBooking(List<Booking> bookings) throws SQLException {
        System.out.print("Select a booking you would like to review: ");
        int input = scanner.nextInt();
        if (input < 0 || input > bookings.size()) {
            System.out.println("Invalid Booking");
            return;
        }
        Booking booking = bookings.get(input);
        if (booking.getReview() != null) {
            System.out.println("Selected booking has been reviewed");
            return;
        }
        scanner.nextLine();
        System.out.print("Review: ");
        String review = scanner.nextLine();
        System.out.print("Rating (0-5): ");
        int rating = scanner.nextInt();
        dao.reviewBooking(booking.getBid(), review, rating);
        System.out.println("Booking reviewed successfully");
    }

    public static List<User> displayUsers() throws SQLException {
        List<User> users;
        if (loggedInUser.getClass().equals(Renter.class)) {
            users = dao.getHostsOfRenter(loggedInUser.getUid());
        } else {
            users = dao.getRentersOfHost(loggedInUser.getUid());
        }
        for (int i=0; i<users.size(); i++) {
            System.out.println(i + ") " + users.get(i));
        }
        return users;
    }

    public static void reviewUser(List<User> users) throws SQLException {
        if (loggedInUser.getClass().equals(Renter.class)) {
            System.out.print("Select a host you would like to review: ");
        } else {
            System.out.print("Select a renter you would like to review: ");
        }
        int input = scanner.nextInt();
        if (input < 0 || input > users.size()) {
            System.out.println("Invalid User");
            return;
        }
        scanner.nextLine();
        System.out.print("Review: ");
        String review = scanner.nextLine();
        System.out.print("Rating (0-5): ");
        int rating = scanner.nextInt();
        dao.reviewUser(loggedInUser.getUid(), users.get(input).getUid(), review, rating);
        System.out.println("Review added successfully");
    }

    public static boolean handleRenterInput(int choice) {
        boolean isLoggedIn = true;
        try {
            switch (choice) {
                case 1:
                    List<Listing> listings = displayListings();
                    if (!listings.isEmpty()) {
                        createBooking(listings);
                    } else {
                        System.out.println("No listings to display");
                    }
                    break;
                case 2:
                    List<Booking> bookings = displayBookings("UPCOMING");
                    if (!bookings.isEmpty()) {
                        cancelBooking(bookings);
                    } else {
                        System.out.println("No bookings to display");
                    }
                    break;
                case 3:
                    List<Booking> pastBookings = displayBookings("PAST");
                    if (!pastBookings.isEmpty()) {
                        reviewBooking(pastBookings);
                    } else {
                        System.out.println("No bookings to display");
                    }
                    break;
                case 4:
                    List<User> hosts = displayUsers();
                    if (!hosts.isEmpty()) {
                        reviewUser(hosts);
                    } else {
                        System.out.println("No hosts to display");
                    }
                    break;
                case 5:
                    dao.deleteRenter(loggedInUser.getUid());
                    System.out.println("User deleted successfully");
                    isLoggedIn = false;
                    loggedInUser = null;
                    break;
                case 6:
                    System.out.println("Thank you for using MyBnB!");
                    isLoggedIn = false;
                    break;
                default:
                    System.out.println("Invalid Choice");
                    break;
            }
        } catch (Exception e) {
            System.out.println("Something went wrong");
            e.printStackTrace();
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
                    if (choice == 4) {
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
