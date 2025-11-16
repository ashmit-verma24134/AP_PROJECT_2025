package edu.univ.erp.ui.student;

import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.StudentDaoImpl;
import edu.univ.erp.service.RegistrationEventBus;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * GradesPanel: semester accordion + per-semester course table + "See Components" action.
 *
 * Relies on:
 *  - StudentDao.getGradeDetails(studentId) -> List<Map<String,Object>>
 *  - AssessmentsDialog(Window, long, String, String) constructor
 *  - RegistrationEventBus for real-time refresh
 */
public class GradesPanel extends JPanel implements RegistrationEventBus.Listener {

    private String studentId;

    private final JPanel semestersContainer; // container holding semester sections
    private final JButton btnRefresh;
    private final JLabel statusLabel;

    public GradesPanel() {
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

        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    StudentDao dao = new StudentDaoImpl(conn);
                    return dao.getGradeDetails(studentId);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
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

    private int countUniqueEnrollments(List<Map<String, Object>> rows) {
        if (rows == null) return 0;
        java.util.Set<Object> s = new java.util.HashSet<>();
        for (Map<String, Object> r : rows) {
            Object en = r.get("enrollment_id");
            if (en != null) s.add(en);
        }
        return s.size();
    }

    /**
     * Build the semester accordion panels from DAO rows.
     * Each semester becomes a collapsible panel with a course table inside.
     */
private void buildSemesterPanels(List<Map<String,Object>> rows) {

    // semestersContainer already exists (final) — just reset
    semestersContainer.removeAll();
    semestersContainer.setLayout(new BoxLayout(semestersContainer, BoxLayout.Y_AXIS));
    semestersContainer.setBackground(Color.WHITE);

    // Group by semester string
    java.util.Map<String, java.util.List<Map<String,Object>>> bySem = new java.util.LinkedHashMap<>();
    for (Map<String,Object> r : rows) {
        String sem;
        Object semObj = r.get("semester");
        Object yearObj = r.get("year");

        if (semObj != null)
            sem = String.valueOf(semObj) + (yearObj != null ? " / " + yearObj : "");
        else if (yearObj != null)
            sem = "Term / " + yearObj;
        else
            sem = "Current";

        bySem.computeIfAbsent(sem, k -> new java.util.ArrayList<>()).add(r);
    }


    boolean firstAdded = false;
    for (String semKey : bySem.keySet()) {
        java.util.List<Map<String,Object>> semRows = bySem.get(semKey);

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
        for (Map<String,Object> r : semRows) {
            Object enId = r.get("enrollment_id");
            if (seen.contains(enId)) continue;
            seen.add(enId);
            Object code = r.get("course_code");
            Object title = r.get("course_name");
            Object credits = r.get("credits") == null ? "—" : r.get("credits");
            Object finalGrade = r.get("final_grade") == null ? "N/A" : r.get("final_grade");
            Object gradePoint = r.get("grade_point") == null ? "—" : r.get("grade_point");
            tm.addRow(new Object[]{ idx++, code, title, credits, finalGrade, gradePoint, "See Components" });
        }

        // Table scroll pane sized to a reasonable collapsed/expanded height
        JScrollPane tableScroll = new JScrollPane(semesterTable);
        tableScroll.setPreferredSize(new Dimension(900, 220)); // expanded size
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.add(tableScroll, BorderLayout.CENTER);

        // footer SGPA - very simple
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        JLabel sgpa = new JLabel("SGPA: 0.00 (Credits: " + Math.max(0, tm.getRowCount()) + ")");
        sgpa.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footer.add(sgpa);
        body.add(footer, BorderLayout.SOUTH);

        // Create collapsible semester panel
        SemesterPanel sp = new SemesterPanel(semKey, body);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add click handler for "See Components" by mouse click on the table
        semesterTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = semesterTable.rowAtPoint(e.getPoint());
                int col = semesterTable.columnAtPoint(e.getPoint());
                if (row < 0) return;
                int actionsCol = semesterTable.getColumnModel().getColumnIndex("Actions");
                if (col == actionsCol) {
                    // find matching enrollment id from semRows by matching code+title
                    Object code = semesterTable.getValueAt(row, 1);
                    Object title = semesterTable.getValueAt(row, 2);
                    long enrollmentId = -1;
                    String courseCode = null;
                    String courseTitle = null;
                    for (Map<String,Object> rr : semRows) {
                        Object rc = rr.get("course_code");
                        Object rt = rr.get("course_name");
                        if (rc != null && rt != null && rc.equals(code) && rt.equals(title)) {
                            Object en = rr.get("enrollment_id");
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
                            AssessmentsDialog dlg = new AssessmentsDialog(owner, finalEnrollmentId, finalCourseCode, finalCourseTitle);
                            dlg.setVisible(true);
                        });
                    } else {
                        JOptionPane.showMessageDialog(GradesPanel.this, "Cannot locate enrollment id for that course", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Add semester panel to container
        semestersContainer.add(sp);
        semestersContainer.add(Box.createVerticalStrut(8));

        // expand first semester automatically (optional)
        if (!firstAdded) {
            sp.expand();
            firstAdded = true;
        }
    }

    semestersContainer.revalidate();
    semestersContainer.repaint();
}
    /**
     * Inner class: simple collapsible semester panel.
     * Header appears as a button-like label; clicking toggles visibility of body.
     */
      /**
     * Inner class: simple collapsible semester panel.
     * Header appears as a button-like row; clicking toggles visibility of body.
     */
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

        // Header area
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

        // Body: card layout so collapsed state occupies no preferred height
        bodyCards = new JPanel(new CardLayout());
        bodyCards.setBackground(Color.WHITE);

        // empty panel (zero-ish height)
        JPanel empty = new JPanel();
        empty.setBackground(Color.WHITE);

        // content wrapper: we add the provided content inside a wrapper that has a preferred size
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(Color.WHITE);
        contentWrapper.add(content, BorderLayout.CENTER);

        bodyCards.add("empty", empty);
        bodyCards.add("content", contentWrapper);

        // start collapsed
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
            // collapse siblings for accordion behavior
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

        // force layout update
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
    /**
     * RegistrationEventBus callback (other parts call notifyChange() after register/drop).
     * We'll just reload.
     */
    @Override
    public void onRegistrationChanged() {
        SwingUtilities.invokeLater(this::reload);
    }

    /**
     * Cleanup when disposing parent (unregister listener).
     */
    public void dispose() {
        try { RegistrationEventBus.get().unregister(this); } catch (Throwable t) {}
    }
}
