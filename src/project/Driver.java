package project;

import java.sql.*;

public class Driver {
    public static final String dbName = "";
    public static final String user = "root";
    public static final String password = "";

    public static void main(String [] args) {
        try {
            String url = "jdbc:mysql://localhost/" + dbName;
            Connection conn = DriverManager.getConnection(url, user, password);

            /* Execute a query and iterate through the resulting  tuples. */
//            PreparedStatement execStat = conn.prepareStatement(
//                    "SELECT * FROM student");
//            ResultSet rs = execStat.executeQuery();
            /* Extract data from result set*/
//            while (rs.next()) {
//                int sid = rs.getInt("sid");
//                String sname = rs.getString("firstName");
//                String rating = rs.getString("campus");
//                String age = rs.getString("email");
//                System.out.println(sname + ", " + rating + ", " + age);
//                /* Continued ... */
//            }

//            rs.close();
//            execStat.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
