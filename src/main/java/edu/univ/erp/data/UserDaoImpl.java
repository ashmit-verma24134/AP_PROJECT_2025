package edu.univ.erp.data;

import edu.univ.erp.util.DBConnection;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple UserDaoImpl for the `users` table:
 * columns: id, email, password_hash, role, first_name, last_name, phone, active, created_at, updated_at
 */
public class UserDaoImpl implements UserDao {

    private final Connection conn;

    public UserDaoImpl(Connection conn) {
        this.conn = conn;
    }

    // helper: SHA-256 hashing (hex) for password -- simple demonstration only
    private static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Override
    public long createUser(String username, String plainPassword, String role,
                           String firstName, String lastName, String email) throws Exception {

        // Use email field as canonical 'username' since table has 'email'
        final String sql = "INSERT INTO users (email, password_hash, role, first_name, last_name, created_at, active) " +
                           "VALUES (?, ?, ?, ?, ?, NOW(), 1)";

        String passwordHash = plainPassword == null ? null : sha256Hex(plainPassword);

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email != null ? email : username);
            if (passwordHash != null) ps.setString(2, passwordHash);
            else ps.setNull(2, java.sql.Types.VARCHAR);
            ps.setString(3, role != null ? role : "STUDENT");
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    @Override
    public List<User> listUsers() throws SQLException {
        List<User> out = new ArrayList<>();
        final String sql = "SELECT id, email, role, first_name, last_name, phone, active, created_at, updated_at FROM users ORDER BY id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                u.setFirstName(rs.getString("first_name"));
                u.setLastName(rs.getString("last_name"));
                u.setPhone(rs.getString("phone"));
                u.setActive(rs.getInt("active") == 1);
                u.setCreatedAt(rs.getTimestamp("created_at"));
                u.setUpdatedAt(rs.getTimestamp("updated_at"));
                out.add(u);
            }
        }
        return out;
    }

    @Override
    public boolean deleteUser(long userId) throws SQLException {
        final String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean usernameExists(String username) throws SQLException {
        // our table stores email as username, so check email
        final String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
