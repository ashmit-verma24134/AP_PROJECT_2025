package edu.univ.erp.ui.student;

import edu.univ.erp.model.GradeDetail;
import edu.univ.erp.service.ServiceException;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.service.RegistrationEventBus; // keep your existing event bus package
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * Refactored GradesPanel: UI no longer talks to DB/DAO directly.
 * It calls StudentService to fetch grade detail DTOs.
 */
public class GradesPanel extends JPanel implements RegistrationEventBus.Listener {

    private String studentId;

    private final JPanel semestersContainer; // container holding semester sections
    private final JButton btnRefresh;
    private final JLabel statusLabel;

    private final StudentService studentService;

    public GradesPanel(StudentService studentService) {
        this.studentService = studentService;

        setLayout(new BorderLayout(8, 8));
        setBackground(Theme.BACKGROUND);

        // Header
        JLabel title = new JLabel("Grades & Assessments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(title, BorderLayout.NORTH);

        // Top toolbar with Refresh
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.BACKGROUND);
        btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> reload());
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        r.setBackground(Theme.BACKGROUND);
        r.add(btnRefresh);
        topBar.add(r, BorderLayout.EAST);

        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        topBar.add(statusLabel, BorderLayout.WEST);

        add(topBar, BorderLayout.BEFORE_FIRST_LINE);

        // center: scrollable semesters container
        semestersContainer = new JPanel();
        semestersContainer.setLayout(new BoxLayout(semestersContainer, BoxLayout.Y_AXIS));
        semestersContainer.setBackground(Theme.BACKGROUND);

        JScrollPane sc = new JScrollPane(semestersContainer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(sc, BorderLayout.CENTER);

        // register to event bus so registration changes trigger reload
        RegistrationEventBus.get().register(this);
    }

    /**
     * Public API to set current student and load their data.
     */
    public void setStudentId(String id) {
        this.studentId = id;
        reload();
    }

    /**
     * Reload the whole panel (fetch grade rows and build semester accordion).
     */
    public void reload() {
        if (studentId == null) return;
        btnRefresh.setEnabled(false);
        statusLabel.setText("Loading courses...");

        // clear existing UI quickly
        semestersContainer.removeAll();
        semestersContainer.add(new JLabel("Loading...", SwingConstants.CENTER));
        semestersContainer.revalidate();
        semestersContainer.repaint();

        new SwingWorker<List<GradeDetail>, Void>() {
            @Override
            protected List<GradeDetail> doInBackground() throws Exception {
                try {
                    return studentService.getGradeDetails(studentId);
                } catch (ServiceException se) {
                    throw se;
                }
            }

            @Override
            protected void done() {
                try {
                    List<GradeDetail> rows = get();
                    buildSemesterPanels(rows);
                    statusLabel.setText("Loaded " + countUniqueEnrollments(rows) + " courses");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    semestersContainer.removeAll();
                    semestersContainer.add(new JLabel("Error loading data: " + ex.getMessage()));
                    statusLabel.setText("Error");
                } finally {
                    btnRefresh.setEnabled(true);
                }
            }
        }.execute();
    }

    private int countUniqueEnrollments(List<GradeDetail> rows) {
        if (rows == null) return 0;
        java.util.Set<Object> s = new java.util.HashSet<>();
        for (GradeDetail r : rows) {
            Object en = r.getEnrollmentId();
            if (en != null) s.add(en);
        }
        return s.size();
    }

    /**
     * Build the semester accordion panels from service DTOs.
     */
    private void buildSemesterPanels(List<GradeDetail> rows) {
        semestersContainer.removeAll();
        semestersContainer.setLayout(new BoxLayout(semestersContainer, BoxLayout.Y_AXIS));
        semestersContainer.setBackground(Color.WHITE);

        java.util.Map<String, java.util.List<GradeDetail>> bySem = new java.util.LinkedHashMap<>();
        if (rows != null) {
            for (GradeDetail r : rows) {
                String sem;
                String semStr = r.getSemester();
                Integer year = r.getYear();
                if (semStr != null)
                    sem = semStr + (year != null ? " / " + year : "");
                else if (year != null)
                    sem = "Term / " + year;
                else
                    sem = "Current";

                bySem.computeIfAbsent(sem, k -> new java.util.ArrayList<>()).add(r);
            }
        }

        boolean firstAdded = false;
        for (String semKey : bySem.keySet()) {
            java.util.List<GradeDetail> semRows = bySem.get(semKey);

            // Build table model for this semester
            String[] cols = new String[] {"#", "Course Code", "Course Title", "Credits", "Grade", "Grade Point", "Actions"};
            DefaultTableModel tm = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable semesterTable = new JTable(tm);
            semesterTable.setRowHeight(28);
            semesterTable.getTableHeader().setBackground(Theme.PRIMARY);
            semesterTable.getTableHeader().setForeground(Color.WHITE);

            // collect unique enrollment rows
            java.util.Set<Object> seen = new java.util.HashSet<>();
            int idx = 1;
            for (GradeDetail r : semRows) {
                Object enId = r.getEnrollmentId();
                if (seen.contains(enId)) continue;
                seen.add(enId);
                Object code = r.getCourseCode();
                Object title = r.getCourseName();
                Object credits = r.getCredits() == null ? "—" : r.getCredits();
                Object finalGrade = r.getFinalGrade() == null ? "N/A" : r.getFinalGrade();
                Object gradePoint = r.getGradePoint() == null ? "—" : r.getGradePoint();
                tm.addRow(new Object[]{ idx++, code, title, credits, finalGrade, gradePoint, "See Components" });
            }

            JScrollPane tableScroll = new JScrollPane(semesterTable);
            tableScroll.setPreferredSize(new Dimension(900, 220)); // expanded size
            JPanel body = new JPanel(new BorderLayout());
            body.setBackground(Color.WHITE);
            body.add(tableScroll, BorderLayout.CENTER);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            footer.setBackground(Color.WHITE);
            JLabel sgpa = new JLabel("SGPA: 0.00 (Credits: " + Math.max(0, tm.getRowCount()) + ")");
            sgpa.setFont(new Font("Segoe UI", Font.BOLD, 12));
            footer.add(sgpa);
            body.add(footer, BorderLayout.SOUTH);

            SemesterPanel sp = new SemesterPanel(semKey, body);
            sp.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Add click handler for "See Components"
            semesterTable.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int row = semesterTable.rowAtPoint(e.getPoint());
                    int col = semesterTable.columnAtPoint(e.getPoint());
                    if (row < 0) return;
                    int actionsCol = semesterTable.getColumnModel().getColumnIndex("Actions");
                    if (col == actionsCol) {
                        Object code = semesterTable.getValueAt(row, 1);
                        Object title = semesterTable.getValueAt(row, 2);
                        long enrollmentId = -1;
                        String courseCode = null;
                        String courseTitle = null;
                        for (GradeDetail rr : semRows) {
                            Object rc = rr.getCourseCode();
                            Object rt = rr.getCourseName();
                            if (rc != null && rt != null && rc.equals(code) && rt.equals(title)) {
                                Object en = rr.getEnrollmentId();
                                if (en instanceof Number) enrollmentId = ((Number)en).longValue();
                                else {
                                    try { enrollmentId = Long.parseLong(String.valueOf(en)); }
                                    catch (Exception ex) { enrollmentId = -1; }
                                }
                                courseCode = rc == null ? null : String.valueOf(rc);
                                courseTitle = rt == null ? null : String.valueOf(rt);
                                break;
                            }
                        }
                        if (enrollmentId > 0) {
                            final long finalEnrollmentId = enrollmentId;
                            final String finalCourseCode = courseCode;
                            final String finalCourseTitle = courseTitle;
SwingUtilities.invokeLater(() -> {
    Window owner = SwingUtilities.getWindowAncestor(GradesPanel.this);
    // create an AssessmentService implementation (adjust Impl class name if different)
    edu.univ.erp.service.AssessmentService assessmentService =
            new edu.univ.erp.service.AssessmentServiceImpl();
    AssessmentsDialog dlg = new AssessmentsDialog(owner, finalEnrollmentId, finalCourseCode, finalCourseTitle, assessmentService);
    dlg.setVisible(true);
});

                        } else {
                            JOptionPane.showMessageDialog(GradesPanel.this, "Cannot locate enrollment id for that course", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

            semestersContainer.add(sp);
            semestersContainer.add(Box.createVerticalStrut(8));

            if (!firstAdded) {
                sp.expand();
                firstAdded = true;
            }
        }

        semestersContainer.revalidate();
        semestersContainer.repaint();
    }

    // SemesterPanel inner class (same as original) ...
    private class SemesterPanel extends JPanel {
        private final JPanel header;
        private final JPanel bodyCards; // CardLayout: "empty" or "content"
        private final String semTitle;
        private boolean expanded = false;

        SemesterPanel(String title, JPanel content) {
            this.semTitle = title;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));

            header = new JPanel(new BorderLayout());
            header.setBackground(new Color(35, 57, 115));
            header.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            header.setOpaque(true);
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel lbl = new JLabel(title);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            header.add(lbl, BorderLayout.WEST);

            JLabel arrow = new JLabel("\u25BC"); // down arrow
            arrow.setForeground(Color.WHITE);
            arrow.setFont(arrow.getFont().deriveFont(Font.BOLD, 14f));
            header.add(arrow, BorderLayout.EAST);

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { toggle(); }
                @Override public void mouseEntered(MouseEvent e) { header.setBackground(new Color(45,67,135)); }
                @Override public void mouseExited(MouseEvent e) { header.setBackground(new Color(35,57,115)); }
            };
            header.addMouseListener(ma);
            lbl.addMouseListener(ma);
            arrow.addMouseListener(ma);
            header.putClientProperty("arrow", arrow);

            bodyCards = new JPanel(new CardLayout());
            bodyCards.setBackground(Color.WHITE);

            JPanel empty = new JPanel();
            empty.setBackground(Color.WHITE);

            JPanel contentWrapper = new JPanel(new BorderLayout());
            contentWrapper.setBackground(Color.WHITE);
            contentWrapper.add(content, BorderLayout.CENTER);

            bodyCards.add("empty", empty);
            bodyCards.add("content", contentWrapper);

            showCard("empty");

            add(header, BorderLayout.NORTH);
            add(bodyCards, BorderLayout.CENTER);
        }

        private void showCard(String name) {
            CardLayout cl = (CardLayout) bodyCards.getLayout();
            cl.show(bodyCards, name);
        }

        void toggle() {
            expanded = !expanded;
            JLabel arrow = (JLabel) header.getClientProperty("arrow");
            if (expanded) {
                showCard("content");
                if (arrow != null) arrow.setText("\u25B2"); // up
                Component[] comps = semestersContainer.getComponents();
                for (Component c : comps) {
                    if (c instanceof SemesterPanel && c != this) {
                        SemesterPanel sp = (SemesterPanel) c;
                        if (sp.expanded) {
                            sp.expanded = false;
                            sp.showCard("empty");
                            JLabel a = (JLabel) sp.header.getClientProperty("arrow");
                            if (a != null) a.setText("\u25BC");
                        }
                    }
                }
            } else {
                showCard("empty");
                if (arrow != null) arrow.setText("\u25BC"); // down
            }

            bodyCards.revalidate();
            bodyCards.repaint();
            this.revalidate();
            this.repaint();
            semestersContainer.revalidate();
            semestersContainer.repaint();
        }

        void expand() {
            if (!expanded) toggle();
        }
    }

    @Override
    public void onRegistrationChanged() {
        SwingUtilities.invokeLater(this::reload);
    }

    public void dispose() {
        try { RegistrationEventBus.get().unregister(this); } catch (Throwable t) {}
    }
}