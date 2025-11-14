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
 * InstructorPanel — clean sidebar + main content using CardLayout.
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
    private final JPanel gradebookPanel = new CourseDetailsPanel();
    private final JPanel timetablePanel = new InstructorTimetablePanel();
    private final JPanel announcementsPanel = createPlaceholderPanel("📢 Announcements - Post or view messages");
    private final JPanel profilePanel = createPlaceholderPanel("👤 Profile - Manage personal information");


    public InstructorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // === Header ===
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel title = new JLabel("Instructor Portal");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Color.WHITE);

        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setBackground(Theme.PRIMARY);
        headerWrap.add(title, BorderLayout.WEST);

        JButton logout = new JButton("Logout");
        logout.setBackground(Color.WHITE);
        logout.setForeground(Theme.PRIMARY);
        logout.setFocusPainted(false);
        logout.addActionListener(e -> mainFrame.showCard("login"));
        headerWrap.add(logout, BorderLayout.EAST);

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
        btnCourses.addActionListener(e -> { setNavActive(btnCourses); showCard("courses"); });
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
}
