package edu.univ.erp.ui;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.util.DBConnection;
import edu.univ.erp.data.*;

import javax.imageio.ImageIO;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.sql.Connection;

public class LoginPanel extends JPanel {
    // brand colors (kept for local use)
    private static final Color PRIMARY_TEAL = new Color(47, 182, 173);   // #2FB6AD
    private static final Color DARK_GRAY = new Color(59, 59, 59);       // #3B3B3B
    private static final Color MID_GRAY = new Color(120, 130, 140);     // #78828C
    private static final Color SOFT_BG = new Color(245, 247, 250);      // #F5F7FA
    private static final Color BORDER_GRAY = new Color(220, 220, 220);

    private final MainFrame main;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton signInBtn;
    private JToggleButton adminBtn, instBtn, studentBtn;
    private JLabel statusLabel;
    private int selectedRole = 1;

    public LoginPanel(MainFrame main) {
        this.main = main;
        setLayout(new GridBagLayout());
        setBackground(SOFT_BG);

        JPanel card = createCard();
        add(card);
    }

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(480, 580));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                new EmptyBorder(28, 30, 28, 30)
        ));

        // logo (loads resources/iiitd_logo.png)
        JLabel logo = new JLabel();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        try (InputStream in = getClass().getResourceAsStream("/iiitd_logo.png")) {
            if (in != null) {
                Image img = ImageIO.read(in).getScaledInstance(110, 44, Image.SCALE_SMOOTH);
                logo.setIcon(new ImageIcon(img));
            } else {
                logo.setText("IIITD");
                logo.setFont(logo.getFont().deriveFont(Font.BOLD, 22f));
                logo.setForeground(PRIMARY_TEAL);
            }
        } catch (Exception e) {
            logo.setText("IIITD");
            logo.setForeground(PRIMARY_TEAL);
        }
        card.add(logo);
        card.add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel title = new JLabel("Welcome Back");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(DARK_GRAY);
        card.add(title);

        JLabel subtitle = new JLabel("Sign in to your IIITD account");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        subtitle.setForeground(MID_GRAY);
        card.add(subtitle);

        card.add(Box.createRigidArea(new Dimension(0, 18)));

        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        usernameField = createPlaceholderField("Username", false);
        form.add(usernameField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        passwordField = (JPasswordField) createPlaceholderField("Password", true);
        form.add(passwordField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));
        form.add(Box.createRigidArea(new Dimension(0, 14)));

        signInBtn = new JButton("Sign In");
        signInBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        signInBtn.setPreferredSize(new Dimension(400, 46));
        signInBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        signInBtn.setBackground(PRIMARY_TEAL);
        signInBtn.setForeground(Color.WHITE);
        signInBtn.setFocusPainted(false);
        signInBtn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        signInBtn.addActionListener(this::onSignIn);
        form.add(signInBtn);

        form.add(Box.createRigidArea(new Dimension(0, 16)));
        form.add(new JSeparator());
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel rlbl = new JLabel("Role-based Access");
        rlbl.setForeground(MID_GRAY);
        rlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(rlbl);
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        chips.setBackground(Color.WHITE);
        adminBtn = createChip("Admin");
        instBtn = createChip("Instructor");
        studentBtn = createChip("Student");
        ButtonGroup g = new ButtonGroup();
        g.add(adminBtn); g.add(instBtn); g.add(studentBtn);
        adminBtn.setSelected(true);
        chips.add(adminBtn); chips.add(instBtn); chips.add(studentBtn);
        form.add(chips);

        form.add(Box.createRigidArea(new Dimension(0, 12)));
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(160, 40, 40));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(statusLabel);

        card.add(form);
        card.add(Box.createVerticalGlue());

        JButton createAccBtn = new JButton("Create account");
        createAccBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        createAccBtn.setBorderPainted(false);
        createAccBtn.setContentAreaFilled(false);
        createAccBtn.setForeground(MID_GRAY);
        createAccBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createAccBtn.addActionListener(e -> main.showCard("signup"));
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(createAccBtn);

        JLabel footer = new JLabel("© IIITD. Need help? Contact IT Support");
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.setFont(footer.getFont().deriveFont(11f));
        footer.setForeground(MID_GRAY);
        card.add(footer);

        adminBtn.addActionListener(e -> selectedRole = 1);
        instBtn.addActionListener(e -> selectedRole = 2);
        studentBtn.addActionListener(e -> selectedRole = 3);

        passwordField.addActionListener(e -> signInBtn.doClick());
        return card;
    }

    private JTextField createField(String tooltip) {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(400, 44));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        f.setToolTipText(tooltip);
        return f;
    }

    private JToggleButton createChip(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setBackground(new Color(250, 250, 250));
        b.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(120, 40));
        b.setForeground(DARK_GRAY);
        return b;
    }

    // fixed placeholder field to match new Theme names
    private JTextField createPlaceholderField(String placeholder, boolean isPassword) {
        JTextField field;
        if (isPassword) {
            field = new JPasswordField();
            ((JPasswordField) field).setEchoChar('•');
        } else {
            field = new JTextField();
        }

        field.setPreferredSize(new Dimension(400, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),   // ✅ fixed
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setForeground(Theme.NEUTRAL_DARK); // ✅ fixed
        field.setFont(Theme.BODY_FONT); // stays same

        field.setText(placeholder);
        field.setForeground(new Color(180, 180, 180));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Theme.NEUTRAL_DARK); // ✅ fixed
                    if (isPassword) ((JPasswordField) field).setEchoChar('•');
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(new Color(180, 180, 180));
                    field.setText(placeholder);
                    if (isPassword) ((JPasswordField) field).setEchoChar((char) 0);
                }
            }
        });

        if (isPassword) {
            ((JPasswordField) field).setEchoChar((char) 0);
        }

        return field;
    }

    private void onSignIn(ActionEvent ev) {
        String username = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (username.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        setBusy(true, "Signing in...");
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return AuthService.authenticateByRole(username, pass, selectedRole);
            }

            @Override
            protected void done() {
                try {
                    int result = get();
                    setBusy(false, " ");
                    if (result == -1) {
                        statusLabel.setText("Login failed — check credentials or role.");
                    } else {
                        if (result == 1) {
    // Admin login - pass username
    try {
        main.getAdminPanel().setAdminUsername(username);
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    main.showCard("admin");
}
                      else if (result == 2) {
    // Instructor login – resolve instructor id then set context so MyCoursesPanel loads
    long instructorId = 0L;

    try {
        // 1) Optional helper via AuthService if present
        try {
            java.lang.reflect.Method m = AuthService.class.getMethod("getInstructorIdByUsername", String.class);
            Object val = m.invoke(null, username);
            if (val != null) {
                if (val instanceof Number) instructorId = ((Number) val).longValue();
                else instructorId = Long.parseLong(String.valueOf(val));
            }
        } catch (NoSuchMethodException ignored) {}

        try (Connection conn = DBConnection.getErpConnection()) {
            // 2) Try instructors.username (you already added this column)
            if (instructorId == 0L) {
                try (java.sql.PreparedStatement ps0 = conn.prepareStatement(
                        "SELECT instructor_id FROM instructors WHERE username = ? LIMIT 1")) {
                    ps0.setString(1, username);
                    try (java.sql.ResultSet rs0 = ps0.executeQuery()) {
                        if (rs0.next()) instructorId = rs0.getLong("instructor_id");
                    }
                }
            }

            // 3) Try mapping users.email -> users.id -> instructors.instructor_id
            if (instructorId == 0L) {
                try (java.sql.PreparedStatement pus = conn.prepareStatement(
                        "SELECT id FROM users WHERE email = ? LIMIT 1")) {
                    pus.setString(1, username);
                    try (java.sql.ResultSet rus = pus.executeQuery()) {
                        if (rus.next()) {
                            long userId = rus.getLong("id");
                            try (java.sql.PreparedStatement pis = conn.prepareStatement(
                                    "SELECT instructor_id FROM instructors WHERE instructor_id = ? LIMIT 1")) {
                                pis.setLong(1, userId);
                                try (java.sql.ResultSet ris = pis.executeQuery()) {
                                    if (ris.next()) instructorId = ris.getLong("instructor_id");
                                }
                            }
                        }
                    }
                }
            }

            // 4) Last resort: full_name LIKE match
            if (instructorId == 0L) {
                try (java.sql.PreparedStatement ps1 = conn.prepareStatement(
                        "SELECT instructor_id FROM instructors WHERE full_name LIKE ? LIMIT 1")) {
                    ps1.setString(1, "%" + username + "%");
                    try (java.sql.ResultSet rs1 = ps1.executeQuery()) {
                        if (rs1.next()) instructorId = rs1.getLong("instructor_id");
                    }
                }
            }
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }

    System.out.println("DEBUG: Resolved instructorId=" + instructorId + " for username=" + username);

    if (instructorId > 0) {
        try { 
            // Pass username to setInstructorContext (3 parameter version)
            main.getInstructorPanel().setInstructorContext(instructorId, null, username); 
        } catch (Exception ex) { 
            ex.printStackTrace(); 
        }
    } else {
        System.out.println("Warning: instructor id not found for username=" + username);
    }

    main.showCard("instructor");
}


else if (result == 3) {
                            String sid = AuthService.getStudentIdByUsername(username);
                            if (sid == null) {
                                statusLabel.setText("Student record not found for this username/roll.");
                            } else {
                                main.setCurrentStudentId(sid);
                            }
                        }
                    }
                } catch (Exception ex) {
                    setBusy(false, " ");
                    ex.printStackTrace();
                    statusLabel.setText("An error occurred. See console.");
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy, String message) {
        signInBtn.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        adminBtn.setEnabled(!busy);
        instBtn.setEnabled(!busy);
        studentBtn.setEnabled(!busy);
        statusLabel.setText(message);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
