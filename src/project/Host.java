package project;

import java.sql.SQLException;
import java.util.Locale;

public class Host extends User {
    public static boolean signup(DAO dao, String name, String email, String password, String sin, String dob, String occupation,
                              String address, String city, String country, String postalCode)
            throws IllegalArgumentException, SQLException {
        User.validateParameters(name, email, password, sin, dob, occupation, address, city, country, postalCode);

        int aid = dao.createAddress(address.toLowerCase(Locale.ROOT).trim(), city.toLowerCase(Locale.ROOT).trim(),
                country.toLowerCase(Locale.ROOT).trim(), postalCode.toLowerCase(Locale.ROOT).trim());
        if (dao.getUserOnEmail(email.toLowerCase(Locale.ROOT).trim()) != -1) {
           return false;
        }
        int uid = dao.createUser(sin.toLowerCase(Locale.ROOT).trim(), name.trim(), dob.trim(),
                occupation.toLowerCase().trim(), email.toLowerCase(Locale.ROOT).trim(), password, aid);
        dao.createHost(uid);
        return true;
    }
}
