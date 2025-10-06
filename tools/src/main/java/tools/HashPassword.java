package tools;

import org.mindrot.jbcrypt.BCrypt;

public class HashPassword {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: mvn -f tools/pom.xml exec:java -Dexec.mainClass=tools.HashPassword -Dexec.args=\"<plaintext>\"");
            System.exit(1);
        }
        String plaintext = args[0];
        String hash = BCrypt.hashpw(plaintext, BCrypt.gensalt(12));
        System.out.println("Hashed password:\n" + hash);
    }
}
