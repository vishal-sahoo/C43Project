package project;

public class User {
    private int uid;
    private String sin;
    private String email;
    private String occupation;
    private String password;
    private String dob;
    private String name;
    private int aid;

    private String status;

    public User(int uid, String sin, String email, String occupation, String password,
                String dob, String name, int aid, String status) {
        this.uid = uid;
        this.sin = sin;
        this.email = email;
        this.occupation = occupation;
        this.password = password;
        this.dob = dob;
        this.name = name;
        this.aid = aid;
        this.status = status;
    }

    public User(User user) {
        this.uid = user.uid;
        this.sin = user.sin;
        this.email = user.email;
        this.occupation = user.occupation;
        this.password = user.password;
        this.dob = user.dob;
        this.name = user.name;
        this.aid = user.aid;
        this.status = user.status;
    }

    public static void validateParameters(String name, String email, String password, String sin, String dob,
                                          String occupation, String address, String city, String country, String postalCode)
                    throws IllegalArgumentException {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || sin.isEmpty() || dob.isEmpty() || occupation.isEmpty()
            || address.isEmpty() || city.isEmpty() || country.isEmpty() || postalCode.isEmpty()) {
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

    public int getUid() {
        return uid;
    }
    public String getPassword() {return password;}

    public String getStatus() { return status; }
    public String toString() {
        return name + " is a " + occupation;
    }
}
