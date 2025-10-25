package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;


import edu.univ.erp.util.DBConnection;

public class AuthService {

    private static final int MAX_FAILED = 5;
    private static final int LOCK_MINUTES = 15;

    /**
     * Authenticate user by username (your DB does not have 'email' column).
     * Returns role_id on success, -1 on failure.
     * 
     */

     // In AuthService.java
   public static String getStudentId(String rollNo) {
        try (Connection conn = DBConnection.getErpConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT student_id FROM students WHERE roll_no = ? LIMIT 1"
            );
            ps.setString(1, rollNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return String.valueOf(rs.getLong("student_id"));
            }
        } catch (Exception e) {
            System.err.println("getStudentId error: " + e.getMessage());
        }
        return null;
    }

    public static Long getStudentIdByRoll(String rollNo) {
    try (Connection conn = DBConnection.getErpConnection();
         PreparedStatement ps = conn.prepareStatement("SELECT student_id FROM students WHERE roll_no = ?")) {
        ps.setString(1, rollNo);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong("student_id");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null; // not found
}
// AuthService.java (new helper)
public static Long getStudentIdForUsername(String username) {
    try (Connection conn = DBConnection.getErpConnection()) {
        String sql = "SELECT student_id FROM students WHERE roll_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("student_id");
            }
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return null;
}


    
    public static int authenticateByRole(String username, String password, int requiredRole) {
        // Note: query uses username only because your users table doesn't have an email column
        String selectSql = "SELECT user_id, pass_hash, role_id, status, failed_attempts, locked_until FROM users WHERE username = ? LIMIT 1";
        String updateSuccessSql = "UPDATE users SET failed_attempts = 0, locked_until = NULL, last_login = NOW() WHERE user_id = ?";
        String updateFailSql = "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE user_id = ?";

        

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {

            ps.setString(1, username); // only username

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return -1; // no such user

                long userId = rs.getLong("user_id");
                String hash = rs.getString("pass_hash");
                int roleId = rs.getInt("role_id");
                String status = rs.getString("status");
                int failed = rs.getInt("failed_attempts");
                Timestamp lockedUntil = rs.getTimestamp("locked_until");

                // check status
                if (!"ACTIVE".equalsIgnoreCase(status)) return -1;

                // check locked_until
                if (lockedUntil != null && lockedUntil.after(new Timestamp(System.currentTimeMillis()))) {
                    return -1; // account locked
                }

                // verify password
                if (BCrypt.checkpw(password, hash)) {
                    // verify requested role matches stored role
                    if (roleId != requiredRole) {
                        return -1;
                    }
                    // success -> reset counters, update last_login
                    try (PreparedStatement psSucc = conn.prepareStatement(updateSuccessSql)) {
                        psSucc.setLong(1, userId);
                        psSucc.executeUpdate();
                    }
                    return roleId;
                } else {
                    // incorrect password -> increment failed attempts and maybe lock
                    failed += 1;
                    Timestamp newLockedUntil = null;
                    if (failed >= MAX_FAILED) {
                        long lockMillis = System.currentTimeMillis() + LOCK_MINUTES * 60L * 1000L;
                        newLockedUntil = new Timestamp(lockMillis);
                    }
                    try (PreparedStatement psFail = conn.prepareStatement(updateFailSql)) {
                        psFail.setInt(1, failed);
                        if (newLockedUntil != null) psFail.setTimestamp(2, newLockedUntil);
                        else psFail.setNull(2, Types.TIMESTAMP);
                        psFail.setLong(3, userId);
                        psFail.executeUpdate();
                    }
                    return -1;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }
    
}

