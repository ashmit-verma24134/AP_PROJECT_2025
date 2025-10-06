package service;
import dao.UserDAO;
import models.User;
import org.mindrot.jbcrypt.BCrypt;
public class AuthService{
    private final UserDAO userDAO;
    public AuthService(UserDAO userDAO)
     { 
        this.userDAO = userDAO;
     }
    public User authenticate(String username,String password) throws Exception{
        User u=userDAO.findByUsername(username);
        if (u==null) return null;
        if (BCrypt.checkpw(password, u.getPassHash())) return u;
        return null;
    }
}

//mvn -f tools/pom.xml compile exec:java -Dexec.mainClass=tools.HashPassword -Dexec.args="admin1Password"
//need to use this to activate daoooooooooooooooooooooooo... used mindrot lib for this