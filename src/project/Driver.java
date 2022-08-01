package project;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Scanner;

public class Driver {

    public static final String dbName = "C43Project";
    public static final String user = "root";
    public static final String password = "";

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

        scanner.nextLine();
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
                return true;
            } else if (choice == 2) {
                loggedInUser = Host.login(dao, email, password);
                return true;
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
//                displayAllListings()
//                filteringListings()
                System.out.println("Select a listing you would like to book: ");
//                handleBooking()
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
