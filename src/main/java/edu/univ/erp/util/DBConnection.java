package edu.univ.erp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // ✅ Auth DB (default)
    private static final String URL = "jdbc:mysql://localhost:3306/auth_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "Anuradha@babu1";

    // --- existing method ---
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // --- ERP DB connection (used for main student/instructor/admin data) ---
    public static Connection getErpConnection() throws SQLException {
        // ✅ just reuse same credentials but switch DB name in URL
        String erpUrl = "jdbc:mysql://localhost:3306/erp_db?useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(erpUrl, USER, PASSWORD);
    }

    // --- optional helper (auth-specific clarity) ---
    public static Connection getAuthConnection() throws SQLException {
        // Same as default URL
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
