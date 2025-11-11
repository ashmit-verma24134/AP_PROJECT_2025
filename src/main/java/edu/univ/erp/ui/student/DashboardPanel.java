package edu.univ.erp.ui.student;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.ui.Theme;

public class DashboardPanel extends JPanel {

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // Sidebar (fixed)
        //add(createSidebar(), BorderLayout.WEST);

        // Scrollable main area
        JScrollPane scrollPane = new JScrollPane(createDashboardContent());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ---------------- SIDEBAR ---------------- //
    /*private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(Theme.SIDEBAR_BG);

        // Logo section
        JLabel logo = new JLabel("<html><center><h2 style='color:#2FB6AD;'>IIITD</h2><small style='color:#FFFFFF99;'>Student Portal</small></center></html>", SwingConstants.CENTER);
        logo.setBorder(new EmptyBorder(25, 0, 25, 0));
        sidebar.add(logo, BorderLayout.NORTH);

        /*  Sidebar buttons
        String[] menuItems = {"Dashboard", "Course Catalog", "Timetable", "My Grades", "My Courses", "My Finances", "Transcript"};
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(0, 1, 0, 5));
        menuPanel.setBackground(Theme.SIDEBAR_BG);

        for (String item : menuItems) {
            JButton btn = new JButton(item);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(true);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 0));
            btn.setFont(Theme.BODY_FONT);
            btn.setBackground(Theme.SIDEBAR_BG);
            btn.setForeground(Color.WHITE);

            btn.addChangeListener(e -> {
                if (btn.getModel().isRollover())
                    btn.setBackground(Theme.PRIMARY_DARK);
                else
                    btn.setBackground(Theme.SIDEBAR_BG);
            });

            menuPanel.add(btn);
        }

        sidebar.add(menuPanel, BorderLayout.CENTER);
        */

       /*  // Footer user info
        JLabel user = new JLabel("<html><center><b style='color:white;'>John Smith</b><br><small style='color:#CCCCCC;'>2021CS101</small></center></html>", SwingConstants.CENTER);
        user.setBorder(new EmptyBorder(20, 0, 20, 0));
        sidebar.add(user, BorderLayout.SOUTH);

        return sidebar;
    }
    */

    // ---------------- MAIN CONTENT ---------------- //
    private JPanel createDashboardContent() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(Theme.BACKGROUND);
        main.setBorder(new EmptyBorder(25, 30, 30, 30));

        // Header
        JLabel heading = new JLabel("<html><h1>Dashboard</h1><p>Welcome back, John! Here's your academic overview.</p></html>");
        heading.setFont(Theme.HEADER_FONT);
        heading.setForeground(Theme.NEUTRAL_DARK);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(heading);
        main.add(Box.createVerticalStrut(20));

        // Info cards
        JPanel infoRow = new JPanel(new GridLayout(1, 4, 20, 10));
        infoRow.setOpaque(false);
        infoRow.add(createInfoCard("Current CGPA", "8.76", "+0.12 from last semester", Theme.SUCCESS));
        infoRow.add(createInfoCard("Enrolled Courses", "6", "24 credits this semester", Theme.NEUTRAL_MED));
        infoRow.add(createInfoCard("Attendance", "94%", "Above 75% requirement", Theme.SUCCESS));
        infoRow.add(createInfoCard("Pending Fees", "₹0", "All paid up", Theme.SUCCESS));
        main.add(infoRow);
        main.add(Box.createVerticalStrut(25));

        // Schedule + Grades row
        JPanel middle = new JPanel(new GridLayout(1, 2, 20, 10));
        middle.setOpaque(false);
        middle.add(createScheduleCard());
        middle.add(createGradesCard());
        main.add(middle);
        main.add(Box.createVerticalStrut(25));

        // Announcements section
        main.add(createAnnouncementsCard());

        // Add some bottom space for scrolling
        main.add(Box.createVerticalStrut(50));

        return main;
    }

    private JPanel createInfoCard(String title, String value, String subtitle, Color subColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.NEUTRAL_LIGHT, 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.BODY_FONT);
        lblTitle.setForeground(Theme.NEUTRAL_MED);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(Theme.NEUTRAL_DARK);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setForeground(subColor);
        lblSub.setFont(Theme.BODY_FONT);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        card.add(lblSub, BorderLayout.SOUTH);
        return card;
    }

    // Section Cards
    private JPanel createSectionCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.NEUTRAL_LIGHT, 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));
        JLabel lbl = new JLabel("<html><h3>" + title + "</h3></html>");
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setForeground(Theme.NEUTRAL_DARK);
        card.add(lbl);
        card.add(Box.createVerticalStrut(10));
        return card;
    }

    private JPanel createScheduleCard() {
        JPanel card = createSectionCard("Today's Schedule");
        card.add(createListItem("Data Structures", "Room C-101", "10:00 AM"));
        card.add(createListItem("Database Systems", "Room C-204", "2:00 PM"));
        card.add(createListItem("Computer Networks", "Room C-105", "4:00 PM"));
        return card;
    }

    private JPanel createGradesCard() {
        JPanel card = createSectionCard("Recent Grades");
        card.add(createGradeItem("Algorithms", "4 Credits", "A"));
        card.add(createGradeItem("Operating Systems", "4 Credits", "A-"));
        card.add(createGradeItem("Software Engineering", "3 Credits", "B+"));
        return card;
    }

    private JPanel createAnnouncementsCard() {
        JPanel card = createSectionCard("Announcements");
        card.add(createAnnouncement("Mid-term Exam Schedule Released", "2 days ago", true));
        card.add(createAnnouncement("Guest Lecture on AI/ML", "3 days ago", false));
        card.add(createAnnouncement("Library Hours Extended", "5 days ago", false));
        return card;
    }

    // ---------------- LIST ITEMS ---------------- //
    private JPanel createListItem(String course, String room, String time) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Theme.PRIMARY_LIGHT);
        item.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblCourse = new JLabel("<html><b>" + course + "</b><br><small>" + room + "</small></html>");
        lblCourse.setForeground(Theme.NEUTRAL_DARK);

        JLabel lblTime = new JLabel("<html><b>" + time + "</b><br><small>upcoming</small></html>");
        lblTime.setForeground(Theme.PRIMARY);

        item.add(lblCourse, BorderLayout.WEST);
        item.add(lblTime, BorderLayout.EAST);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        return item;
    }

    private JPanel createGradeItem(String course, String credits, String grade) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Theme.PRIMARY_LIGHT);
        item.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblCourse = new JLabel("<html><b>" + course + "</b><br><small>" + credits + "</small></html>");
        lblCourse.setForeground(Theme.NEUTRAL_DARK);

        JLabel lblGrade = new JLabel("<html><h3 style='color:#2FB6AD;'>" + grade + "</h3></html>");
        item.add(lblCourse, BorderLayout.WEST);
        item.add(lblGrade, BorderLayout.EAST);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        return item;
    }

    private JPanel createAnnouncement(String title, String time, boolean important) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Theme.PRIMARY_LIGHT);
        item.setBorder(new EmptyBorder(10, 15, 10, 15));

        String text = "<html><b>" + title + "</b><br><small>" + time + "</small></html>";
        if (important)
            text += "  <span style='color:white; background-color:#E74C3C; padding:2px 6px; border-radius:3px;'>Important</span>";

        JLabel lbl = new JLabel(text);
        lbl.setForeground(Theme.NEUTRAL_DARK);
        item.add(lbl);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        return item;
    }
}
