package edu.univ.erp.ui.student;

import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * Semester-wise Grades Panel.
 *
 * - Shows semesters stacked (semester + year) with course rows.
 * - Each course row has an action "See Components" which opens AssessmentsDialog(enrollmentId, code, title).
 * - Has a Refresh button to reload instantly.
 *
 * Requires:
 *  - DBConnection.getErpConnection()
 *  - edu.univ.erp.ui.student.AssessmentsDialog(Window owner, long enrollmentId, String courseCode, String courseTitle)
 *  - Theme constants used for look & feel
 *
 * To integrate: replace previous GradesPanel usage with new SemesterGradesPanel in StudentPanel.
 */
public class SemesterGradesPanel extends JPanel {

    private String studentId;
    private final JPanel semestersContainer;
    private final JButton btnRefresh;
    private final JLabel statusLabel;

    public SemesterGradesPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(8,8,8,8));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BACKGROUND);
        JLabel title = new JLabel("Grades & Assessments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setBackground(Theme.BACKGROUND);
        btnRefresh = new JButton("Refresh");
        btnRefresh.setBackground(Theme.PRIMARY);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.addActionListener(e -> reload());
        right.add(btnRefresh);

        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Body: scrollable vertical container to hold semester panels
        semestersContainer = new JPanel();
        semestersContainer.setLayout(new BoxLayout(semestersContainer, BoxLayout.Y_AXIS));
        semestersContainer.setBackground(Theme.BACKGROUND);

        JScrollPane bodyScroll = new JScrollPane(semestersContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(bodyScroll, BorderLayout.CENTER);

        // Status area
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.BACKGROUND);
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        footer.add(statusLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }

    /**
     * Set the student id (string). Accepts numeric string or null.
     */
    public void setStudentId(String studentId) {
        this.studentId = studentId;
        reload();
    }

    /**
     * Reload UI from DB.
     */
    public void reload() {
        if (studentId == null) {
            statusLabel.setText("No student selected");
            clearSemesters();
            return;
        }
        btnRefresh.setEnabled(false);
        statusLabel.setText("Loading grades...");
        clearSemesters();

        SwingWorker<Map<String, List<CourseRow>>, Void> w = new SwingWorker<>() {
            @Override
            protected Map<String, List<CourseRow>> doInBackground() throws Exception {
                Map<String, List<CourseRow>> bySemester = new LinkedHashMap<>();

                String sql =
    "SELECT sec.semester AS sem_label, sec.year AS sem_year, e.enrollment_id, c.code AS course_code, " +
    "       c.title AS course_title, c.credits AS credits, " +
    "       g_final.final_grade AS final_letter, g_final.score AS final_score " +
    "FROM enrollments e " +
    "JOIN sections sec ON e.section_id = sec.section_id " +
    "JOIN courses c ON sec.course_id = c.course_id " +
    "LEFT JOIN grades g_final ON g_final.enrollment_id = e.enrollment_id AND LOWER(g_final.component) = 'final' " +
    "WHERE e.student_id = ? AND e.status IN ('ENROLLED','COMPLETED') " +
    "ORDER BY sec.year DESC, " +
    "         CASE sec.semester " +
    "            WHEN 'Monsoon' THEN 1 " +
    "            WHEN 'Fall' THEN 1 " +
    "            WHEN 'Winter' THEN 2 " +
    "            WHEN 'Spring' THEN 2 " +
    "            WHEN 'Summer' THEN 3 " +
    "            ELSE 4 " +
    "         END ASC, " +
    "         c.code ASC";

                try (Connection conn = DBConnection.getErpConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    // allow numeric or string id
                    try { ps.setLong(1, Long.parseLong(studentId)); }
                    catch (NumberFormatException ex) { ps.setString(1, studentId); }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String sem = safe(rs.getString("sem_label"));
                            Integer year = rs.getObject("sem_year") == null ? null : rs.getInt("sem_year");
                            String headerKey = sem == null ? (year == null ? "Unknown" : ("Year " + year)) : (sem + (year==null ? "" : " / " + year));
                            CourseRow r = new CourseRow();
                            r.enrollmentId = rs.getLong("enrollment_id");
                            r.courseCode = rs.getString("course_code");
                            r.courseTitle = rs.getString("course_title");
                            r.credits = rs.getObject("credits") == null ? null : rs.getDouble("credits");
                            r.finalLetter = rs.getString("final_letter");
                            r.finalScore = rs.getObject("final_score") == null ? null : rs.getDouble("final_score");

                            bySemester.computeIfAbsent(headerKey, k -> new ArrayList<>()).add(r);
                        }
                    }
                }

                return bySemester;
            }

            @Override
            protected void done() {
                try {
                    Map<String, List<CourseRow>> map = get();
                    if (map.isEmpty()) {
                        JLabel empty = new JLabel("No courses found for this student.", SwingConstants.CENTER);
                        empty.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                        empty.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
                        semestersContainer.add(empty);
                    } else {
                        // Build panels for each semester (preserve insertion order)
                        for (Map.Entry<String, List<CourseRow>> e : map.entrySet()) {
                            String semLabel = e.getKey();
                            List<CourseRow> rows = e.getValue();
                            JPanel semPanel = createSemesterPanel(semLabel, rows);
                            semestersContainer.add(semPanel);
                            semestersContainer.add(Box.createRigidArea(new Dimension(1,12)));
                        }
                    }
                    semestersContainer.revalidate();
                    semestersContainer.repaint();
                    statusLabel.setText("Loaded " + map.values().stream().mapToInt(List::size).sum() + " courses");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    statusLabel.setText("Failed loading grades: " + ex.getMessage());
                    JLabel err = new JLabel("Error loading grades: " + ex.getMessage(), SwingConstants.CENTER);
                    err.setForeground(Color.RED);
                    semestersContainer.add(err);
                } finally {
                    btnRefresh.setEnabled(true);
                }
            }
        };
        w.execute();
    }

    private void clearSemesters() {
        semestersContainer.removeAll();
        semestersContainer.revalidate();
        semestersContainer.repaint();
    }

    /**
     * Create a titled panel for a semester with a table listing course rows and SGPA summary.
     */
    private JPanel createSemesterPanel(String semLabel, List<CourseRow> rows) {
        JPanel panel = new JPanel(new BorderLayout(6,6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,1,1,1, new Color(220,220,220)),
                BorderFactory.createEmptyBorder(8,8,8,8))
        );

        // header row — sem title on left
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(panel.getBackground());
        JLabel h = new JLabel(semLabel);
        h.setFont(new Font("Segoe UI", Font.BOLD, 14));
        top.add(h, BorderLayout.WEST);
        panel.add(top, BorderLayout.NORTH);

        // table
        String[] cols = {"#", "Course Code", "Course Title", "Credits", "Grade", "Grade Point", "Actions"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(m);
        table.setRowHeight(28);
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setFillsViewportHeight(true);

        // populate rows
        int idx = 1;
        double totalCredits = 0.0;
        double totalWeightedPoints = 0.0;
        for (CourseRow cr : rows) {
            Double gp = gradePointFromLetter(cr.finalLetter);
            if (cr.credits != null) {
                totalCredits += cr.credits;
                if (gp != null) totalWeightedPoints += (gp * cr.credits);
            }
            Object grade = cr.finalLetter == null ? "N/A" : cr.finalLetter;
            Object gpObj = gp == null ? "—" : String.format("%.2f", gp);
            m.addRow(new Object[]{idx++, cr.courseCode, cr.courseTitle, cr.credits == null ? "—" : cr.credits, grade, gpObj, "See Components"});
        }

        JScrollPane sp = new JScrollPane(table);
        panel.add(sp, BorderLayout.CENTER);

        // actions: make the "Actions" column render a button behavior (single-click)
        int actionsCol = 6;
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                int c = table.columnAtPoint(e.getPoint());
                if (r >= 0 && c == actionsCol) {
                    CourseRow cr = rows.get(r);
                    openAssessmentsDialog(cr);
                } else if (e.getClickCount() == 2) {
                    // double click anywhere opens components
                    CourseRow cr = rows.get(r);
                    openAssessmentsDialog(cr);
                }
            }
        });

        // footer: show SGPA (if credits exist)
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(panel.getBackground());
        footer.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        String sgpaText;
        if (totalCredits > 0.0) {
            double sgpa = totalWeightedPoints / totalCredits;
            sgpa = Math.round(sgpa * 100.0) / 100.0;
            sgpaText = "SGPA: " + String.format("%.2f", sgpa) + "   (Credits: " + (int)totalCredits + ")";
        } else {
            sgpaText = "SGPA: N/A";
        }
        JLabel lblSGPA = new JLabel(sgpaText);
        lblSGPA.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footer.add(lblSGPA, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private void openAssessmentsDialog(CourseRow cr) {
        Window w = SwingUtilities.getWindowAncestor(this);
        try {
            AssessmentsDialog dlg = new AssessmentsDialog(w, cr.enrollmentId, cr.courseCode, cr.courseTitle);
            dlg.setVisible(true);
        } catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to open assessment dialog: " + t.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static Double gradePointFromLetter(String letter) {
        if (letter == null) return null;
        String l = letter.trim().toUpperCase();
        return switch (l) {
            case "A+" -> 10.0;
            case "A" -> 10.0;
            case "A-" -> 9.0;
            case "B+" -> 8.0;
            case "B" -> 7.0;
            case "B-" -> 6.0;
            case "C+" -> 5.0;
            case "C" -> 4.0;
            case "C-" -> 3.0;
            case "D" -> 2.0;
            case "F" -> 0.0;
            default -> null;
        };
    }

    // simple POJO to carry course row details
    private static class CourseRow {
        long enrollmentId;
        String courseCode;
        String courseTitle;
        Double credits;
        String finalLetter;
        Double finalScore;
    }
}
