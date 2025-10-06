package test;
import util.DBConnection;
import java.sql.*;
public class DBTest {
  public static void main(String[] args) throws Exception {
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM auth_db.roles");
         ResultSet rs = ps.executeQuery()) {
      if (rs.next()) System.out.println("roles count: " + rs.getInt(1));
    }
    System.out.println("DB connection OK");
  }
}
