package project;

import java.sql.SQLException;
import java.util.Locale;

public class Renter extends User {
    private String creditCard;
    public Renter(int uid, String sin, String email, String occupation, String password,
                String dob, String name, int aid, String status, String creditCard) {
        super(uid, sin, email, occupation, password, dob, name, aid, status);
        this.creditCard = creditCard;
    }

    public Renter(User user, String creditCard) {
        super(user);
        this.creditCard = creditCard;
    }

    public static boolean signup(DAO dao, String name, String email, String password, String sin, String dob, String occupation,
                  String address, String city, String country, String postalCode, String creditCard)
            throws IllegalArgumentException, SQLException {
        User.validateParameters(name, email, password, sin, dob, occupation, address, city, country, postalCode);
        if (creditCard.isEmpty()) {
            throw new IllegalArgumentException();
        } else {
            if (!dao.isLegalAge(dob)) {
                System.out.println("Must be at least 18 years old to sign up");
                return false;
            }
            int aid = dao.createAddress(address.toLowerCase(Locale.ROOT).trim(), city.toLowerCase(Locale.ROOT).trim(),
                    country.toLowerCase(Locale.ROOT).trim(), postalCode.toLowerCase(Locale.ROOT).trim());

            int uid = dao.createUser(sin.toLowerCase(Locale.ROOT).trim(), name.trim(), dob.trim(),
                    occupation.toLowerCase().trim(), email.toLowerCase(Locale.ROOT).trim(), password, aid);
            dao.createRenter(uid, creditCard.toLowerCase(Locale.ROOT).trim());
        }
        System.out.println("Sign up successful!");
        return true;
    }

    public static User login(DAO dao, String email, String password) throws SQLException {
        User user = dao.getUserOnEmail(email);
        if (user == null) {
            System.out.println("Email is not registered");
            return null;
        }
        Renter renter = dao.getRenterFromUser(user);
        if (renter == null) {
            System.out.println("Email is not registered as renter");
            return null;
        }
        if (!renter.getPassword().equals(password)) {
            System.out.println("Incorrect password entered");
            return null;
        }
        if (renter.getStatus().equals("INACTIVE")) {
            System.out.println("Account had been deleted");
            return null;
        }
        return renter;
    }

    public static boolean exists(DAO dao, String email, String password, String creditCard) throws SQLException {
        if (creditCard.isEmpty() || email.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException();
        }
        User user = dao.getUserOnEmail(email.toLowerCase(Locale.ROOT).trim());
        if (user != null) {
            Renter renter = dao.getRenterFromUser(user);
            if (user.getStatus().equals("INACTIVE")) {
                // user whole account was deleted is signing up again
                if (!user.getPassword().equals(password)) {
                    System.out.println("Incorrect password");
                    return true;
                }
                dao.updateUserStatus(user.getUid(), "ACTIVE");
                System.out.println("Your account has been reactivated");
                if (renter == null) {
                    dao.createRenter(user.getUid(), creditCard.toLowerCase(Locale.ROOT).trim());
                    System.out.println("Your account has been added as a renter");
                }
                return true;
            }
            if (renter == null) {
                // user is an active host, signup as renter
                if (!user.getPassword().equals(password)) {
                    System.out.println("Incorrect password");
                    return true;
                }
                dao.createRenter(user.getUid(), creditCard.toLowerCase(Locale.ROOT).trim());
                System.out.println("Your account has been added as a renter");
                return true;
            }
            System.out.println("Email already registered as a renter");
            return true;
        }
        return false;
    }
}
