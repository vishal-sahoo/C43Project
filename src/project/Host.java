package project;

import java.sql.SQLException;
import java.util.Locale;

public class Host extends User {

    public Host(User user) {
        super(user);
    }

    public static boolean signup(DAO dao, String name, String email, String password, String sin, String dob, String occupation,
                              String address, String city, String country, String postalCode)
            throws IllegalArgumentException, SQLException {
        User.validateParameters(name, email, password, sin, dob, occupation, address, city, country, postalCode);
        if (!dao.isLegalAge(dob)) {
            System.out.println("Must be at least 18 years old to sign up");
            return false;
        }
        int aid = dao.createAddress(address.toLowerCase(Locale.ROOT).trim(), city.toLowerCase(Locale.ROOT).trim(),
                country.toLowerCase(Locale.ROOT).trim(), postalCode.toLowerCase(Locale.ROOT).trim());

        int uid = dao.createUser(sin.toLowerCase(Locale.ROOT).trim(), name.trim(), dob.trim(),
                occupation.toLowerCase().trim(), email.toLowerCase(Locale.ROOT).trim(), password, aid);
        dao.createHost(uid);
        System.out.println("Sign up successful!");
        return true;
    }

    public static User login(DAO dao, String email, String password) throws SQLException {
        User user = dao.getUserOnEmail(email);
        if (user == null) {
            System.out.println("Email is not registered");
            return null;
        }
        Host host = dao.getHostFromUser(user);
        if (host == null) {
            System.out.println("Email is not registered as host");
            return null;
        }
        if (!host.getPassword().equals(password)) {
            System.out.println("Incorrect password entered");
            return null;
        }
        if (host.getStatus().equals("INACTIVE")) {
            System.out.println("Account had been deleted");
            return null;
        }
        return host;
    }

    public static boolean exists(DAO dao, String email, String password) throws SQLException {
        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException();
        }
        User user = dao.getUserOnEmail(email.toLowerCase(Locale.ROOT).trim());
        if ( user != null) {
            Host host = dao.getHostFromUser(user);
            if (user.getStatus().equals("INACTIVE")) {
                // user whole account was deleted is signing up again
                if (!user.getPassword().equals(password)) {
                    System.out.println("Incorrect password");
                    return true;
                }
                dao.updateUserStatus(user.getUid(), "ACTIVE");
                System.out.println("Your account has been reactivated");
                if (host == null) {
                    dao.createHost(user.getUid());
                    System.out.println("Your account has been added as a host");
                }
                return true;
            }
            if (host == null) {
                // user is an active renter, signup as host
                if (!user.getPassword().equals(password)) {
                    System.out.println("Incorrect password");
                    return true;
                }
                dao.createHost(user.getUid());
                System.out.println("Your account has been added as a host");
                return true;
            }
            System.out.println("Email already registered as a host");
            return true;
        }
        return false;
    }
}
