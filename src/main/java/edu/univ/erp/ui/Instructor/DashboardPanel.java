package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;

/**
 * DashboardPanel - dynamic content for instructor dashboard.
 * Public API: setInstructorContext(long instructorId, String username)
 */
public class DashboardPanel extends JPanel {

    private final JLabel lblWelcome;
    private JLabel lblActiveCourses;
    private JLabel lblTotalStudents;
    private final JPanel myCoursesPanel;
    private final NotificationPanel notificationPanel;

    private long currentInstructorId = 0L;
    private String currentUsername = "Instructor";

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // header with welcome
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(20, 24, 20, 24));

        lblWelcome = new JLabel("👋 Welcome back, Dr. Gupta");
        lblWelcome.setFont(Theme.HEADER_FONT);
        lblWelcome.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Here's what's happening with your courses today.");
        subtitle.setFont(Theme.BODY_FONT);
        subtitle.setForeground(Color.WHITE);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(Theme.PRIMARY);
        titlePanel.add(lblWelcome);
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(new EmptyBorder(20, 30, 30, 30));

        content.add(createStatsRow());
        content.add(Box.createVerticalStrut(25));

        JPanel middlePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        middlePanel.setBackground(Theme.BACKGROUND);

        myCoursesPanel = new JPanel();
        myCoursesPanel.setLayout(new BoxLayout(myCoursesPanel, BoxLayout.Y_AXIS));
        myCoursesPanel.setBackground(Theme.BACKGROUND);
        JPanel myCoursesWrapper = new JPanel(new BorderLayout());
        myCoursesWrapper.setBackground(Theme.BACKGROUND);
        myCoursesWrapper.add(createMyCoursesHeader(), BorderLayout.NORTH);
        myCoursesWrapper.add(new JScrollPane(myCoursesPanel), BorderLayout.CENTER);

        middlePanel.add(myCoursesWrapper);

        // RIGHT column = notifications + upcoming classes stacked
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setBackground(Theme.BACKGROUND);

        notificationPanel = new NotificationPanel();

        // Add notification panel at the top
        rightCol.add(notificationPanel);
        rightCol.add(Box.createVerticalStrut(20));

        // Existing upcoming classes panel (unchanged)
        rightCol.add(createUpcomingClassesPanel());

        // Add assembled right column
        middlePanel.add(rightCol);

        content.add(middlePanel);
        content.add(Box.createVerticalStrut(25));

        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomPanel.setBackground(Theme.BACKGROUND);
        //bottomPanel.add(createPerformanceChartPanel());
        bottomPanel.add(createDynamicStatsChartPanel());
        bottomPanel.add(createRecentActivityPanel());
        content.add(bottomPanel);

        add(new JScrollPane(content), BorderLayout.CENTER);

        // Initialize label references (will be set in createStatsRow)
        lblActiveCourses = new JLabel("—");
        lblActiveCourses.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTotalStudents = new JLabel("—");
        lblTotalStudents.setFont(new Font("Segoe UI", Font.BOLD, 24));
    }

    private JPanel createStatsRow() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 20, 0));
        panel.setBackground(Theme.BACKGROUND);

        JPanel c1 = createStatCard("📘 Active Courses", "0", "");
        JPanel c2 = createStatCard("👥 Total Students", "0", "");
        JPanel c3 = createStatCard("📝 Pending Reviews", "0", "");
        JPanel c4 = createStatCard("📊 Avg. Performance", "—", "");

        // extract the big labels for dynamic updates
        JLabel v1 = extractValueLabelFromStatCard(c1);
        JLabel v2 = extractValueLabelFromStatCard(c2);

        // store references
        lblActiveCourses = v1;
        lblTotalStudents = v2;

        panel.add(c1);
        panel.add(c2);
        panel.add(c3);
        panel.add(c4);
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

    private JLabel extractValueLabelFromStatCard(JPanel card) {
        Component center = ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (center instanceof JPanel) {
            JPanel vp = (JPanel) center;
            if (vp.getComponentCount() >= 1 && vp.getComponent(0) instanceof JLabel) {
                return (JLabel) vp.getComponent(0);
            }
        }
        JLabel fallback = new JLabel("—");
        fallback.setFont(new Font("Segoe UI", Font.BOLD, 24));
        return fallback;
    }

    private JPanel createMyCoursesHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BACKGROUND);
        JLabel header = new JLabel("My Courses");
        header.setFont(Theme.TITLE_FONT);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        p.add(header, BorderLayout.WEST);
        return p;
    }

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

    private JPanel createPerformanceChartPanel() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(78, "Avg Score (%)", "DS");
        dataset.addValue(92, "Avg Score (%)", "Algo");
        dataset.addValue(85, "Avg Score (%)", "DBMS");
        dataset.addValue(70, "Avg Score (%)", "OS");

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

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.BODY_BOLD);
        btn.setBackground(bg);
        btn.setForeground(bg.equals(Theme.SURFACE) ? Theme.NEUTRAL_DARK : Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Public API to set instructor context and trigger load.
     */
    public void setInstructorContext(long instructorId, String username) {
        this.currentInstructorId = instructorId;
        this.currentUsername = (username == null || username.isBlank()) ? "Instructor" : username;
        SwingUtilities.invokeLater(() -> lblWelcome.setText("👋 Welcome back, " + this.currentUsername));
        loadInstructorMetricsAsync(instructorId);
        SwingUtilities.invokeLater(() -> {
        removeAll();
        revalidate();
        repaint();
});

    }

    private void loadInstructorMetricsAsync(long instructorId) {
        SwingWorker<Map<String,Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Map<String,Object> out = new HashMap<>();
                int activeCourses = 0;
                int totalStudents = 0;
                List<Map<String,Object>> sections = new ArrayList<>();

                String secSql = """
                    SELECT s.section_id, c.code AS course_code,
                           c.title AS course_title,
                           (SELECT COUNT(*) FROM enrollments e
                                WHERE e.section_id = s.section_id AND e.status='ENROLLED') AS enrolled
                    FROM sections s
                    JOIN courses c ON s.course_id = c.course_id
                    WHERE s.instructor_id = ?
                    ORDER BY c.code
                """;

                try (Connection conn = DBConnection.getErpConnection();
                     PreparedStatement ps = conn.prepareStatement(secSql)) {
                    ps.setLong(1, instructorId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            activeCourses++;
                            int enrolled = rs.getInt("enrolled");
                            totalStudents += enrolled;
                            Map<String,Object> row = new HashMap<>();
                            row.put("course_code", rs.getString("course_code"));
                            row.put("course_title", rs.getString("course_title"));
                            row.put("enrolled", enrolled);
                            sections.add(row);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                out.put("activeCourses", activeCourses);
                out.put("totalStudents", totalStudents);
                out.put("sections", sections);
                return out;
            }

            @Override
            protected void done() {
                try {
                    Map<String,Object> result = get();
                    int ac = (int) result.getOrDefault("activeCourses", 0);
                    int ts = (int) result.getOrDefault("totalStudents", 0);
                    @SuppressWarnings("unchecked")
                    List<Map<String,Object>> secs = (List<Map<String,Object>>) result.getOrDefault("sections", new ArrayList<>());

                    lblActiveCourses.setText(String.valueOf(ac));
                    lblTotalStudents.setText(String.valueOf(ts));

                    // Update notification panel with instructor ID
                    notificationPanel.setInstructorId(instructorId);

                    myCoursesPanel.removeAll();
                    if (secs.isEmpty()) {
                        JLabel none = new JLabel("No active courses.");
                        none.setFont(Theme.BODY_FONT);
                        none.setForeground(Theme.NEUTRAL_MED);
                        myCoursesPanel.add(none);
                    } else {
                        JPanel grid = new JPanel(new GridLayout(Math.max(1, (secs.size()+1)/2), 2, 20, 20));
                        grid.setBackground(Theme.BACKGROUND);
                        for (Map<String,Object> r : secs) {
                            String code = String.valueOf(r.getOrDefault("course_code",""));
                            String title = String.valueOf(r.getOrDefault("course_title",""));
                            int enrolled = (int) r.getOrDefault("enrolled", 0);
                            grid.add(makeCourseCard(title, code, enrolled, "TBD", ""));
                        }
                        myCoursesPanel.add(grid);
                    }
                    myCoursesPanel.revalidate();
                    myCoursesPanel.repaint();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private JPanel makeCourseCard(String name, String code, int students, String schedule, String notice) {
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

    /**
 * Dynamic Class Stats Graph — Average Final Scores Per Course
 */
private JPanel createDynamicStatsChartPanel() {

    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    // load real stats
    try (Connection conn = DBConnection.getErpConnection()) {

        String sql = """
            SELECT c.title,
                   AVG(CASE 
                        WHEN g.final_grade REGEXP '^[0-9]+(\\.[0-9]+)?$' 
                        THEN CAST(g.final_grade AS DECIMAL(10,2))
                        ELSE NULL
                       END) AS avg_final
            FROM grades g
            JOIN enrollments e ON g.enrollment_id = e.enrollment_id
            JOIN sections s ON e.section_id = s.section_id
            JOIN courses c ON s.course_id = c.course_id
            WHERE g.component = '__FINAL__'
              AND s.instructor_id = ?
            GROUP BY c.title
            ORDER BY c.title;
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentInstructorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String course = rs.getString("title");
                    double avg = rs.getDouble("avg_final");
                    dataset.addValue(avg, "Avg Final (%)", course);
                }
            }
        }

    } catch (Exception ex) {
        ex.printStackTrace();
    }

    // create chart
    JFreeChart chart = ChartFactory.createBarChart(
            "Class Performance Overview",
            "Course",
            "Average Final Score",
            dataset
    );

    ChartPanel cp = new ChartPanel(chart);
    cp.setPreferredSize(new Dimension(500, 350));
    cp.setBackground(Theme.SURFACE);

    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setBackground(Theme.SURFACE);
    wrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
    wrapper.add(cp, BorderLayout.CENTER);
    return wrapper;
}

}