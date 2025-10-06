package ui;
import dao.UserDAOImpl;
import models.User;
import service.AuthService;
import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame{
    private final JTextField usernameField=new JTextField(20);
    private final JPasswordField passwordField=new JPasswordField(20);
    private final JButton loginButton=new JButton("Login");
    public LoginFrame() {
        super("Uni ERP - Login (prototype)");
        System.out.println("[LoginFrame] ctor: constructing LoginFrame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());
        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(loginButton);
        UserDAOImpl userDao =new UserDAOImpl();
        AuthService authService= new AuthService(userDao);
        loginButton.addActionListener(e -> {
            System.out.println("[LoginFrame] action: Login button clicked");
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            System.out.println("[LoginFrame] Username typed: '" + username + "', pwdLen: " + password.length());
            JOptionPane.showMessageDialog(this, "DEBUG: button clicked", "Debug", JOptionPane.INFORMATION_MESSAGE);
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter username and password!", "Missing", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                User user = authService.authenticate(username, password);
                System.out.println("[LoginFrame] Auth result: " + (user == null ? "FAILED" : "SUCCESS"));
                if (user == null) {
                    JOptionPane.showMessageDialog(this, "❌ Invalid username or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "✅ Login success! Role ID: " + user.getRoleId(), "Authenticated", JOptionPane.INFORMATION_MESSAGE);
                    openDashboardFor(user);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        pack();
        setLocationRelativeTo(null);
    }
    private void openDashboardFor(User user) {
        String title;
        if (user.getRoleId() == 1) title = "Admin Dashboard";
        else if (user.getRoleId() == 2) title = "Instructor Dashboard";
        else title = "Student Dashboard";
        JFrame dash = new JFrame(title);
        dash.setSize(600, 400);
        dash.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dash.add(new JLabel(title, SwingConstants.CENTER), BorderLayout.CENTER);
        dash.setLocationRelativeTo(this);
        dash.setVisible(true);
    }
}
