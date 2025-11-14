package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.Theme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardPanel extends JPanel {

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ===== Header Bar =====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel welcome = new JLabel("👋 Welcome back, Dr. Gupta");
        welcome.setFont(Theme.HEADER_FONT);
        welcome.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Here’s what’s happening with your courses today.");
        subtitle.setFont(Theme.BODY_FONT);
        subtitle.setForeground(Color.WHITE);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(Theme.PRIMARY);
        titlePanel.add(welcome);
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ===== Scrollable Content =====
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(new EmptyBorder(20, 30, 30, 30));

        // --- Metrics Row ---
        content.add(createStatsRow());
        content.add(Box.createVerticalStrut(25));

        // --- Courses & Upcoming Classes ---
        JPanel middlePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        middlePanel.setBackground(Theme.BACKGROUND);
        middlePanel.add(createMyCoursesPanel());
        middlePanel.add(createUpcomingClassesPanel());
        content.add(middlePanel);

        content.add(Box.createVerticalStrut(25));

        // --- Performance Chart + Activity Feed ---
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomPanel.setBackground(Theme.BACKGROUND);
        bottomPanel.add(createPerformanceChartPanel());
        bottomPanel.add(createRecentActivityPanel());
        content.add(bottomPanel);

        add(new JScrollPane(content), BorderLayout.CENTER);
    }

    // ==========================================
    // Stats Section
    // ==========================================
    private JPanel createStatsRow() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 20, 0));
        panel.setBackground(Theme.BACKGROUND);

        panel.add(createStatCard("📘 Active Courses", "4", "+1 this semester"));
        panel.add(createStatCard("👥 Total Students", "312", "+12%"));
        panel.add(createStatCard("📝 Pending Reviews", "24", ""));
        panel.add(createStatCard("📊 Avg. Performance", "82%", "+5%"));

        return panel;
    }

    private JPanel createStatCard(String title, String value, String subtext) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.BODY_BOLD);
        titleLabel.setForeground(Theme.NEUTRAL_MED);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(Theme.NEUTRAL_DARK);

        JLabel subLabel = new JLabel(subtext);
        subLabel.setFont(Theme.BODY_FONT);
        subLabel.setForeground(Theme.SUCCESS);

        JPanel valuePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        valuePanel.setBackground(Theme.SURFACE);
        valuePanel.add(valueLabel);
        valuePanel.add(subLabel);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valuePanel, BorderLayout.CENTER);
        return card;
    }

    // ==========================================
    // My Courses Section
    // ==========================================
    private JPanel createMyCoursesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BACKGROUND);

        JLabel header = new JLabel("My Courses");
        header.setFont(Theme.TITLE_FONT);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(header);

        List<JPanel> courseCards = new ArrayList<>();
        courseCards.add(createCourseCard("Data Structures & Algorithms", "CSE201", 85, "Mon, Wed 10:00 AM", "8 assignments to grade"));
        courseCards.add(createCourseCard("Advanced Algorithms", "CSE401", 62, "Tue, Thu 2:00 PM", "6 assignments to grade"));
        courseCards.add(createCourseCard("Database Management Systems", "CSE301", 95, "Mon, Wed 2:00 PM", "10 assignments to grade"));
        courseCards.add(createCourseCard("Operating Systems", "CSE302", 70, "Tue, Thu 10:00 AM", "5 assignments to grade"));

        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setBackground(Theme.BACKGROUND);
        for (JPanel c : courseCards) grid.add(c);

        panel.add(grid);
        return panel;
    }

    private JPanel createCourseCard(String name, String code, int students, String schedule, String notice) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JLabel title = new JLabel("<html><b>" + name + "</b> <span style='color:gray;font-size:10pt'>&nbsp;(" + code + ")</span></html>");
        title.setFont(Theme.BODY_BOLD);
        JLabel subtitle = new JLabel("👥 " + students + " students • 🕒 " + schedule);
        subtitle.setFont(Theme.BODY_FONT);
        subtitle.setForeground(Theme.NEUTRAL_MED);

        JLabel assignments = new JLabel("📄 " + notice);
        assignments.setOpaque(true);
        assignments.setBackground(Theme.WARNING);
        assignments.setForeground(Color.DARK_GRAY);
        assignments.setBorder(new EmptyBorder(6, 8, 6, 8));

        JPanel info = new JPanel(new GridLayout(3, 1, 0, 4));
        info.setBackground(Theme.SURFACE);
        info.add(title);
        info.add(subtitle);
        info.add(assignments);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        btnPanel.setBackground(Theme.SURFACE);
        btnPanel.add(styledButton("View Course", Theme.PRIMARY));
        btnPanel.add(styledButton("Materials", Theme.SURFACE));

        card.add(info, BorderLayout.CENTER);
        card.add(btnPanel, BorderLayout.SOUTH);
        return card;
    }

    // ==========================================
    // Upcoming Classes
    // ==========================================
    private JPanel createUpcomingClassesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BACKGROUND);

        JLabel header = new JLabel("Upcoming Classes");
        header.setFont(Theme.TITLE_FONT);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(header);

        panel.add(createUpcomingClassCard("Data Structures", "Today", "10:00 - 11:30 AM", "LHC-101", "Topic: Binary Search Trees"));
        panel.add(createUpcomingClassCard("Database Systems", "Today", "2:00 - 3:30 PM", "LHC-203", "Topic: SQL Joins & Transactions"));
        panel.add(createUpcomingClassCard("Advanced Algorithms", "Tomorrow", "2:00 - 3:30 PM", "LHC-102", "Topic: Dynamic Programming"));

        return panel;
    }

    private JPanel createUpcomingClassCard(String name, String day, String time, String room, String topic) {
        JPanel card = new JPanel(new GridLayout(5, 1, 0, 2));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        card.add(new JLabel("<html><b>" + name + "</b></html>"));
        card.add(new JLabel("📅 " + day));
        card.add(new JLabel("🕒 " + time));
        card.add(new JLabel("📍 " + room));

        JLabel topicLabel = new JLabel(topic);
        topicLabel.setFont(Theme.BODY_FONT);
        topicLabel.setForeground(Theme.NEUTRAL_MED);
        card.add(topicLabel);
        return card;
    }

    // ==========================================
    // Chart Section
    // ==========================================
    private JPanel createPerformanceChartPanel() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(78, "Avg Score (%)", "DS");
        dataset.addValue(92, "Avg Score (%)", "Algo");
        dataset.addValue(85, "Avg Score (%)", "DBMS");
        dataset.addValue(70, "Avg Score (%)", "OS");

        dataset.addValue(90, "Submission Rate (%)", "DS");
        dataset.addValue(95, "Submission Rate (%)", "Algo");
        dataset.addValue(93, "Submission Rate (%)", "DBMS");
        dataset.addValue(80, "Submission Rate (%)", "OS");

        JFreeChart chart = ChartFactory.createBarChart(
                "Course Performance Overview", "", "Percentage (%)", dataset);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 350));
        chartPanel.setBackground(Theme.SURFACE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(chartPanel, BorderLayout.CENTER);
        return panel;
    }

    // ==========================================
    // Recent Activity Panel
    // ==========================================
    private JPanel createRecentActivityPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BACKGROUND);

        JLabel header = new JLabel("Recent Activity");
        header.setFont(Theme.TITLE_FONT);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(header);

        panel.add(activityItem("RS", "Rahul Sharma submitted assignment in Data Structures", "2 hours ago"));
        panel.add(activityItem("PP", "Priya Patel asked a question in Algorithms", "3 hours ago"));
        panel.add(activityItem("AK", "Amit Kumar submitted assignment in Database Systems", "5 hours ago"));
        panel.add(activityItem("SR", "Sneha Reddy attended office hours in Operating Systems", "1 day ago"));
        panel.add(activityItem("VS", "Vikram Singh submitted assignment in Data Structures", "1 day ago"));

        return panel;
    }

    private JPanel activityItem(String initials, String message, String time) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));
        card.setPreferredSize(new Dimension(280, 60));

        JLabel icon = new JLabel(initials, SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(Theme.PRIMARY_LIGHT);
        icon.setForeground(Theme.PRIMARY_DARK);
        icon.setFont(Theme.BODY_BOLD);
        icon.setPreferredSize(new Dimension(32, 32));

        JLabel msg = new JLabel("<html><b>" + message.split(" ")[0] + " " + message.split(" ")[1] + "</b> " +
                message.substring(message.indexOf(" ", message.indexOf(" ")+1)) + "</html>");
        msg.setFont(Theme.BODY_FONT);

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(Theme.BODY_FONT);
        timeLabel.setForeground(Theme.NEUTRAL_MED);

        JPanel text = new JPanel(new BorderLayout());
        text.setBackground(Theme.SURFACE);
        text.add(msg, BorderLayout.CENTER);
        text.add(timeLabel, BorderLayout.SOUTH);

        card.add(icon, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    // ==========================================
    // Button styling
    // ==========================================
    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.BODY_BOLD);
        btn.setBackground(bg);
        btn.setForeground(bg.equals(Theme.SURFACE) ? Theme.NEUTRAL_DARK : Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(Theme.darken(bg, 0.1f));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }
}
