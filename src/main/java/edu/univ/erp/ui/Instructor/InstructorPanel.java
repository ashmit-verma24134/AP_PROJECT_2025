package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;

import edu.univ.erp.ui.Instructor.DashboardPanel;
import edu.univ.erp.ui.Instructor.MyCoursesPanel;
import edu.univ.erp.ui.Instructor.CourseDetailsPanel;
import edu.univ.erp.ui.Instructor.InstructorTimetablePanel;


import javax.swing.*;
import java.awt.*;

/**
 * InstructorPanel – clean sidebar + main content using CardLayout.
 * Provides navigation for instructor-specific modules.
 */
public class InstructorPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JPanel navPanel = new JPanel(null);
    private final JPanel navButtonsContainer = new JPanel();
    private final JPanel cards = new JPanel(new CardLayout());

    // Placeholder pages
    // Real panels
    private final JPanel dashboardPanel = new DashboardPanel();
    private final JPanel coursesPanel = new MyCoursesPanel();
    // current instructor context (set after login)
    private long currentInstructorId = 0L;
    private String currentTerm = null;
    private String instructorUsername = "Instructor"; // Store username

    private final JPanel gradebookPanel = new InstructorGradebookPanel();
    private final JPanel timetablePanel = new InstructorTimetablePanel();
    private final JPanel announcementsPanel = createPlaceholderPanel("📢 Announcements - Post or view messages");
    private final JPanel profilePanel = createPlaceholderPanel("👤 Profile - Manage personal information");

    // Welcome label to update dynamically
    private final JLabel welcomeLabel = new JLabel("Welcome, Instructor");

    public InstructorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // === Header ===
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        welcomeLabel.setFont(Theme.TITLE_FONT);
        welcomeLabel.setForeground(Color.WHITE);

        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(Theme.PRIMARY);
        headerWrap.add(welcomeLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);
        
        JButton changePassword = new JButton("Change Password");
        changePassword.setBackground(new Color(255, 255, 255, 180));
        changePassword.setForeground(Theme.PRIMARY);
        changePassword.setFocusPainted(false);
        changePassword.addActionListener(e -> showChangePasswordDialog());
        
        JButton logout = new JButton("Logout");
        logout.setBackground(Color.WHITE);
        logout.setForeground(Theme.PRIMARY);
        logout.setFocusPainted(false);
        logout.addActionListener(e -> mainFrame.showCard("login"));
        
        rightPanel.add(changePassword);
        rightPanel.add(logout);
        headerWrap.add(rightPanel, BorderLayout.EAST);

        add(headerWrap, BorderLayout.NORTH);

        // === Sidebar ===
        navPanel.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        navPanel.setBackground(Theme.SIDEBAR_BG);

        navButtonsContainer.setLayout(new BoxLayout(navButtonsContainer, BoxLayout.Y_AXIS));
        navButtonsContainer.setOpaque(false);
        navButtonsContainer.setBounds(0, 16, Theme.SIDEBAR_WIDTH, 600);

        JButton btnDashboard = makeNavButton("Dashboard");
        JButton btnCourses = makeNavButton("My Courses");
        JButton btnGradebook = makeNavButton("Gradebook");
        JButton btnTimetable = makeNavButton("Timetable");
        JButton btnAnnouncements = makeNavButton("Announcements");
        JButton btnProfile = makeNavButton("Profile");

        navButtonsContainer.add(Box.createVerticalStrut(12));
        navButtonsContainer.add(btnDashboard);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnCourses);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnGradebook);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnTimetable);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnAnnouncements);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnProfile);
        navButtonsContainer.add(Box.createVerticalGlue());

        navPanel.add(navButtonsContainer);
        add(navPanel, BorderLayout.WEST);

        // === Card Layout (Right side) ===
        cards.setBackground(Theme.BACKGROUND);
        cards.add(wrapInPadding(dashboardPanel), "dashboard");
        cards.add(wrapInPadding(coursesPanel), "courses");
        cards.add(wrapInPadding(gradebookPanel), "gradebook");
        cards.add(wrapInPadding(timetablePanel), "timetable");
        cards.add(wrapInPadding(announcementsPanel), "announcements");
        cards.add(wrapInPadding(profilePanel), "profile");

        add(cards, BorderLayout.CENTER);

        // === Navigation actions ===
        btnDashboard.addActionListener(e -> { setNavActive(btnDashboard); showCard("dashboard"); });
        btnCourses.addActionListener(e -> {
            setNavActive(btnCourses);
            if (currentInstructorId > 0 && coursesPanel instanceof edu.univ.erp.ui.Instructor.MyCoursesPanel) {
                ((edu.univ.erp.ui.Instructor.MyCoursesPanel) coursesPanel).loadForInstructor(currentInstructorId, currentTerm);
            }
            showCard("courses");
        });
        btnGradebook.addActionListener(e -> { setNavActive(btnGradebook); showCard("gradebook"); });
        btnTimetable.addActionListener(e -> { setNavActive(btnTimetable); showCard("timetable"); });
        btnAnnouncements.addActionListener(e -> { setNavActive(btnAnnouncements); showCard("announcements"); });
        btnProfile.addActionListener(e -> { setNavActive(btnProfile); showCard("profile"); });

        // Default active page
        SwingUtilities.invokeLater(() -> {
            setNavActive(btnDashboard);
            showCard("dashboard");
        });
    }

    /** Utility: Create uniform navigation button */
    private JButton makeNavButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_ITEM_HEIGHT));
        b.setFont(Theme.BODY_FONT);
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        b.setForeground(Color.WHITE);
        b.setBackground(Theme.SIDEBAR_BG);
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        return b;
    }

    /** Highlight the selected nav item */
    private void setNavActive(AbstractButton active) {
        for (Component c : navButtonsContainer.getComponents()) {
            if (c instanceof AbstractButton) {
                c.setBackground(Theme.SIDEBAR_BG);
                ((AbstractButton) c).setForeground(Color.WHITE);
            }
        }
        active.setBackground(Theme.SIDEBAR_ACTIVE);
        active.setForeground(Color.WHITE);
    }

    /**
     * Called after login to set which instructor this panel should show.
     * Now also accepts username to display.
     */
    public void setInstructorContext(long instructorId, String term, String username) {
        this.currentInstructorId = instructorId;
        this.currentTerm = term;
        this.instructorUsername = username == null ? "Instructor" : username;
        
        // Update welcome label
        welcomeLabel.setText("Welcome, " + this.instructorUsername);
        
        // Set context for gradebook panel
        if (gradebookPanel instanceof InstructorGradebookPanel) {
            ((InstructorGradebookPanel) gradebookPanel).setInstructorContext(instructorId, term);
        }
        
        try {
            if (coursesPanel instanceof edu.univ.erp.ui.Instructor.MyCoursesPanel && instructorId > 0) {
                ((edu.univ.erp.ui.Instructor.MyCoursesPanel) coursesPanel).loadForInstructor(instructorId, term);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Backward compatibility - keep old method signature
    public void setInstructorContext(long instructorId, String term) {
        setInstructorContext(instructorId, term, null);
    }

    /** Show page inside CardLayout */
    private void showCard(String name) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
    }

    /** Wrap each content panel in rounded white container */
    private JComponent wrapInPadding(JComponent c) {
        RoundedPanel p = new RoundedPanel(Theme.BORDER_RADIUS);
        p.setLayout(new BorderLayout());
        p.setBackground(Theme.SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(
                Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Temporary placeholder panels for UI structure */
    private JPanel createPlaceholderPanel(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        JLabel lbl = new JLabel("<html><center>" + text + "</center></html>", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        panel.add(lbl, BorderLayout.CENTER);
        return panel;
    }
    
    /** Show change password dialog */
    private void showChangePasswordDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Change Password", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPasswordField oldPassword = new JPasswordField(20);
        JPasswordField newPassword = new JPasswordField(20);
        JPasswordField confirmPassword = new JPasswordField(20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Current Password:"), gbc);
        gbc.gridx = 1;
        panel.add(oldPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("New Password:"), gbc);
        gbc.gridx = 1;
        panel.add(newPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        panel.add(confirmPassword, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            String oldPass = new String(oldPassword.getPassword());
            String newPass = new String(newPassword.getPassword());
            String confirmPass = new String(confirmPassword.getPassword());
            
            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(dialog, "New passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPass.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 6 characters!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // TODO: Implement actual password change logic with DB
            JOptionPane.showMessageDialog(dialog, "Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
}