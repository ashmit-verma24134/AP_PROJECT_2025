package edu.univ.erp.ui.Instructor;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.service.CourseService;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.GradeService;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;


import javax.swing.*;
import java.awt.*;

/**
 * InstructorPanel — SERVICE-INJECTED VERSION
 * Uses InstructorService + CourseService + SectionService + GradeService
 * No direct DB calls except maintenance-mode checks.
 */
public class InstructorPanel extends JPanel {

    // ---------- SERVICES ----------
    private final InstructorService instructorService;
    private final CourseService courseService;
    private final SectionService sectionService;
    private final EnrollmentService enrollmentService;
    private final GradeService gradeService;

    private final MainFrame mainFrame;

    // UI regions
    private final JPanel navPanel = new JPanel(null);
    private final JPanel navButtonsContainer = new JPanel();
    private final JPanel cards = new JPanel(new CardLayout());

    // maintenance banner
    private final JPanel maintenanceBanner = new JPanel(new BorderLayout());

    // nav buttons
    private JButton btnGradebook;

    // CHILD PANELS (now service-based)
    private final MyCoursesPanel coursesPanel;
    private final InstructorGradebookPanel gradebookPanel;
    private final JPanel notificationsPanel = new NotificationPanel();

    // context
    private long currentInstructorId = 0L;
    private String currentTerm = null;
    private String instructorUsername = "Instructor";
    private String instructorAuthUsername = null;

    // header label
    private final JLabel welcomeLabel = new JLabel("Welcome, Instructor");
    private JButton changePasswordBtn;


    // ------------------------------------------------------------
    // CONSTRUCTOR — ALL SERVICES INJECTED
    // ------------------------------------------------------------
    public InstructorPanel(MainFrame mainFrame,
                           InstructorService instructorService,
                           CourseService courseService,
                           SectionService sectionService,
                           EnrollmentService enrollmentService,
                           GradeService gradeService) {

        this.mainFrame = mainFrame;

        this.instructorService = instructorService;
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.enrollmentService = enrollmentService;
        this.gradeService = gradeService;

        // instantiate service-based children
        this.coursesPanel = new MyCoursesPanel(enrollmentService, courseService, sectionService);
        this.gradebookPanel = new InstructorGradebookPanel(gradeService, sectionService);

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        buildUI();

        javax.swing.Timer maintenancePoller = new javax.swing.Timer(3000, e -> updateMaintenanceState());
        maintenancePoller.setRepeats(true);
        maintenancePoller.setInitialDelay(0);
        maintenancePoller.start();

        updateMaintenanceState();
    }

    

    // ------------------------------------------------------------
    // BUILD UI EXACTLY AS BEFORE (UNCHANGED VISUALLY)
    // ------------------------------------------------------------
    private void buildUI() {

        // ----- HEADER -----
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(Theme.PRIMARY);
        headerWrap.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        welcomeLabel.setFont(Theme.TITLE_FONT);
        welcomeLabel.setForeground(Color.WHITE);
        headerWrap.add(welcomeLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

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

        // banner
        JPanel headerAndBanner = new JPanel(new BorderLayout());
        headerAndBanner.add(headerWrap, BorderLayout.NORTH);

        maintenanceBanner.setBackground(new Color(254, 246, 243));
        maintenanceBanner.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel mLabel = new JLabel(
                "<html>&#x26A0;&nbsp;<b>Site is in Maintenance Mode — contents are view-only.</b></html>",
                SwingConstants.CENTER);
        mLabel.setForeground(new Color(117, 40, 24));
        mLabel.setFont(Theme.BODY_BOLD);
        maintenanceBanner.add(mLabel, BorderLayout.CENTER);
        maintenanceBanner.setVisible(false);
        headerAndBanner.add(maintenanceBanner, BorderLayout.SOUTH);

        add(headerAndBanner, BorderLayout.NORTH);


        // ----- SIDEBAR -----
        navPanel.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        navPanel.setBackground(Theme.SIDEBAR_BG);

        navButtonsContainer.setLayout(new BoxLayout(navButtonsContainer, BoxLayout.Y_AXIS));
        navButtonsContainer.setOpaque(false);
        navButtonsContainer.setBounds(0, 16, Theme.SIDEBAR_WIDTH, 600);

        JButton btnCourses = makeNavButton("My Courses");
        btnGradebook = makeNavButton("Gradebook");
        JButton btnNotifications = makeNavButton("Notifications");

        navButtonsContainer.add(Box.createVerticalStrut(12));
        navButtonsContainer.add(btnCourses);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnGradebook);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnNotifications);
        navButtonsContainer.add(Box.createVerticalGlue());

        navPanel.add(navButtonsContainer);
        add(navPanel, BorderLayout.WEST);


        // ----- CARDS -----
        cards.setBackground(Theme.BACKGROUND);
        cards.add(wrap(coursesPanel), "courses");
        cards.add(wrap(gradebookPanel), "gradebook");
        cards.add(wrap(notificationsPanel), "notifications");
        add(cards, BorderLayout.CENTER);


        // ----- NAV ACTIONS -----

        btnCourses.addActionListener(e -> {
            setNavActive(btnCourses);
            if (currentInstructorId > 0)
                coursesPanel.loadForInstructor(currentInstructorId, currentTerm);
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
    }

    // Helpers ----------------------------------------------------

    private JButton makeNavButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_ITEM_HEIGHT));
        b.setFont(Theme.BODY_FONT);
        b.setForeground(Color.WHITE);
        b.setBackground(Theme.SIDEBAR_BG);
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
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
        ((CardLayout) cards.getLayout()).show(cards, name);
    }

    private JComponent wrap(JComponent c) {
        RoundedPanel p = new RoundedPanel(Theme.BORDER_RADIUS);
        p.setLayout(new BorderLayout());
        p.setBackground(Theme.SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(
                Theme.CARD_PADDING, Theme.CARD_PADDING,
                Theme.CARD_PADDING, Theme.CARD_PADDING));
        p.add(c, BorderLayout.CENTER);
        return p;
    }


    // ------------------------------------------------------------
    // MAINTENANCE STATE
    // ------------------------------------------------------------
    public void updateMaintenanceState() {
        boolean maintenance = DBConnection.isMaintenanceMode();

        maintenanceBanner.setVisible(maintenance);

        if (changePasswordBtn != null) {
            changePasswordBtn.setEnabled(!maintenance);
            changePasswordBtn.setToolTipText(
                    maintenance ? "Disabled during maintenance" : "Change your password");
        }

        if (maintenance) gradebookPanel.setEditable(false);
        else gradebookPanel.refreshForMaintenance();

        SwingUtilities.invokeLater(() -> {
            maintenanceBanner.revalidate();
            maintenanceBanner.repaint();
        });
    }


    // ------------------------------------------------------------
    // CONTEXT SETTER
    // ------------------------------------------------------------
    public void setInstructorContext(long instructorId, String term, String username) {
        this.currentInstructorId = instructorId;
        this.currentTerm = term;
        this.instructorUsername = (username == null || username.isBlank())
                ? "Instructor"
                : username;

        welcomeLabel.setText("Welcome, " + instructorUsername);

        try {
            gradebookPanel.setInstructorContext(instructorId, term);
        } catch (Throwable ignored) {}

        try {
            coursesPanel.loadForInstructor(instructorId, term);
        } catch (Throwable ignored) {}

        if (notificationsPanel instanceof NotificationPanel) {
            ((NotificationPanel) notificationsPanel).setInstructorContext(instructorId, term);
        }

        updateMaintenanceState();
    }

    public void setInstructorContext(long instructorId, String username) {
        setInstructorContext(instructorId, null, username);
    }


    // ------------------------------------------------------------
    // CHANGE PASSWORD
    // ------------------------------------------------------------
    private void showChangePasswordDialog() {
        if (DBConnection.isMaintenanceMode()) {
            JOptionPane.showMessageDialog(this,
                    "Password changes are not allowed during maintenance.",
                    "Maintenance Active",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (instructorAuthUsername == null) {
            JOptionPane.showMessageDialog(this, "Auth user not set.");
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

        JOptionPane.showMessageDialog(this,
                success ? "Password changed successfully!" : "Old password incorrect.");
    }

    public void setAuthUsername(String username) {
        this.instructorAuthUsername = username;
    }
}
