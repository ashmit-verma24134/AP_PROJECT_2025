package edu.univ.erp.service;

import edu.univ.erp.data.UserDao;
import edu.univ.erp.data.User;
import edu.univ.erp.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.List;

public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public List<User> listAuthUsers() throws Exception {
        return userDao.listUsers();
    }

    @Override
    public boolean usernameExists(String username) throws Exception {
        return userDao.usernameExists(username);
    }

    @Override
    public long createAuthUser(String username, String plainPassword, String role,
                               String firstName, String lastName, String email,
                               String phone) throws Exception {

        // ----------------------------
        // 1) CREATE AUTH USER (same as before)
        // ----------------------------
        long authUserId = userDao.createUser(
                username,
                plainPassword,
                role,
                firstName,
                lastName,
                email
        );

        if (authUserId <= 0) {
            throw new Exception("Failed to insert user into auth_db.users");
        }

        // ----------------------------
        // 2) INSERT INTO instructors (if role=INSTRUCTOR)
        // instructors:
        //   instructor_id BIGINT PK (must equal user_id)
        //   full_name VARCHAR(200) NOT NULL
        //   department VARCHAR(100)
        //   created_at, updated_at auto
        //   username optional
        // ----------------------------
        if ("INSTRUCTOR".equalsIgnoreCase(role)) {

            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) fullName = username;

            try (Connection erp = DBConnection.getErpConnection()) {

                // check whether row already exists
                try (PreparedStatement chk = erp.prepareStatement(
                        "SELECT instructor_id FROM instructors WHERE instructor_id = ? LIMIT 1"
                )) {
                    chk.setLong(1, authUserId);
                    try (ResultSet rs = chk.executeQuery()) {
                        if (rs.next()) {
                            // exists -> do nothing
                        } else {
                            try (PreparedStatement ins = erp.prepareStatement(
                                    "INSERT INTO instructors (instructor_id, full_name, department, username) " +
                                    "VALUES (?, ?, NULL, ?)"
                            )) {
                                ins.setLong(1, authUserId);
                                ins.setString(2, fullName);
                                ins.setString(3, username);
                                ins.executeUpdate();
                            }
                        }
                    }
                }

            } catch (SQLException ex) {
                throw new Exception("User added, but failed to create instructor row: " + ex.getMessage(), ex);
            }
        }

        // ----------------------------
        // 3) INSERT INTO students (if role=STUDENT)
        // students:
        //   student_id BIGINT AI (NOT USED manually)
        //   roll_no VARCHAR(50) UNIQUE NOT NULL
        //   full_name VARCHAR(200) NOT NULL
        //   program VARCHAR(100)
        //   year INT
        //   current_sem tinyint
        //   user_id BIGINT FK (nullable)
        //   department VARCHAR(100)
        // ----------------------------
        if ("STUDENT".equalsIgnoreCase(role)) {

            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) fullName = username;

            try (Connection erp = DBConnection.getErpConnection()) {

                String sql = """
                    INSERT INTO students 
                    (roll_no, full_name, program, year, current_sem, user_id, department)
                    VALUES (?, ?, 'Unknown', 1, 1, ?, 'IIIT-Delhi')
                """;

                try (PreparedStatement ps = erp.prepareStatement(sql)) {
                    ps.setString(1, username);     // roll_no
                    ps.setString(2, fullName);
                    ps.setLong(3, authUserId);     // FK match
                    ps.executeUpdate();
                }

            } catch (SQLException ex) {
                throw new Exception("User added, but failed to create student row: " + ex.getMessage(), ex);
            }
        }

        return authUserId;
    }

    @Override
    public boolean deleteAuthUser(long userId) throws Exception {
        return userDao.deleteUser(userId);
    }
}
