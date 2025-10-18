package edu.univ.erp.ui;

import edu.univ.erp.service.AuthService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.InputStream;


 
public class LoginPanel extends JPanel {
    // brand colors
    private static final Color PRIMARY_TEAL = new Color(47, 182, 173);   // #2FB6AD
    private static final Color DARK_GRAY = new Color(59, 59, 59);       // #3B3B3B
    private static final Color MID_GRAY = new Color(120, 130, 140);     // #78828C
    private static final Color SOFT_BG = new Color(245, 247, 250);      // #F5F7FA
    private static final Color BORDER_GRAY = new Color(220, 220, 220);

    private final MainFrame main;
    private  JTextField usernameField;
    private  JPasswordField passwordField;
    private  JButton signInBtn;
    private  JToggleButton adminBtn, instBtn, studentBtn;
    private  JLabel statusLabel;
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
                // fallback text
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

        // title + subtitle
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

        // form
        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

usernameField = createPlaceholderField("Username", false);
form.add(usernameField);
form.add(Box.createRigidArea(new Dimension(0, 12)));

passwordField = (JPasswordField) createPlaceholderField("Password", true);
form.add(passwordField);


        form.add(Box.createRigidArea(new Dimension(0, 12)));

        JCheckBox remember = new JCheckBox("Remember me");
        remember.setBackground(Color.WHITE);
        remember.setForeground(MID_GRAY);
        remember.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(remember);

        form.add(Box.createRigidArea(new Dimension(0, 14)));

        // sign in button (brand teal)
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

        // role chips
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

        JLabel footer = new JLabel("© IIITD. Need help? Contact IT Support");
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.setFont(footer.getFont().deriveFont(11f));
        footer.setForeground(MID_GRAY);
        card.add(footer);

        // role listeners
        adminBtn.addActionListener(e -> selectedRole = 1);
        instBtn.addActionListener(e -> selectedRole = 2);
        studentBtn.addActionListener(e -> selectedRole = 3);

        // enter key triggers sign in
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
    
    // helper to create a text field with placeholder (hint)
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
            BorderFactory.createLineBorder(Theme.BORDER_GRAY),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
    ));
    field.setForeground(Theme.DARK_GRAY);
    field.setFont(Theme.BODY_FONT);

    // add placeholder behavior
    field.setText(placeholder);
    field.setForeground(new Color(180, 180, 180)); // light gray placeholder

    field.addFocusListener(new java.awt.event.FocusAdapter() {
        @Override
        public void focusGained(java.awt.event.FocusEvent e) {
            if (field.getText().equals(placeholder)) {
                field.setText("");
                field.setForeground(Theme.DARK_GRAY);
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
        ((JPasswordField) field).setEchoChar((char) 0); // hide dots until focused
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
                        if (result == 1) main.showCard("admin");
                        else if (result == 2) main.showCard("instructor");
                        else if (result == 3) main.showCard("student");
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
