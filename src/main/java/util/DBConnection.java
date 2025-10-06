package util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;
public class DBConnection {
    private static final Properties props = new Properties();
    static {
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            props.load(in);
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Unable to load DB properties", e);
        }
    }
    public static Connection getConnection() throws Exception {
        String url = props.getProperty("jdbc.url");
        String user = props.getProperty("jdbc.user");
        String pass = props.getProperty("jdbc.password");
        return DriverManager.getConnection(url, user, pass);
    }
}
