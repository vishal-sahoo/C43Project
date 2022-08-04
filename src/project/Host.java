package project;

import java.sql.SQLException;
import java.util.Locale;

public class Host extends User {

    public Host(int uid, String sin, String email, String occupation, String password,
                  String dob, String name, int aid, String status) {
        super(uid, sin, email, occupation, password, dob, name, aid, status);
    }

    public Host(User user) {
        super(user);
    }

    public static boolean signup(DAO dao, String name, String email, String password, String sin, String dob, String occupation,
                              String address, String city, String country, String postalCode)
            throws IllegalArgumentException, SQLException {
        User.validateParameters(name, email, password, sin, dob, occupation, address, city, country, postalCode);

        int aid = dao.createAddress(address.toLowerCase(Locale.ROOT).trim(), city.toLowerCase(Locale.ROOT).trim(),
                country.toLowerCase(Locale.ROOT).trim(), postalCode.toLowerCase(Locale.ROOT).trim());
        if (dao.getUserOnEmail(email.toLowerCase(Locale.ROOT).trim()) != null) {
           return false;
        }
        int uid = dao.createUser(sin.toLowerCase(Locale.ROOT).trim(), name.trim(), dob.trim(),
                occupation.toLowerCase().trim(), email.toLowerCase(Locale.ROOT).trim(), password, aid);
        dao.createHost(uid);
        return true;
    }

    public static User login(DAO dao, String email, String password) throws SQLException {
        User user = dao.getUserOnEmail(email);
        if (user != null) {
            Host host = dao.getHostFromUser(user);
            if (host != null && host.getPassword().equals(password) &&
                    host.getStatus().equals("ACTIVE")) {
                return host;
            }
        }
        return null;
    }
}
