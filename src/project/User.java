package project;

import java.util.Locale;

public abstract class User {
    private int uid;
    private String sin;
    private String email;
    private String occupation;
    private String password;
    private String dob;
    private String name;
    private Address address;

    public static void validateParameters(String name, String email, String password, String sin, String dob,
                String occupation, String address, String city, String country, String postalCode)
                    throws IllegalArgumentException {
        if (name == "" || email == "" || password == "" || sin == "" || dob == "" || occupation == ""
            || address == "" || city == "" || country == "" || postalCode == "") {
            throw new IllegalArgumentException();
        }
//        this.name = name;
//        this.email = email.toLowerCase(Locale.ROOT).trim();
//        this.password = password;
//        this.sin = sin.toUpperCase(Locale.ROOT);
//        this.dob = dob;
//        this.occupation = occupation.toLowerCase(Locale.ROOT);
//        this.address = address.toLowerCase(Locale.ROOT).trim();

    }

}
