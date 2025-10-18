package edu.univ.erp.util;

import edu.univ.erp.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class SeedUsers {
    public static void main(String[] args) {
        // change passwords here for seed users
        seedUser("admin1", "adminpass", "ADMIN");
        seedUser("inst1", "instrpass", "INSTRUCTOR");
        seedUser("stu1", "studpass", "STUDENT");
    }

    private static void seedUser(String username, String plainPassword, String roleName) {
        String findRole = "SELECT role_id FROM roles WHERE role_name = ?";
        String insertSql = "INSERT INTO users (username, pass_hash, role_id, status) VALUES (?, ?, ?, 'ACTIVE') ON DUPLICATE KEY UPDATE pass_hash = VALUES(pass_hash), role_id = VALUES(role_id), status = VALUES(status)";

        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement rsRole = conn.prepareStatement(findRole)) {

            rsRole.setString(1, roleName);
            var rs = rsRole.executeQuery();
            int roleId;
            if (rs.next()) roleId = rs.getInt(1);
            else {
                System.err.println("Role not found: " + roleName);
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setInt(3, roleId);
                int updated = ps.executeUpdate();
                System.out.println("Seeded user " + username + " (role " + roleName + "), rows=" + updated);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
