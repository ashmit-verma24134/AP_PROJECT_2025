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
        setBackground(Theme.BACKGROUND);

        // === HEADER BAR ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.PRIMARY);
        headerPanel.setPreferredSize(new Dimension(100, 70));

        JLabel welcomeLabel = new JLabel("👋 Welcome, Rahul Kumar!");
        welcomeLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 15));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // === MAIN CONTENT (SCROLLABLE) ===
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Theme.BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // === STAT CARDS ===
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 15));
        statsPanel.setBackground(Theme.BACKGROUND);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));

        statsPanel.add(createStatCard("🎓 Registered Credits", "20", new Color(56, 142, 60)));
        statsPanel.add(createStatCard("📈 Current CGPA", "9.15", new Color(25, 118, 210)));
        statsPanel.add(createStatCard("📚 Courses This Term", "5", new Color(255, 152, 0)));
        statsPanel.add(createStatCard("⭐ Credits Earned", "88", new Color(233, 30, 99)));

        contentPanel.add(statsPanel);

        // === GRADE DISTRIBUTION CHART ===
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("A+ (10)", 33);
        dataset.setValue("A (9)", 25);
        dataset.setValue("B+ (8)", 17);
        dataset.setValue("B (7)", 13);
        dataset.setValue("C+ (6)", 8);
        dataset.setValue("C (5)", 4);

        JFreeChart pieChart = ChartFactory.createPieChart(
                "Grade Distribution (Cumulative)",
                dataset,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(700, 300));
        chartPanel.setBackground(Color.WHITE);

        RoundedPanel chartContainer = new RoundedPanel(25);
        chartContainer.setBackground(Theme.CARD_BG);
        chartContainer.setLayout(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        chartContainer.add(chartPanel, BorderLayout.CENTER);

        JLabel chartLabel = new JLabel("📊 Grade Distribution (Cumulative)");
        chartLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        chartLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        chartContainer.add(chartLabel, BorderLayout.NORTH);

        JPanel chartWrap = new JPanel(new BorderLayout());
        chartWrap.setBackground(Theme.BACKGROUND);
        chartWrap.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        chartWrap.add(chartContainer, BorderLayout.CENTER);
        contentPanel.add(chartWrap);

        // === ANNOUNCEMENTS SECTION ===
        JLabel announceHeader = new JLabel("📢 Important Announcements");
        announceHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        announceHeader.setBorder(BorderFactory.createEmptyBorder(15, 25, 5, 10));
        contentPanel.add(announceHeader);

        JPanel announcePanel = new JPanel();
        announcePanel.setLayout(new BoxLayout(announcePanel, BoxLayout.Y_AXIS));
        announcePanel.setBackground(Theme.BACKGROUND);
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

        RoundedPanel announceContainer = new RoundedPanel(25);
        announceContainer.setBackground(Theme.CARD_BG);
        announceContainer.setLayout(new BorderLayout());
        announceContainer.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        announceContainer.add(announcePanel, BorderLayout.CENTER);
        contentPanel.add(announceContainer);
    }

    /** Creates a rounded stat card with icon, title, and value */
    private JPanel createStatCard(String title, String value, Color color) {
        RoundedPanel card = new RoundedPanel(25);
        card.setBackground(Theme.CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel iconLabel = new JLabel(title.split(" ")[0]); // Emoji part
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setForeground(color);

        JLabel titleLabel = new JLabel(title.substring(title.indexOf(" ") + 1));
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(90, 90, 90));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(color);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        textPanel.setBackground(Theme.CARD_BG);
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);

        card.add(iconLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    /** Creates clean announcement cards */
    private JPanel createAnnouncementCard(String title, String desc, String tag, String date) {
        RoundedPanel card = new RoundedPanel(15);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));

        JLabel tagLabel = new JLabel(tag);
        tagLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tagLabel.setOpaque(true);
        tagLabel.setForeground(Color.WHITE);
        tagLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        if (tag.equalsIgnoreCase("Urgent"))
            tagLabel.setBackground(new Color(220, 53, 69));
        else
            tagLabel.setBackground(new Color(33, 150, 243));

        header.add(titleLabel);
        header.add(tagLabel);

        JLabel descLabel = new JLabel("<html><p style='width:550px;'>" + desc + "</p></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JLabel dateLabel = new JLabel(date);
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        dateLabel.setForeground(new Color(120, 120, 120));

        card.add(header, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);
        card.add(dateLabel, BorderLayout.SOUTH);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        return card;
    }

    public void setActionsEnabled(boolean enabled) {
        // Optional: disable interactions if maintenance mode
    }

    public void loadData(String studentId) {
        System.out.println("Loading dashboard data for student: " + studentId);
    }
}
