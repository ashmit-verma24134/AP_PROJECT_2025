import javax.swing.*;
//import java.io.*;
import jawa.awt.*;
import java.awt.event.*;
import java.sql.*;

public class signup {
    public static void main(String[] args){
        JFrame frame = new JFrame("ERP - Sign UP");
        frame.setSize(800,600);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(new Color(245, 247, 250));   //bg light grey kar diya

        /*JLabel title = new JLabel("Create Account");
        title.setBounds(130, 30, 200, 30);
        frame.add(title);
        */

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Color.WHITE);
        panel.setBounds(250, 100, 300, 400);    //bich mein white rectangle
        frame.add(panel);

        ImageIcon logo = new ImageIcon("iiitd logo.png");
        Image img = logo.getImage();
        Image resize = img.getScaledInstance(80,50,Image.SCALE_SMOOTH );  //logo add kar diya
        logo = new ImageIcon(resize);
        JLabel logo_label = new JLabel(logo);
        logo_label.setBounds(110,20,80,50);
        panel.add(logo_label);


        JLabel wlcm = new JLabel("Welcome Back");
        wlcm.setFont(new Font("Arial", Font.BOLD, 16));     //welcome back added
        wlcm.setBounds(90,80,200,25);
        panel.add(wlcm);
        
        JLabel subtitle = new JLabel("Sign in to your IIITD account");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 11));
        subtitle.setForeground(Color.GRAY);
        subtitle.setBounds(80,100,200,20);
        panel.add(subtitle);

        JTextField us_n = new JTextField();
        us_n.setBounds(50, 140, 200, 30);
        us_n.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));       //username ka box daal diya
        panel.add(us_n);

        JPasswordField pw = new JPasswordField();
        pw.setBounds(50, 185, 200, 30);
        pw.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));         //passsword ka box daal diya
        panel.add(pw);

        

        JCheckBox rem = new JCheckBox("Remember me");       
        rem.setBounds(50, 220, 120, 20);                //rem me ka checkboxx bhi add ho gaya 
        rem.setBackground(Color.WHITE);
        panel.add(rem);

        JButton sign_button = new JButton("Sign In");
        sign_button.setBounds(50, 250, 200, 35);
        sign_button.setBackground(Color.decode("#2FB6AD"));   //sign in vala button added
        sign_button.setForeground(Color.WHITE);
        sign_button.setFocusPainted(false);
        sign_button.setBorderPainted(false);
        panel.add(sign_button);


        loginButton.addMouseListener(new MouseAdapter() {           //hover efffect 
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(Color.decode("#259b8d"));
            }
            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(Color.decode("#2FB6AD"));
            }
        });

        JLabel role = new JLabel("Role-based Access");
        role.setFont(new Font("Arial", Font.PLAIN, 11));        // roles vala section-heading 
        role.setForeground(Color.GRAY);
        role.setBounds(95, 300, 200, 20);
        panel.add(role);

        JButton admBtn = new JButton("Admin");
        JButton instBtn = new JButton("Instructor");
        JButton stuBtn = new JButton("Student");

        adm.setBounds(30, 330, 70, 25);                 //added buttons for all roles
        instBtn.setBounds(110, 330, 90, 25);
        stuBtn.setBounds(210, 330, 70, 25);

        JButton[] buttons = {admBtn, instBtn, stuBtn};
        for (JButton b : buttons) {
            b.setFocusPainted(false);
            b.setBackground(Color.WHITE);
            b.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            panel.add(b);
        }

        
        instBtn.setBackground(Color.LIGHT_GRAY);

        
        JLabel footer = new JLabel("© IIITD. Need help? Contact IT Support", SwingConstants.CENTER);
        footer.setBounds(0, 520, 800, 30);
        footer.setFont(new Font("Arial", Font.PLAIN, 11));
        footer.setForeground(Color.GRAY);
        frame.add(footer);

        
        frame.setVisible(true);

        private void handleSignup() {
        String username = us_n.getText();
        String password = new String(pw.getPassword());
    

        if (us_n.isEmpty() || pw.isEmpty()) {
            messageLabel.setText("All fields are required!");
            messageLabel.setForeground(Color.RED);
            return;
        }

        //if (!pw.equals(confirm)) {
          //  messageLabel.setText("Passwords do not match!");
           // messageLabel.setForeground(Color.RED);
            //return;
        //}

        try {
            // connect to DB using existing DBConnection class
            conn = DBConnection.getConnection();

            // check if username exists
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                messageLabel.setText("Username already exists!");
                messageLabel.setForeground(Color.RED);
                return;
            }

            // hash password with BCrypt
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());

            // insert user (default role_id = 3 for Student)
            PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (username, pass_hash, role_id) VALUES (?, ?, 3)");
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashed);
            insertStmt.executeUpdate();

            messageLabel.setText("Signup successful!");
            messageLabel.setForeground(Color.decode("#2FB6AD"));

            JOptionPane.showMessageDialog(frame, "Account created! Redirecting to login...");
            frame.dispose(); // close signup
            // new LoginPage(); // you can open login page here later

        } catch (Exception ex) {
            ex.printStackTrace();
            messageLabel.setText("Error: " + ex.getMessage());
            messageLabel.setForeground(Color.RED);
        }
    }
}




