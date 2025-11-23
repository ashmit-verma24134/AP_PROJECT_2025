package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

/**
 * AuthService - authentication helpers for your schema (auth_db.users).
 *
 * - authenticateByRole(...) : authenticates and enforces lockout policy
 * - changePassword(...) : change password (validates old password, updates pass_hash)
 * - getStudentIdByRoll / getStudentIdByUsername : student lookup helpers
 * - getInstructorIdByUsername : instructor lookup helper
 * - getLockInfo : returns a helpful locked/failed message for UI
 *
 * NOTE: Uses DBConnection.getAuthConnection() for auth_db.users and DBConnection.getErpConnection()
 * for ERP tables (instructors, students).
 */
public class AuthService {

    private static final int MAX_FAILED = 5;       // attempts before lock
    private static final int LOCK_MINUTES = 15;    // lock duration in minutes

    /**
     * Authenticate user and ensure role matches requiredRole.
     * Returns role_id on success (1/2/3), -1 on failure.
     */
    public static int authenticateByRole(String username, String password, int requiredRole) {
        final String SELECT_SQL =
                "SELECT user_id, username, pass_hash, role_id, status, failed_attempts, locked_until " +
                        "FROM users WHERE username = ? LIMIT 1";
        final String RESET_SUCCESS_SQL =
                "UPDATE users SET failed_attempts = 0, locked_until = NULL, last_login = NOW() WHERE user_id = ?";
        final String UPDATE_FAIL_SQL =
                "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_SQL)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return -1; // not found

                long userId = rs.getLong("user_id");
                String storedHash = rs.getString("pass_hash");
                int dbRole = rs.getInt("role_id");
                String status = rs.getString("status");
                int failed = rs.getInt("failed_attempts");
                Timestamp lockedUntil = rs.getTimestamp("locked_until");

                // status check
                if (!"ACTIVE".equalsIgnoreCase(status)) return -1;

                // lock check
                if (lockedUntil != null && lockedUntil.after(new Timestamp(System.currentTimeMillis()))) {
                    return -1;
                }

                boolean ok;
                try {
                    ok = BCrypt.checkpw(password, storedHash);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return -1;
                }

                if (ok) {
                    // role check
                    if (dbRole != requiredRole) {
                        return -1;
                    }
                    // success: reset failed attempts & update last_login
                    try (PreparedStatement ps2 = conn.prepareStatement(RESET_SUCCESS_SQL)) {
                        ps2.setLong(1, userId);
                        ps2.executeUpdate();
                    }
                    return dbRole;
                } else {
                    // increment failures and possibly lock
                    failed += 1;
                    Timestamp newLockedUntil = null;
                    if (failed >= MAX_FAILED) {
                        long unlockMs = System.currentTimeMillis() + LOCK_MINUTES * 60L * 1000L;
                        newLockedUntil = new Timestamp(unlockMs);
                    }
                    try (PreparedStatement ps3 = conn.prepareStatement(UPDATE_FAIL_SQL)) {
                        ps3.setInt(1, failed);
                        if (newLockedUntil != null) ps3.setTimestamp(2, newLockedUntil);
                        else ps3.setNull(2, Types.TIMESTAMP);
                        ps3.setLong(3, userId);
                        ps3.executeUpdate();
                    }
                    return -1;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    /**
     * Change password for username: validate old password and update pass_hash.
     * Returns true on success, false on failure (invalid old password or DB error).
     */
    public static boolean changePassword(String username, String oldPassword, String newPassword) {
        final String SELECT_SQL = "SELECT user_id, pass_hash FROM users WHERE username = ? LIMIT 1";
        final String UPDATE_SQL = "UPDATE users SET pass_hash = ?, updated_at = NOW() WHERE user_id = ?";

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_SQL)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                long userId = rs.getLong("user_id");
                String storedHash = rs.getString("pass_hash");
                if (!BCrypt.checkpw(oldPassword, storedHash)) return false;

                String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
                try (PreparedStatement pu = conn.prepareStatement(UPDATE_SQL)) {
                    pu.setString(1, newHash);
                    pu.setLong(2, userId);
                    pu.executeUpdate();
                }
                return true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Helpful lock info for UI after a failed login attempt (or when login returns -1).
     * Returns string that can be displayed or null if no special info.
     */
    public static String getLockInfo(String username) {
        final String SQL = "SELECT failed_attempts, locked_until FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int failed = rs.getInt("failed_attempts");
                Timestamp locked = rs.getTimestamp("locked_until");
                if (locked != null && locked.after(new Timestamp(System.currentTimeMillis()))) {
                    long minsLeft = (locked.getTime() - System.currentTimeMillis()) / 60000L;
                    return "Account locked. Try again in " + Math.max(1, minsLeft) + " minute(s).";
                }
                if (failed > 0) {
                    return "Failed attempts: " + failed + "/" + MAX_FAILED + ".";
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return null;
    }

    /* ----------------- Student & Instructor lookups ----------------- */

    public static Long getStudentIdByRoll(String rollNo) {
        final String SQL = "SELECT student_id FROM students WHERE roll_no = ? LIMIT 1";
        try (Connection conn = DBConnection.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setString(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("student_id");
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return null;
    }

    public static String getStudentIdByUsername(String username) {
        Long id = getStudentIdByRoll(username);
        return (id == null) ? null : String.valueOf(id);
    }

    /**
     * Resolve instructor_id for a login username.
     * Tries multiple strategies:
     *  - instructors.username
     *  - users.email -> users.id -> instructors.instructor_id
     *  - instructors.full_name LIKE username
     * Returns null if not found.
     */
    public static Long getInstructorIdByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        try (Connection conn = DBConnection.getErpConnection()) {
            // 1) instructors.username
            try (PreparedStatement p1 = conn.prepareStatement("SELECT instructor_id FROM instructors WHERE username = ? LIMIT 1")) {
                p1.setString(1, username);
                try (ResultSet r = p1.executeQuery()) {
                    if (r.next()) return r.getLong("instructor_id");
                }
            } catch (SQLException ignored) {}

            // 2) users.email -> users.id -> instructors.instructor_id
            try (PreparedStatement p2 = conn.prepareStatement("SELECT id FROM users WHERE email = ? LIMIT 1")) {
                p2.setString(1, username);
                try (ResultSet r2 = p2.executeQuery()) {
                    if (r2.next()) {
                        long uid = r2.getLong("id");
                        try (PreparedStatement p3 = conn.prepareStatement("SELECT instructor_id FROM instructors WHERE instructor_id = ? LIMIT 1")) {
                            p3.setLong(1, uid);
                            try (ResultSet r3 = p3.executeQuery()) {
                                if (r3.next()) return r3.getLong("instructor_id");
                            }
                        }
                    }
                }
            } catch (SQLException ignored) {}

            // 3) full_name LIKE
            try (PreparedStatement p4 = conn.prepareStatement("SELECT instructor_id FROM instructors WHERE full_name LIKE ? LIMIT 1")) {
                p4.setString(1, "%" + username + "%");
                try (ResultSet r4 = p4.executeQuery()) {
                    if (r4.next()) return r4.getLong("instructor_id");
                }
            } catch (SQLException ignored) {}

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
