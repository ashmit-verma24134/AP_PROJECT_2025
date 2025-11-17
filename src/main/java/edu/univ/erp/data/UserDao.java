package edu.univ.erp.data;

import java.sql.SQLException;
import java.util.List;

public interface UserDao {
    long createUser(String username, String plainPassword, String role,
                    String firstName, String lastName, String email) throws Exception;
    List<User> listUsers() throws SQLException;
    boolean deleteUser(long userId) throws SQLException;
    boolean usernameExists(String username) throws SQLException;
}
