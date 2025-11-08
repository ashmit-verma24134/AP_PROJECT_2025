package edu.univ.erp.ui.student;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.ui.RoundedPanel;

public class DashboardPanel extends JPanel {

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Top header bar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.PRIMARY);
        headerPanel.setPreferredSize(new Dimension(100, 70));

        JLabel welcomeLabel = new JLabel("👋 Welcome, Rahul Kumar!");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 15));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Main content area
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);

        // === Stats Row ===
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 15));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        statsPanel.add(createStatCard("Registered Credits", "20"));
        statsPanel.add(createStatCard("Current CGPA", "9.15"));
        statsPanel.add(createStatCard("Courses This Term", "5"));
        statsPanel.add(createStatCard("Credits Earned", "88"));

        contentPanel.add(statsPanel);

        // === Grade Distribution Chart ===
        JLabel chartLabel = new JLabel("Grade Distribution (Cumulative)");
        chartLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chartLabel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 10));

        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("A+ (10): 33%", 33);
        dataset.setValue("A (9): 25%", 25);
        dataset.setValue("B+ (8): 17%", 17);
        dataset.setValue("B (7): 13%", 13);
        dataset.setValue("C+ (6): 8%", 8);
        dataset.setValue("C (5): 4%", 4);

        JFreeChart pieChart = ChartFactory.createPieChart("", dataset, false, true, false);
        ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(600, 300));
        chartPanel.setBackground(Color.WHITE);

        RoundedPanel chartContainer = new RoundedPanel(25, Theme.CARD_BG);
        chartContainer.setLayout(new BorderLayout());
        chartContainer.add(chartLabel, BorderLayout.NORTH);
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        chartContainer.setBorder(BorderFactory.createEmptyBorder(15, 25, 25, 25));

        contentPanel.add(chartContainer);

        // === Announcements Section ===
        JLabel announceLabel = new JLabel("Important Announcements 📢");
        announceLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        announceLabel.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 10));
        contentPanel.add(announceLabel);

        JPanel announcePanel = new JPanel();
        announcePanel.setLayout(new BoxLayout(announcePanel, BoxLayout.Y_AXIS));
        announcePanel.setBackground(Color.WHITE);
        announcePanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 25, 25));

        Map<String, String[]> announcements = new LinkedHashMap<>();
        announcements.put("End-Sem Dates Tentative", new String[]{
                "The tentative schedule for End-Semester Examinations for the Monsoon 2025 term has been released.",
                "Urgent", "Nov 10, 2025"});
        announcements.put("Fee Payment Reminder", new String[]{
                "The last date for paying the hostel and mess fees is Nov 15, 2025.",
                "Urgent", "Nov 8, 2025"});
        announcements.put("Faculty Office Hours Update", new String[]{
                "Dr. Anjali's office hours have changed starting next week.",
                "Info", "Nov 5, 2025"});
        announcements.put("Library Extended Hours", new String[]{
                "The library will now remain open until midnight during the examination period.",
                "Info", "Nov 3, 2025"});

        for (Map.Entry<String, String[]> entry : announcements.entrySet()) {
            announcePanel.add(createAnnouncementCard(
                    entry.getKey(),
                    entry.getValue()[0],
                    entry.getValue()[1],
                    entry.getValue()[2]
            ));
        }

        contentPanel.add(announcePanel);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, String value) {
        RoundedPanel card = new RoundedPanel(20, Theme.CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(90, 90, 90));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(Color.BLACK);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAnnouncementCard(String title, String desc, String tag, String date) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title + "  ");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel tagLabel = new JLabel(tag);
        tagLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tagLabel.setOpaque(true);
        tagLabel.setForeground(Color.WHITE);
        tagLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        if (tag.equalsIgnoreCase("Urgent"))
            tagLabel.setBackground(new Color(220, 53, 69));
        else
            tagLabel.setBackground(new Color(0, 123, 255));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(titleLabel);
        headerPanel.add(tagLabel);

        JLabel descLabel = new JLabel("<html><p style='width:500px;'>" + desc + "</p></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel dateLabel = new JLabel(date);
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        dateLabel.setForeground(new Color(120, 120, 120));

        textPanel.add(headerPanel);
        textPanel.add(descLabel);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(dateLabel, BorderLayout.EAST);

        return card;
    }

    public void setActionsEnabled(boolean enabled) {
    // If your dashboard has buttons or interactive controls, disable them here
    // For example:
    // myButton.setEnabled(enabled);
    // myChartPanel.setEnabled(enabled);

    // If it’s mostly static content, you can leave it empty for now:
}

public void loadData(String studentId) {
    // This method will be called whenever a student logs in.
    // You can use the studentId to load personalized dashboard data, e.g.:
    //
    // - Show total registered courses
    // - Display outstanding fees
    // - Show GPA or upcoming deadlines
    //
    // For now, we can just store it or print it for debugging.

    System.out.println("Loading dashboard data for student: " + studentId);

    // TODO: Replace this with actual logic to fetch dashboard stats from DB.
    // Example:
    // StudentService svc = new StudentService();
    // Map<String, Object> dashboardData = svc.getDashboardData(studentId);
    // updateDashboardUI(dashboardData);
}

}
