package project;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.sql.SQLException;
import java.util.Locale;

public class Renter extends User {
    private String creditCard;

    public static boolean signup(DAO dao, String name, String email, String password, String sin, String dob, String occupation,
                  String address, String city, String country, String postalCode, String creditCard)
            throws IllegalArgumentException, SQLException {
        User.validateParameters(name, email, password, sin, dob, occupation, address, city, country, postalCode);
        if (creditCard == "") {
            throw new IllegalArgumentException();
        } else {
            int aid = dao.createAddress(address.toLowerCase(Locale.ROOT).trim(), city.toLowerCase(Locale.ROOT).trim(),
                    country.toLowerCase(Locale.ROOT).trim(), postalCode.toLowerCase(Locale.ROOT).trim());
            if (dao.getUserOnEmail(email.toLowerCase(Locale.ROOT).trim()) != -1) {
                return false;
            }
            int uid = dao.createUser(sin.toLowerCase(Locale.ROOT).trim(), name.trim(), dob.trim(),
                    occupation.toLowerCase().trim(), email.toLowerCase(Locale.ROOT).trim(), password, aid);
            dao.createRenter(uid, creditCard.toLowerCase(Locale.ROOT).trim());
        }
        return true;
    }
}
