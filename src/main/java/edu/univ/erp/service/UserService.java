package edu.univ.erp.service;

import edu.univ.erp.data.User;
import java.util.List;

public interface UserService {

    List<User> listAuthUsers() throws Exception;

    long createAuthUser(String username, String plainPassword, String role,
                        String firstName, String lastName, String email,
                        String phone) throws Exception;

    boolean deleteAuthUser(long userId) throws Exception;

    boolean usernameExists(String username) throws Exception;
}
