package edu.univ.erp.ui;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.util.DBConnection;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.sql.Connection;

/**
 * LoginPanel — cleaned, uses AuthService, displays lock/failed info, resolves instructor/student context.
 * Keep behavior consistent: role mapping: Admin=1, Instructor=2, Student=3
 */
public class LoginPanel extends JPanel {
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
        setBackground(Theme.BACKGROUND);

        JPanel card = createCard();
        add(card);
    }

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(480, 580));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                new EmptyBorder(28, 30, 28, 30)
        ));

        JLabel logo = new JLabel();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        try (InputStream in = getClass().getResourceAsStream("/iiitd_logo.png")) {
            if (in != null) {
                Image img = ImageIO.read(in).getScaledInstance(110, 44, Image.SCALE_SMOOTH);
                logo.setIcon(new ImageIcon(img));
            } else {
                logo.setText("IIITD");
                logo.setFont(logo.getFont().deriveFont(Font.BOLD, 22f));
                logo.setForeground(Theme.PRIMARY);
            }
        } catch (Exception e) {
            logo.setText("IIITD");
            logo.setForeground(Theme.PRIMARY);
        }
        card.add(logo);
        card.add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel title = new JLabel("Welcome Back");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(Theme.NEUTRAL_DARK);
        card.add(title);

        JLabel subtitle = new JLabel("Sign in to your IIITD account");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        subtitle.setForeground(Theme.NEUTRAL_DARK);
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
        signInBtn.setBackground(Theme.PRIMARY);
        signInBtn.setForeground(Color.WHITE);
        signInBtn.setFocusPainted(false);
        signInBtn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        signInBtn.addActionListener(this::onSignIn);
        form.add(signInBtn);

        form.add(Box.createRigidArea(new Dimension(0, 16)));
        form.add(new JSeparator());
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel rlbl = new JLabel("Role-based Access");
        rlbl.setForeground(Theme.NEUTRAL_DARK);
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
        createAccBtn.setForeground(Theme.NEUTRAL_DARK);
        createAccBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createAccBtn.addActionListener(e -> main.showCard("signup"));
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(createAccBtn);

        JLabel footer = new JLabel("© IIITD. Need help? Contact IT Support");
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.setFont(footer.getFont().deriveFont(11f));
        footer.setForeground(Theme.NEUTRAL_DARK);
        card.add(footer);

        adminBtn.addActionListener(e -> selectedRole = 1);
        instBtn.addActionListener(e -> selectedRole = 2);
        studentBtn.addActionListener(e -> selectedRole = 3);

        passwordField.addActionListener(e -> signInBtn.doClick());
        return card;
    }

    private JTextField createPlaceholderField(String placeholder, boolean isPassword) {
        JTextField field;
        if (isPassword) {
            field = new JPasswordField();
            ((JPasswordField) field).setEchoChar((char)0);
        } else {
            field = new JTextField();
        }

        field.setPreferredSize(new Dimension(400, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));
        field.setForeground(Theme.NEUTRAL_DARK);
        field.setFont(Theme.BODY_FONT);

        field.setText(placeholder);
        field.setForeground(new Color(180, 180, 180));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Theme.NEUTRAL_DARK);
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

        if (isPassword) ((JPasswordField) field).setEchoChar((char) 0);

        return field;
    }

    private JToggleButton createChip(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setBackground(new Color(250, 250, 250));
        b.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(120, 40));
        b.setForeground(Theme.NEUTRAL_DARK);
        return b;
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
                    // on failure, show helpful lock/failed info
                    if (result == -1) {
                        String info = AuthService.getLockInfo(username);
                        if (info != null) statusLabel.setText(info);
                        else statusLabel.setText("Login failed — check credentials or role.");
                        return;
                    }

                    // success -> route
                    if (result == 1) {
                        main.showCard("admin");
                        return;
                    }

                    if (result == 2) {
                        // instructor: resolve instructor id and set context
                        long instructorId = 0L;
                        try {
                            Long id = AuthService.getInstructorIdByUsername(username);
                            if (id != null) instructorId = id;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        if (instructorId > 0) {
                            // Use 3-arg overload if available
                            try {
                                main.getInstructorPanel().setInstructorContext(instructorId, null, username);
                            } catch (NoSuchMethodError | AbstractMethodError ignored) {
                                // fallback to 2-arg if older
                                main.getInstructorPanel().setInstructorContext(instructorId, null);
                            }
                        } else {
                            System.out.println("Warning: instructor id not found for username=" + username);
                        }
                        main.showCard("instructor");
                        return;
                    }

                    if (result == 3) {
                        String sid = AuthService.getStudentIdByUsername(username);
                        if (sid == null) {
                            statusLabel.setText("Student record not found for this username/roll.");
                        } else {
                            main.setCurrentStudentId(sid);
                            main.showCard("student");
                        }
                        return;
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    setBusy(false, " ");
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
