package project;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.sql.SQLException;
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

        System.out.print("Name: ");
        String name = scanner.next();

        System.out.print("Email: ");
        String email = scanner.next();

        System.out.print("Password: ");
        String password = scanner.next();

        System.out.print("SIN: ");
        String sin = scanner.next();

        System.out.print("DOB (YYYY-MM-DD): ");
        String dob = scanner.next();

        System.out.print("Occupation: ");
        String occupation = scanner.next();

        scanner.nextLine();
        System.out.print("Address: ");
        String address = scanner.nextLine();

        System.out.print("City: ");
        String city = scanner.next();

        System.out.print("Country: ");
        String country = scanner.next();

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
//                login();
                boolean loginSuccessful = false;
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
                    displayRenterMenu();

                    displayHostMenu();

                    System.out.print("Enter Input: ");
                    isLoggedIn = handleHostInput(scanner.nextInt());

                    isLoggedIn = handleRenterInput(scanner.nextInt());

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
