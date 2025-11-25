package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;

/**
 * InstructorPanel – fixed and cleaned version.
 * - exposes setInstructorContext(instructorId, term, username)
 * - uses DashboardPanel and InstructorGradebookPanel with the new context calls
 * - keeps previous navigation behavior
 */
public class InstructorPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JPanel navPanel = new JPanel(null);
    private final JPanel navButtonsContainer = new JPanel();
    private final JPanel cards = new JPanel(new CardLayout());

    // Panels (existing classes)
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final MyCoursesPanel coursesPanel = new MyCoursesPanel(); // instructor-side MyCoursesPanel
    private final InstructorGradebookPanel gradebookPanel = new InstructorGradebookPanel();
    private final JPanel timetablePanel = new InstructorTimetablePanel();
    private final JPanel announcementsPanel = createPlaceholderPanel("📢 Announcements - Post or view messages");
    private final JPanel profilePanel = createPlaceholderPanel("👤 Profile - Manage personal information");

    // context
    private long currentInstructorId = 0L;
    private String currentTerm = null;
    private String instructorUsername = "Instructor";

    public InstructorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // header
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(Theme.PRIMARY);
        headerWrap.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel welcomeLabel = new JLabel("Welcome, Instructor");
        welcomeLabel.setFont(Theme.TITLE_FONT);
        welcomeLabel.setForeground(Color.WHITE);
        headerWrap.add(welcomeLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);
        JButton changePassword = new JButton("Change Password");
        changePassword.setBackground(new Color(255, 255, 255, 180));
        changePassword.setForeground(Theme.PRIMARY);
        changePassword.setFocusPainted(false);
        changePassword.addActionListener(e -> {
            // delegate to dashboardPanel's dialog or AuthService.changePassword(...) integration
            JOptionPane.showMessageDialog(this, "Use Change Password in header (not implemented here).");
        });
        JButton logout = new JButton("Logout");
        logout.setBackground(Color.WHITE);
        logout.setForeground(Theme.PRIMARY);
        logout.setFocusPainted(false);
        logout.addActionListener(e -> mainFrame.showCard("login"));
        rightPanel.add(changePassword);
        rightPanel.add(logout);
        headerWrap.add(rightPanel, BorderLayout.EAST);
        add(headerWrap, BorderLayout.NORTH);

        // sidebar
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

        // cards
        cards.setBackground(Theme.BACKGROUND);

        // wrap existing panels consistently
        cards.add(wrapInPadding(dashboardPanel), "dashboard");
        cards.add(wrapInPadding(coursesPanel), "courses");
        cards.add(wrapInPadding(gradebookPanel), "gradebook");
        cards.add(wrapInPadding(timetablePanel), "timetable");
        cards.add(wrapInPadding(announcementsPanel), "announcements");
        cards.add(wrapInPadding(profilePanel), "profile");

        add(cards, BorderLayout.CENTER);

        // navigation actions
        btnDashboard.addActionListener(e -> { setNavActive(btnDashboard); showCard("dashboard"); });
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
        btnTimetable.addActionListener(e -> { setNavActive(btnTimetable); showCard("timetable");});
        btnAnnouncements.addActionListener(e -> { setNavActive(btnAnnouncements); showCard("announcements");});
        btnProfile.addActionListener(e -> { setNavActive(btnProfile); showCard("profile");});

        SwingUtilities.invokeLater(() -> {
            setNavActive(btnDashboard);
            showCard("dashboard");
        });
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

        // update dashboard welcome text and load dashboard content
        dashboardPanel.setInstructorContext(instructorId, this.instructorUsername);

        // also tell gradebook & courses panels
        gradebookPanel.setInstructorContext(instructorId, term);
        coursesPanel.loadForInstructor(instructorId, term);
    }

    public void setInstructorContext(long instructorId, String username) {
        setInstructorContext(instructorId, null, username);
    }
}
