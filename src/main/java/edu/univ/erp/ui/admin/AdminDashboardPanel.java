package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class AdminDashboardPanel extends JPanel {

    public AdminDashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ===== Header =====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Admin Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Theme.NEUTRAL_DARK);

        JLabel subtitle = new JLabel("System overview and management");
        subtitle.setFont(Theme.BODY_FONT);
        subtitle.setForeground(Theme.NEUTRAL_MED);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Theme.SURFACE);
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitle);

        header.add(textPanel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ===== Main Scrollable Content =====
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(new EmptyBorder(20, 30, 40, 30));

        // Top metrics cards
        content.add(createTopStatsPanel());
        content.add(Box.createVerticalStrut(25));

        // Department + Instructor activity
        JPanel deptAndActivity = new JPanel(new GridLayout(1, 2, 25, 0));
        deptAndActivity.setBackground(Theme.BACKGROUND);
        deptAndActivity.add(createDepartmentOverview());
        deptAndActivity.add(createInstructorActivity());
        content.add(deptAndActivity);

        content.add(Box.createVerticalStrut(25));

        // System + Semester sections
        JPanel systemAndStats = new JPanel(new GridLayout(1, 2, 25, 0));
        systemAndStats.setBackground(Theme.BACKGROUND);
        systemAndStats.add(createSystemStatus());
        systemAndStats.add(createSemesterStats());
        content.add(systemAndStats);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // ====== Top Summary Cards ======
    private JPanel createTopStatsPanel() {
        JPanel stats = new JPanel(new GridLayout(2, 3, 20, 20));
        stats.setBackground(Theme.BACKGROUND);

        stats.add(createStatCard("👩‍🏫 Total Instructors", "48", "+3 this month"));
        stats.add(createStatCard("🎓 Total Students", "1842", "+124 this semester"));
        stats.add(createStatCard("🏛️ Departments", "12", ""));
        stats.add(createStatCard("📚 Active Courses", "156", "+8 this semester"));
        stats.add(createStatCard("🖥️ Active Sessions", "89", ""));
        stats.add(createStatCard("⚠️ Pending Issues", "7", "Review required"));

        return stats;
    }

    private JPanel createStatCard(String title, String value, String subtext) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.BODY_BOLD);
        titleLbl.setForeground(Theme.NEUTRAL_MED);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLbl.setForeground(Theme.NEUTRAL_DARK);

        JLabel subLbl = new JLabel(subtext);
        subLbl.setFont(Theme.BODY_FONT);
        subLbl.setForeground(Theme.SUCCESS);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setBackground(Theme.SURFACE);
        top.add(titleLbl);
        top.add(valueLbl);

        card.add(top, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    // ===== Department Overview =====
    private JPanel createDepartmentOverview() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Department Overview");
        header.setFont(Theme.TITLE_FONT);
        header.setForeground(Theme.NEUTRAL_DARK);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(header);
        panel.add(Box.createVerticalStrut(10));

        List<String[]> departments = Arrays.asList(
                new String[]{"Computer Science & Engineering", "456 students • 12 instructors • 38 courses"},
                new String[]{"Electronics & Communication", "398 students • 10 instructors • 32 courses"},
                new String[]{"Mathematics", "245 students • 8 instructors • 24 courses"},
                new String[]{"Computational Biology", "178 students • 6 instructors • 18 courses"}
        );

        for (String[] dept : departments) {
            panel.add(createDepartmentCard(dept[0], dept[1]));
            panel.add(Box.createVerticalStrut(10));
        }

        return panel;
    }

    private JPanel createDepartmentCard(String name, String details) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.PRIMARY_LIGHT);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel nameLbl = new JLabel("<html><b>" + name + "</b></html>");
        nameLbl.setFont(Theme.BODY_BOLD);
        nameLbl.setForeground(Theme.NEUTRAL_DARK);

        JLabel detailsLbl = new JLabel(details);
        detailsLbl.setFont(Theme.BODY_FONT);
        detailsLbl.setForeground(Theme.NEUTRAL_MED);

        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setBackground(Theme.PRIMARY_LIGHT);
        text.add(nameLbl);
        text.add(detailsLbl);

        JLabel count = new JLabel("●");
        count.setForeground(Theme.PRIMARY_DARK);

        card.add(text, BorderLayout.CENTER);
        card.add(count, BorderLayout.EAST);

        return card;
    }

    // ===== Instructor Activity =====
    private JPanel createInstructorActivity() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Recent Instructor Activity");
        header.setFont(Theme.TITLE_FONT);
        header.setForeground(Theme.NEUTRAL_DARK);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(header);
        panel.add(Box.createVerticalStrut(15));

        List<String[]> activity = Arrays.asList(
                new String[]{"DRK", "Dr. Rajesh Kumar", "Created new course - Advanced AI", "2 hours ago"},
                new String[]{"PAS", "Prof. Anjali Sharma", "Updated syllabus - DBMS", "4 hours ago"},
                new String[]{"DVS", "Dr. Vikram Singh", "Graded assignments - Algorithms", "5 hours ago"},
                new String[]{"PPV", "Prof. Priya Verma", "Added new student - Data Science", "1 day ago"}
        );

        for (String[] act : activity) {
            panel.add(createActivityRow(act[0], act[1], act[2], act[3]));
            panel.add(Box.createVerticalStrut(10));
        }

        return panel;
    }

    private JPanel createActivityRow(String initials, String name, String desc, String time) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Theme.SURFACE);

        JLabel avatar = new JLabel(initials, SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(Theme.PRIMARY_LIGHT);
        avatar.setForeground(Theme.PRIMARY_DARK);
        avatar.setFont(Theme.BODY_BOLD);
        avatar.setPreferredSize(new Dimension(36, 36));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setBackground(Theme.SURFACE);
        textPanel.add(new JLabel("<html><b>" + name + "</b> " + desc + "</html>"));
        JLabel timeLbl = new JLabel(time);
        timeLbl.setFont(Theme.BODY_FONT);
        timeLbl.setForeground(Theme.NEUTRAL_MED);
        textPanel.add(timeLbl);

        row.add(avatar, BorderLayout.WEST);
        row.add(textPanel, BorderLayout.CENTER);
        return row;
    }

    // ===== System Status =====
    private JPanel createSystemStatus() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("System Status");
        header.setFont(Theme.TITLE_FONT);
        header.setForeground(Theme.NEUTRAL_DARK);
        panel.add(header);
        panel.add(Box.createVerticalStrut(15));

        panel.add(createStatusRow("Database Status", "Healthy", Theme.SUCCESS));
        panel.add(createStatusRow("Server Status", "Online", Theme.SUCCESS));
        panel.add(createStatusRow("Backup Status", "In Progress", Theme.WARNING));
        panel.add(createStatusRow("API Status", "Operational", Theme.SUCCESS));

        return panel;
    }

    private JPanel createStatusRow(String title, String status, Color badgeColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Theme.PRIMARY_LIGHT);
        row.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.BODY_BOLD);

        JLabel statusLbl = new JLabel(status);
        statusLbl.setOpaque(true);
        statusLbl.setBackground(Theme.withAlpha(badgeColor.getRGB(), 0.2f));
        statusLbl.setForeground(badgeColor.darker());
        statusLbl.setBorder(new EmptyBorder(4, 8, 4, 8));
        statusLbl.setFont(Theme.BODY_BOLD);

        row.add(titleLbl, BorderLayout.WEST);
        row.add(statusLbl, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(400, 36));

        return row;
    }

    // ===== Semester Stats =====
    private JPanel createSemesterStats() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Semester Statistics");
        header.setFont(Theme.TITLE_FONT);
        header.setForeground(Theme.NEUTRAL_DARK);
        panel.add(header);
        panel.add(Box.createVerticalStrut(15));

        panel.add(createProgressBarRow("Course Completion Rate", 87, Theme.SUCCESS));
        panel.add(createProgressBarRow("Average Attendance", 92, Theme.PRIMARY));
        panel.add(createProgressBarRow("Student Satisfaction", 84, Theme.INFO));
        panel.add(createProgressBarRow("Assignment Submission", 89, Theme.WARNING));

        return panel;
    }

    private JPanel createProgressBarRow(String title, int percent, Color color) {
        JPanel row = new JPanel();
        row.setLayout(new BorderLayout());
        row.setBackground(Theme.SURFACE);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));

        JLabel label = new JLabel(title);
        label.setFont(Theme.BODY_BOLD);
        label.setForeground(Theme.NEUTRAL_DARK);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percent);
        bar.setForeground(color);
        bar.setBackground(Theme.NEUTRAL_LIGHT);
        bar.setPreferredSize(new Dimension(200, 8));
        bar.setBorderPainted(false);

        JLabel value = new JLabel(percent + "%");
        value.setFont(Theme.BODY_FONT);
        value.setForeground(Theme.NEUTRAL_DARK);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.SURFACE);
        top.add(label, BorderLayout.WEST);
        top.add(value, BorderLayout.EAST);

        row.add(top, BorderLayout.NORTH);
        row.add(bar, BorderLayout.SOUTH);
        return row;
    }
}
