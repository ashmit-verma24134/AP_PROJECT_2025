package edu.univ.erp.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;
import edu.univ.erp.util.DBConnection;

public class SignUpPanel extends JPanel {
    private final MainFrame main;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmField;
    private final JLabel statusLabel;
    private final JToggleButton adminBtn, instBtn, studentBtn;

    private static final Color PRIMARY_TEAL = new Color(47, 182, 173);
    private static final Color MID_GRAY = new Color(120,130,140);
    private static final Color BORDER_GRAY = new Color(220,220,220);

    public SignUpPanel(MainFrame main) {
        this.main = main;
        setLayout(new GridBagLayout());
        setBackground(new Color(245,247,250));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(480, 620));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                new EmptyBorder(28, 30, 28, 30)
        ));

        JLabel title = new JLabel("Create an account");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel subtitle = new JLabel("Register your IIITD account");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(MID_GRAY);
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 18)));

        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(400,44));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(8,10,8,10)
        ));
        usernameField.setText("Username");
        usernameField.setForeground(new Color(140,140,140));
        usernameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (usernameField.getText().equals("Username")) { usernameField.setText(""); usernameField.setForeground(Color.BLACK); }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (usernameField.getText().isEmpty()) { usernameField.setText("Username"); usernameField.setForeground(new Color(140,140,140)); }
            }
        });
        card.add(usernameField);
        card.add(Box.createRigidArea(new Dimension(0,12)));

        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(400,44));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(8,10,8,10)
        ));
        passwordField.setText("Password");
        passwordField.setForeground(new Color(140,140,140));
        passwordField.setEchoChar((char)0);
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).equals("Password")) {
                    passwordField.setText(""); passwordField.setForeground(Color.BLACK); passwordField.setEchoChar('•');
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (passwordField.getPassword().length == 0) {
                    passwordField.setText("Password"); passwordField.setForeground(new Color(140,140,140)); passwordField.setEchoChar((char)0);
                }
            }
        });
        card.add(passwordField);
        card.add(Box.createRigidArea(new Dimension(0,12)));

        confirmField = new JPasswordField();
        confirmField.setPreferredSize(new Dimension(400,44));
        confirmField.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        confirmField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(8,10,8,10)
        ));
        confirmField.setText("Confirm Password");
        confirmField.setForeground(new Color(140,140,140));
        confirmField.setEchoChar((char)0);
        confirmField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (String.valueOf(confirmField.getPassword()).equals("Confirm Password")) {
                    confirmField.setText(""); confirmField.setForeground(Color.BLACK); confirmField.setEchoChar('•');
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (confirmField.getPassword().length == 0) {
                    confirmField.setText("Confirm Password"); confirmField.setForeground(new Color(140,140,140)); confirmField.setEchoChar((char)0);
                }
            }
        });
        card.add(confirmField);
        card.add(Box.createRigidArea(new Dimension(0,12)));

        JLabel rlbl = new JLabel("Sign up as");
        rlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        rlbl.setForeground(MID_GRAY);
        card.add(rlbl);
        card.add(Box.createRigidArea(new Dimension(0,8)));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        chips.setBackground(Color.WHITE);
        adminBtn = new JToggleButton("Admin"); instBtn = new JToggleButton("Instructor"); studentBtn = new JToggleButton("Student");
        adminBtn.setPreferredSize(new Dimension(120,40)); instBtn.setPreferredSize(new Dimension(120,40)); studentBtn.setPreferredSize(new Dimension(120,40));
        adminBtn.setFocusPainted(false); instBtn.setFocusPainted(false); studentBtn.setFocusPainted(false);
        ButtonGroup g = new ButtonGroup(); g.add(adminBtn); g.add(instBtn); g.add(studentBtn);
        studentBtn.setSelected(true);
        chips.add(adminBtn); chips.add(instBtn); chips.add(studentBtn);
        card.add(chips);
        card.add(Box.createRigidArea(new Dimension(0,16)));

        JButton createBtn = new JButton("Create account");
        createBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        createBtn.setPreferredSize(new Dimension(400,44));
        createBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        createBtn.setBackground(PRIMARY_TEAL);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.addActionListener(e -> onSignup());
        card.add(createBtn);

        card.add(Box.createRigidArea(new Dimension(0,12)));
        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setForeground(new Color(160,40,40));
        card.add(statusLabel);

        card.add(Box.createRigidArea(new Dimension(0,18)));
        JButton back = new JButton("Back to Login");
        back.setAlignmentX(Component.CENTER_ALIGNMENT);
        back.setBorderPainted(false);
        back.setContentAreaFilled(false);
        back.setForeground(MID_GRAY);
        back.addActionListener(e -> main.showCard("login"));
        card.add(back);

        add(card);
    }

    private int chosenRoleId() {
        if (adminBtn.isSelected()) return 1;
        if (instBtn.isSelected()) return 2;
        return 3;
    }

private void onSignup() {
    String username = usernameField.getText().trim();
    String pass = new String(passwordField.getPassword());
    String confirm = new String(confirmField.getPassword());
    int roleId = chosenRoleId();

    if (username.isEmpty() || username.equals("Username") ||
        pass.isEmpty() || pass.equals("Password") ||
        confirm.isEmpty() || confirm.equals("Confirm Password")) {
        statusLabel.setText("Please fill all fields.");
        return;
    }
    if (!pass.equals(confirm)) {
        statusLabel.setText("Passwords do not match.");
        return;
    }

    // hash password before inserting
    String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(pass, org.mindrot.jbcrypt.BCrypt.gensalt());

    try (Connection conn = DBConnection.getConnection()) {
        boolean originalAuto = true;
        try {
            originalAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // check username exists
            try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        statusLabel.setText("Username already exists.");
                        return;
                    }
                }
            }

            // insert into users
            long newUserId = -1L;
            String insertUserSql = "INSERT INTO users (username, pass_hash, role_id) VALUES (?, ?, ?)";
            try (PreparedStatement ins = conn.prepareStatement(insertUserSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, username);
                ins.setString(2, hashed);
                ins.setInt(3, roleId);
                int rows = ins.executeUpdate();
                if (rows == 0) throw new SQLException("Creating user failed, no rows affected.");
                try (ResultSet gk = ins.getGeneratedKeys()) {
                    if (gk.next()) newUserId = gk.getLong(1);
                    else throw new SQLException("Creating user failed, no generated key obtained.");
                }
            }

            // if it's a student, also create erp_db.students row
            if (roleId == 3) {
                String insertStudentSql =
                    "INSERT INTO erp_db.students (student_id, roll_no, full_name, program, year, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
                try (PreparedStatement ins2 = conn.prepareStatement(insertStudentSql)) {
                    ins2.setLong(1, newUserId);
                    ins2.setString(2, username);
                    ins2.setString(3, "Auto " + username);
                    ins2.setString(4, "Unknown");
                    ins2.setInt(5, 1);
                    ins2.executeUpdate();
                }
            }

            conn.commit();
            statusLabel.setForeground(new Color(0,128,0));
            statusLabel.setText("Signup successful");
            JOptionPane.showMessageDialog(this, "Account created. Please login.");
            main.showCard("login");

        } catch (SQLException ex) {
            try { conn.rollback(); } catch (Exception ignore) {}
            ex.printStackTrace();
            statusLabel.setText("DB error: " + ex.getMessage());
        } finally {
            try { conn.setAutoCommit(originalAuto); } catch (Exception ignore) {}
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        statusLabel.setText("Error: " + ex.getMessage());
    }
}

}
