package edu.univ.erp.ui.Instructor;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import java.awt.*;

/**
 * InstructorPanel — integrated version with maintenance-aware Change Password blocking
 * and maintenance banner styled like Admin/Student panels (light, centered).
 */
public class InstructorPanel extends JPanel {
    private final MainFrame mainFrame;

    // UI regions
    private final JPanel navPanel = new JPanel(null);
    private final JPanel navButtonsContainer = new JPanel();
    private final JPanel cards = new JPanel(new CardLayout());

    // top-level maintenance banner (visible under header) — styled like student/admin panels
    private final JPanel maintenanceBanner = new JPanel(new BorderLayout());

    // nav button stored as a field so maintenance updater can enable/disable it if needed
    private JButton btnGradebook;

    // Panels (existing classes)
    private final MyCoursesPanel coursesPanel = new MyCoursesPanel(); // instructor-side MyCoursesPanel
    private final InstructorGradebookPanel gradebookPanel = new InstructorGradebookPanel();
    private final JPanel notificationsPanel = new NotificationPanel();


    // context
    private long currentInstructorId = 0L;
    private String currentTerm = null;
    private String instructorUsername = "Instructor";
    private String instructorAuthUsername = null; // used by change-password flow

    // header elements that need runtime update
    private final JLabel welcomeLabel = new JLabel("Welcome, Instructor");

    // change-password button promoted to a field so maintenance toggles can control it
    private JButton changePasswordBtn;

    public InstructorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ----- Header -----
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(Theme.PRIMARY);
        headerWrap.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        welcomeLabel.setFont(Theme.TITLE_FONT);
        welcomeLabel.setForeground(Color.WHITE);
        headerWrap.add(welcomeLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        // create changePasswordBtn as a field
        changePasswordBtn = new JButton("Change Password");
        changePasswordBtn.setBackground(new Color(255, 255, 255, 180));
        changePasswordBtn.setForeground(Theme.PRIMARY);
        changePasswordBtn.setFocusPainted(false);
        changePasswordBtn.addActionListener(e -> showChangePasswordDialog());

        JButton logout = new JButton("Logout");
        logout.setBackground(Color.WHITE);
        logout.setForeground(Theme.PRIMARY);
        logout.setFocusPainted(false);
        logout.addActionListener(e -> mainFrame.showCard("login"));

        rightPanel.add(changePasswordBtn);
        rightPanel.add(logout);
        headerWrap.add(rightPanel, BorderLayout.EAST);

        // Header + Banner wrapper
        JPanel headerAndBanner = new JPanel(new BorderLayout());
        headerAndBanner.add(headerWrap, BorderLayout.NORTH);

        // ----- Maintenance banner (styled like Admin/Student) -----
        // Light background, central small icon and darker text — matches other panels' UI
        maintenanceBanner.setBackground(new Color(254, 246, 243)); // light/beige-ish
        maintenanceBanner.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel mLabel = new JLabel("<html>&#x26A0;&nbsp;<b>Site is in Maintenance Mode — contents are view-only.</b></html>", SwingConstants.CENTER);
        mLabel.setForeground(new Color(117, 40, 24)); // dark/brownish text
        mLabel.setFont(Theme.BODY_BOLD);
        mLabel.setHorizontalAlignment(SwingConstants.CENTER);
        maintenanceBanner.add(mLabel, BorderLayout.CENTER);
        maintenanceBanner.setVisible(false); // hidden initially
        headerAndBanner.add(maintenanceBanner, BorderLayout.SOUTH);

        add(headerAndBanner, BorderLayout.NORTH);

        // ----- Sidebar -----
        navPanel.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        navPanel.setBackground(Theme.SIDEBAR_BG);
        navButtonsContainer.setLayout(new BoxLayout(navButtonsContainer, BoxLayout.Y_AXIS));
        navButtonsContainer.setOpaque(false);
        navButtonsContainer.setBounds(0, 16, Theme.SIDEBAR_WIDTH, 600);

        JButton btnCourses = makeNavButton("My Courses");
        btnGradebook = makeNavButton("Gradebook"); // assign to field
        JButton btnNotifications = makeNavButton("Notifications");

        navButtonsContainer.add(Box.createVerticalStrut(12));
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnCourses);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnGradebook);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnNotifications);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(Box.createVerticalGlue());

        navPanel.add(navButtonsContainer);
        add(navPanel, BorderLayout.WEST);

        // ----- Cards -----
        cards.setBackground(Theme.BACKGROUND);
        // wrap children in consistent rounded surface
        cards.add(wrapInPadding(coursesPanel), "courses");
        cards.add(wrapInPadding(gradebookPanel), "gradebook");
        cards.add(wrapInPadding(notificationsPanel), "notifications");
        add(cards, BorderLayout.CENTER);

        // ----- Navigation actions -----

        btnCourses.addActionListener(e -> {
            setNavActive(btnCourses);
            if (currentInstructorId > 0) coursesPanel.loadForInstructor(currentInstructorId, currentTerm);
            showCard("courses");
        });
        btnGradebook.addActionListener(e -> {
            setNavActive(btnGradebook);
            gradebookPanel.setInstructorContext(currentInstructorId, currentTerm);
            showCard("gradebook");
        });
  
        btnNotifications.addActionListener(e -> {
            setNavActive(btnNotifications);
            showCard("notifications");
        });

  

        // Start maintenance poller (runs on EDT but cheap)
        javax.swing.Timer maintenancePoller = new javax.swing.Timer(3000, e -> updateMaintenanceState());
        maintenancePoller.setRepeats(true);
        maintenancePoller.setInitialDelay(0);
        maintenancePoller.start();

        // initial UI state


        // apply initial maintenance state immediately
        updateMaintenanceState();
    }

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

    private void showCard(String name) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
    }

    private JComponent wrapInPadding(JComponent c) {
        RoundedPanel p = new RoundedPanel(Theme.BORDER_RADIUS);
        p.setLayout(new BorderLayout());
        p.setBackground(Theme.SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(
                Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private JPanel createPlaceholderPanel(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        JLabel lbl = new JLabel("<html><center>" + text + "</center></html>", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        panel.add(lbl, BorderLayout.CENTER);
        return panel;
    }

    // Update banner + gradebook read-only behavior + change-password enable/disable
    public void updateMaintenanceState() {
        boolean maintenance = DBConnection.isMaintenanceMode();

        // show/hide the global banner (top-level InstructorPanel banner)
        maintenanceBanner.setVisible(maintenance);

        // Block change-password when maintenance is ON
        try {
            if (changePasswordBtn != null) {
                changePasswordBtn.setEnabled(!maintenance);
                changePasswordBtn.setToolTipText(maintenance
                        ? "Disabled during maintenance"
                        : "Change your account password");
            }
        } catch (Throwable ignored) {}

        // Do NOT disable Gradebook nav — we let it be viewable; gradebook enforces read-only
        try {
            if (maintenance) {
                gradebookPanel.setEditable(false); // enforce read-only in gradebook
            } else {
                gradebookPanel.refreshForMaintenance(); // allow gradebook to restore its editable state
            }
        } catch (Throwable ignored) {}

        // repaint UI
        SwingUtilities.invokeLater(() -> {
            maintenanceBanner.revalidate();
            maintenanceBanner.repaint();
        });
    }

    /**
     * Set context after login. This is the single authoritative method panel consumers should call.
     *
     * @param instructorId DB instructor_id
     * @param term         optional term (may be null)
     * @param username     display username to show in dashboard/header
     */
    public void setInstructorContext(long instructorId, String term, String username) {
        this.currentInstructorId = instructorId;
        this.currentTerm = term;
        this.instructorUsername = (username == null || username.isBlank()) ? "Instructor" : username;
        // update header label
        welcomeLabel.setText("Welcome, " + this.instructorUsername);

        // propagate to children
        try {
        } catch (Throwable ignored) {}
        try {
            gradebookPanel.setInstructorContext(instructorId, term);
        } catch (Throwable ignored) {}
        try {
            coursesPanel.loadForInstructor(instructorId, term);
        } catch (Throwable ignored) {}
        try {
            if (notificationsPanel instanceof NotificationPanel) {
                ((NotificationPanel) notificationsPanel).setInstructorContext(instructorId, term);
            }
        } catch (Exception ignored) {}

        // ensure maintenance applied now that context changed
        updateMaintenanceState();
    }

    public void setInstructorContext(long instructorId, String username) {
        setInstructorContext(instructorId, null, username);
    }

    // used by MainFrame/login to provide auth username for change-password
    public void setAuthUsername(String username) {
        this.instructorAuthUsername = username;
    }

    private void showChangePasswordDialog() {
        // guard against maintenance mode
        if (DBConnection.isMaintenanceMode()) {
            JOptionPane.showMessageDialog(this,
                    "Password changes are not allowed while the system is under maintenance.",
                    "Maintenance Active", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (instructorAuthUsername == null) {
            JOptionPane.showMessageDialog(this,
                    "Auth user not set. Contact admin.");
            return;
        }

        JPasswordField oldPass = new JPasswordField();
        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();

        Object[] form = {
                "Current Password:", oldPass,
                "New Password:", newPass,
                "Confirm New Password:", confirmPass
        };

        int ok = JOptionPane.showConfirmDialog(
                this, form, "Change Password", JOptionPane.OK_CANCEL_OPTION);

        if (ok != JOptionPane.OK_OPTION) return;

        String oldP = new String(oldPass.getPassword());
        String newP = new String(newPass.getPassword());
        String confP = new String(confirmPass.getPassword());

        if (!newP.equals(confP)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.");
            return;
        }

        boolean success = AuthService.changePassword(instructorAuthUsername, oldP, newP);

        if (success)
            JOptionPane.showMessageDialog(this, "Password changed successfully!");
        else
            JOptionPane.showMessageDialog(this, "Old password incorrect.");
    }
}
