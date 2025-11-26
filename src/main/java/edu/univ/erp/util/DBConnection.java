package edu.univ.erp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBConnection {
    // Auth DB (default)
    private static final String URL = "jdbc:mysql://localhost:3306/auth_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "Anuradha@babu1";

    // --- existing method ---
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // --- ERP DB connection (used for main student/instructor/admin data) ---
    public static Connection getErpConnection() throws SQLException {
        // just reuse same credentials but switch DB name in URL
        String erpUrl = "jdbc:mysql://localhost:3306/erp_db?useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(erpUrl, USER, PASSWORD);
    }

    // --- optional helper (auth-specific clarity) ---
    public static Connection getAuthConnection() throws SQLException {
        // Same as default URL
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

public static boolean isMaintenanceMode() {
    String sql = "SELECT `value` FROM settings WHERE `key` = 'maintenance_on' LIMIT 1";
    try (Connection conn = getErpConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        String dbUrl = "unknown";
        try { dbUrl = conn.getMetaData().getURL(); } catch (Exception ignore) {}

        if (rs.next()) {
            String v = rs.getString(1);
            if (v == null) v = "null";
            String norm = v.trim().toLowerCase();
            boolean result = norm.equals("true") || norm.equals("1") || norm.equals("yes") || norm.equals("on");

            // DEBUG: prints what we read (remove when everything is working)
            System.out.println("[DBDEBUG] isMaintenanceMode() -> raw='" + v + "' norm='" + norm +
                               "' result=" + result + " dbUrl=" + dbUrl + " time=" + new java.util.Date());

            return result;
        } else {
            System.out.println("[DBDEBUG] isMaintenanceMode() -> no row found in settings (dbUrl=" + dbUrl + ")");
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return false;
}


}
