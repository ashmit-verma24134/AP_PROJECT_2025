package test;
import dao.UserDAOImpl;
import service.AuthService;
import models.User;
public class MainAuthTest{
    public static void main(String[] args) throws Exception{
        UserDAOImpl dao = new UserDAOImpl();
        AuthService auth = new AuthService(dao);
        String username = "admin1";
        String plaintext = "$2a$12$T4604dwSi0D4TgrWtjauU.4wCjm2IXmSf"; // the exact plaintext you hashed earlier
        System.out.println("Checking user: " + username);
        User u = dao.findByUsername(username);
        System.out.println("DAO returned: " + (u == null ? "null" : u));
        User logged = auth.authenticate(username, plaintext);
        System.out.println("Auth result: " + (logged == null ? "FAILED" : "SUCCESS -> " + logged));
    }
}
