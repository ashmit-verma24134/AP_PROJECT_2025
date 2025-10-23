import java.sql.*;


public class DBConnec {
    public static Connection main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/database_name"; // Database details
        String username = "rootgfg"; // MySQL credentials
        String password = "gfg123";   //idk what to write...correct this 
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Establish connection
        Connection con = DriverManager.getConnection(url, username, password);
        System.out.println("Connection Established successfully");
        return con;
    }
}


