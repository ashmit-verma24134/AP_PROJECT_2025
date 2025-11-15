package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;

import java.sql.*;

public class AdminService {

    public long createAuthUser(String username, String hashedPassword, String role) throws Exception {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getAuthConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            ps.setString(3, role);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
                throw new Exception("Failed to insert auth user");
            }
        }
    }

    public void createStudentProfile(long userId, String roll, String name, String email) throws Exception {
        String sql = "INSERT INTO students (user_id, roll_number, name, email) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, roll);
            ps.setString(3, name);
            ps.setString(4, email);
            ps.executeUpdate();
        }
    }

    public void createInstructorProfile(long userId, String name, String email) throws Exception {
        String sql = "INSERT INTO instructors (user_id, name, email) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.executeUpdate();
        }
    }

    public void createAdminProfile(long userId, String name, String email) throws Exception {
        String sql = "INSERT INTO admins (user_id, name, email) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.executeUpdate();
        }
    }
}
