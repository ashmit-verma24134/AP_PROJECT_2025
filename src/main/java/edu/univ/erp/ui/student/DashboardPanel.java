package edu.univ.erp.ui.student;

import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.StudentDaoImpl;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardPanel — shows student's CGPA, enrolled courses, credits, attendance, schedule, and recent grades.
 * Call setStudentId(studentId) after login to populate values.
 */
public class DashboardPanel extends JPanel implements RegistrationListener {
    private String studentId = null;

    // Info fields
    private final JLabel heading = new JLabel();
    private final JLabel cgpaValueLabel = new JLabel("—");
    private final JLabel enrolledValueLabel = new JLabel("0");
    private final JLabel creditsValueLabel = new JLabel("0");
    private final JLabel attendanceValueLabel = new JLabel("—");
    private final JLabel feesValueLabel = new JLabel("—");

    // Content containers
    private final JPanel scheduleContainer = new JPanel();
    private final JPanel gradesContainer = new JPanel();

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(createDashboardContent());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createDashboardContent() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(Theme.BACKGROUND);
        main.setBorder(new EmptyBorder(20, 24, 24, 24));

        // header (centered)
        heading.setText("<html><h1 style='text-align:center;'>Dashboard</h1><p style='text-align:center;'>Welcome back!</p></html>");
        heading.setFont(Theme.HEADER_FONT);
        heading.setForeground(Theme.NEUTRAL_DARK);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(heading);
        main.add(Box.createVerticalStrut(18));

        // Info cards row
        JPanel infoRowWrap = new JPanel(new BorderLayout());
        infoRowWrap.setOpaque(false);
        JPanel infoRow = new JPanel(new GridLayout(1, 4, 20, 10));
        infoRow.setOpaque(false);

        // create info cards and ensure consistent height
        infoRow.add(createInfoCardWithLabel("Current CGPA", cgpaValueLabel, "+0.00 from last semester", Theme.SUCCESS));
        infoRow.add(createInfoCardWithLabel("Enrolled Courses", enrolledValueLabel, "Active this semester", Theme.NEUTRAL_MED));
        infoRow.add(createInfoCardWithLabel("Total Credits", creditsValueLabel, "This semester", Theme.NEUTRAL_MED));
        infoRow.add(createInfoCardWithLabel("Attendance", attendanceValueLabel, "—", Theme.SUCCESS));

        infoRowWrap.add(infoRow, BorderLayout.CENTER);
        main.add(infoRowWrap);
        main.add(Box.createVerticalStrut(22));

        // Middle row: schedule + grades with equal visual height
        JPanel middle = new JPanel(new GridLayout(1, 2, 20, 10));
        middle.setOpaque(false);

        // Setup containers
        scheduleContainer.setBackground(Theme.PRIMARY_LIGHT);
        scheduleContainer.setLayout(new BoxLayout(scheduleContainer, BoxLayout.Y_AXIS));
        scheduleContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        gradesContainer.setBackground(Theme.PRIMARY_LIGHT);
        gradesContainer.setLayout(new BoxLayout(gradesContainer, BoxLayout.Y_AXIS));
        gradesContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        // fixed content area height (both cards will look symmetrical)
        int contentHeight = 240;

        JScrollPane scheduleScroll = new JScrollPane(scheduleContainer);
        scheduleScroll.setBorder(null);
        scheduleScroll.setPreferredSize(new Dimension(0, contentHeight));
        scheduleScroll.getViewport().setBackground(Theme.PRIMARY_LIGHT);
        scheduleScroll.getVerticalScrollBar().setUnitIncrement(12);

        JScrollPane gradesScroll = new JScrollPane(gradesContainer);
        gradesScroll.setBorder(null);
        gradesScroll.setPreferredSize(new Dimension(0, contentHeight));
        gradesScroll.getViewport().setBackground(Theme.PRIMARY_LIGHT);
        gradesScroll.getVerticalScrollBar().setUnitIncrement(12);

        middle.add(createSectionCardWithContent("Today's Schedule", scheduleScroll));
        middle.add(createSectionCardWithContent("Recent Grades", gradesScroll));
        main.add(middle);
        main.add(Box.createVerticalStrut(22));

        // Announcements
        main.add(createAnnouncementsCard());
        main.add(Box.createVerticalStrut(40));
        return main;
    }

    private JPanel createInfoCardWithLabel(String title, JLabel valueLabel, String subtitle, Color subColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.NEUTRAL_LIGHT, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));
        card.setPreferredSize(new Dimension(0, 110)); // fix height for symmetry

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.BODY_FONT);
        lblTitle.setForeground(Theme.NEUTRAL_MED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(Theme.NEUTRAL_DARK);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setForeground(subColor);
        lblSub.setFont(Theme.BODY_FONT);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(lblSub, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createSectionCardWithContent(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.NEUTRAL_LIGHT, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        JLabel lbl = new JLabel("<html><h3 style='text-align:left;'>" + title + "</h3></html>");
        lbl.setForeground(Theme.NEUTRAL_DARK);
        card.add(lbl, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAnnouncementsCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.NEUTRAL_LIGHT, 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));
        JLabel lbl = new JLabel("<html><h3 style='text-align:center;'>Announcements</h3></html>");
        lbl.setForeground(Theme.NEUTRAL_DARK);
        card.add(lbl);
        card.add(Box.createVerticalStrut(10));
        card.add(createAnnouncement("Mid-term Exam Schedule Released", "2 days ago", true));
        card.add(createAnnouncement("Guest Lecture on AI/ML", "3 days ago", false));
        card.add(createAnnouncement("Library Hours Extended", "5 days ago", false));
        return card;
    }

    private JPanel createAnnouncement(String title, String time, boolean important) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Theme.PRIMARY_LIGHT);
        item.setBorder(new EmptyBorder(10, 12, 10, 12));
        String text = "<html><b>" + title + "</b><br><small>" + time + "</small></html>";
        if (important) text += "  <span style='color:white; background-color:#E74C3C; padding:2px 6px; border-radius:3px;'>Important</span>";
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Theme.NEUTRAL_DARK);
        item.add(lbl, BorderLayout.WEST);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        return item;
    }

    // list item + grade item
    private JPanel createListItem(String course, String room, String time) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Theme.PRIMARY_LIGHT);
        item.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel lblCourse = new JLabel("<html><b>" + course + "</b><br><small>" + room + "</small></html>");
        lblCourse.setForeground(Theme.NEUTRAL_DARK);
        JLabel lblTime = new JLabel("<html><b>" + time + "</b><br><small>upcoming</small></html>");
        lblTime.setForeground(Theme.PRIMARY);

        item.add(lblCourse, BorderLayout.WEST);
        item.add(lblTime, BorderLayout.EAST);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return item;
    }

    private JPanel createGradeItem(String course, String credits, String grade) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Theme.PRIMARY_LIGHT);
        item.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel lblCourse = new JLabel("<html><b>" + course + "</b><br><small>" + credits + "</small></html>");
        lblCourse.setForeground(Theme.NEUTRAL_DARK);
        JLabel lblGrade = new JLabel("<html><h3 style='color:#2FB6AD;'>" + grade + "</h3></html>");

        item.add(lblCourse, BorderLayout.WEST);
        item.add(lblGrade, BorderLayout.EAST);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return item;
    }

    /**
     * Set current student id (string). StudentPanel calls this after login.
     */
    public void setStudentId(String studentId) {
        this.studentId = studentId;
        // update heading and load data
        loadData(studentId);
        loadSchedule();
        loadRecentGrades();
    }

    /**
     * Load CGPA/enrolled/credits/attendance via StudentDao.getStudentOverview
     */
    public void loadData(String studentId) {
        String displayName = (studentId == null ? "—" : studentId);
        // try to prefer username/full_name if available
        if (studentId != null) {
            try (Connection conn = DBConnection.getErpConnection()) {
                String q = "SELECT s.full_name, s.roll_no, u.username " +
                           "FROM students s LEFT JOIN auth_db.users u ON s.user_id = u.user_id WHERE s.student_id = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(q)) {
                    ps.setString(1, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String uname = rs.getString("username");
                            String full = rs.getString("full_name");
                            String roll = rs.getString("roll_no");
                            if (uname != null && !uname.isEmpty()) displayName = uname;
                            else if (full != null && !full.isEmpty()) displayName = full;
                            else if (roll != null && !roll.isEmpty()) displayName = roll;
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        final String hd = displayName == null ? "—" : escapeHtml(displayName);
        SwingUtilities.invokeLater(() -> heading.setText("<html><h1 style='text-align:center;'>Dashboard</h1><p style='text-align:center;'>Welcome back, " + hd + ".</p></html>"));

        // placeholder values
        SwingUtilities.invokeLater(() -> {
            cgpaValueLabel.setText("...");
            enrolledValueLabel.setText("...");
            creditsValueLabel.setText("...");
            attendanceValueLabel.setText("...");
            feesValueLabel.setText("...");
        });

        if (studentId == null) {
            SwingUtilities.invokeLater(() -> {
                cgpaValueLabel.setText("—");
                enrolledValueLabel.setText("0");
                creditsValueLabel.setText("0");
                attendanceValueLabel.setText("—");
                feesValueLabel.setText("—");
            });
            return;
        }

        new SwingWorker<Map<String,Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    StudentDao dao = new StudentDaoImpl(conn);
                    return dao.getStudentOverview(studentId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return new HashMap<>();
                }
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> m = get();
                    if (m == null) m = new HashMap<>();
                    Object cg = m.get("cgpa");
                    cgpaValueLabel.setText(cg == null ? "—" : String.valueOf(cg));
                    enrolledValueLabel.setText(String.valueOf(m.getOrDefault("enrolled_count", 0)));
                    Object tot = m.get("total_credits");
                    creditsValueLabel.setText(tot == null ? "0" : String.valueOf(tot));
                    Object att = m.get("attendance_percent");
                    attendanceValueLabel.setText(att == null ? "—" : String.valueOf(att) + "%");
                    Object fees = m.get("pending_fees");
                    feesValueLabel.setText(fees == null ? "₹0" : ("₹" + fees));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }
/** Dashboard has no interactive actions, but StudentPanel expects this method. */
public void setActionsEnabled(boolean enabled) {
    // nothing to disable here — dashboard is read-only
}

    private void loadSchedule() {
        scheduleContainer.removeAll();
        scheduleContainer.add(createListItem("Loading schedule...", "", ""));
        scheduleContainer.revalidate();
        scheduleContainer.repaint();

        if (studentId == null) {
            scheduleContainer.removeAll();
            scheduleContainer.add(createListItem("No student selected", "", ""));
            return;
        }

        new SwingWorker<List<Map<String,Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    StudentDao dao = new StudentDaoImpl(conn);
                    return dao.getUpcomingSchedule(studentId, 6);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Map<String,Object>> rows = get();
                    scheduleContainer.removeAll();
                    if (rows == null || rows.isEmpty()) {
                        scheduleContainer.add(createListItem("No scheduled classes", "", ""));
                    } else {
                        for (Map<String,Object> r : rows) {
                            String title = (String) r.getOrDefault("course_title", r.getOrDefault("course_code", "Course"));
                            String room = (String) r.getOrDefault("room", "");
                            String dt = (String) r.getOrDefault("day_time", "");
                            scheduleContainer.add(createListItem(title, room, dt));
                        }
                    }
                    scheduleContainer.revalidate();
                    scheduleContainer.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void loadRecentGrades() {
        gradesContainer.removeAll();
        gradesContainer.add(createListItem("Loading grades...", "", ""));
        gradesContainer.revalidate();
        gradesContainer.repaint();

        if (studentId == null) {
            gradesContainer.removeAll();
            gradesContainer.add(createListItem("No student selected", "", ""));
            return;
        }

        new SwingWorker<List<Map<String,Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    StudentDao dao = new StudentDaoImpl(conn);
                    return dao.getRecentGrades(studentId, 6);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Map<String,Object>> rows = get();
                    gradesContainer.removeAll();
                    if (rows == null || rows.isEmpty()) {
                        gradesContainer.add(createListItem("No grades yet", "", ""));
                    } else {
                        for (Map<String,Object> r : rows) {
                            String title = (String) r.getOrDefault("course_title", r.getOrDefault("course_code", "Course"));
                            Object cr = r.get("credits");
                            String credits = cr == null ? "" : (cr instanceof Number ? String.valueOf(((Number) cr).intValue()) : cr.toString());
                            String grade = (String) r.getOrDefault("final_grade", "—");
                            gradesContainer.add(createGradeItem(title, credits + " Credits", grade));
                        }
                    }
                    gradesContainer.revalidate();
                    gradesContainer.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    @Override
    public void onRegistrationChanged() {
        // refresh all dashboard panels if registration state changed
        if (this.studentId != null) {
            loadData(this.studentId);
            loadSchedule();
            loadRecentGrades();
        }
    }

    // small HTML escape
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
