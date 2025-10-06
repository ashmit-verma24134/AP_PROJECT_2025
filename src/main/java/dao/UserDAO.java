package dao;
import models.User;

public interface UserDAO {
    User findByUsername(String username) throws Exception;
    long createUser(User u) throws Exception;
}
