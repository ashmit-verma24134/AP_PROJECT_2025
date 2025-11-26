package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*; // for AbstractDocument, DocumentFilter, BadLocationException, AttributeSet

import javax.swing.table.DefaultTableModel;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

/**
 * AdminCourseSectionPanel - courses + sections editor with section edit dialog (assign instructor)
 *
 * - Upper drop-deadline spinner removed per request.
 * - Table shows only date (yyyy-MM-dd) for drop deadline (no time).
 * - New sections inserted with drop_deadline = NULL by default (use editor to set).
 */
public class AdminCourseSectionPanel extends JPanel {

    // --- Courses UI ---
    private final JTextField txtCourseCode = new JTextField(12);
    private final JTextField txtCourseTitle = new JTextField(30);
    private final JTextField txtCourseCredits = new JTextField(6);

    private final DefaultTableModel coursesModel;
    private final JTable coursesTable;
    private final JButton btnCourseAdd = new JButton("Add Course");
    private static class EnrollmentRef { final String tableName; final String sectionColumn; EnrollmentRef(String t, String c) { tableName = t; sectionColumn = c; } }

    private final JButton btnCourseRefresh = new JButton("Refresh");
    private final SimpleDateFormat dropFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final JButton btnCourseDelete = new JButton("Delete Course"); // new

    // --- Sections UI ---
    private final JComboBox<CourseEntry> cbCourses = new JComboBox<>();
    private final JComboBox<InstructorEntry> cbInstructors = new JComboBox<>();
    private final JTextField txtDayTime = new JTextField(20);
    private final JTextField txtRoom = new JTextField(12);
    // at class level
private final JSpinner spDropDeadline = new JSpinner(new SpinnerDateModel());

    private final JTextField txtCapacity = new JTextField(6);
    private final JTextField txtSemester = new JTextField(8);
    private final JTextField txtYear = new JTextField(6);
    private final DefaultTableModel sectionsModel;
    private final JTable sectionsTable;
    private final JButton btnSectionAdd = new JButton("Add Section");
    private final JButton btnSectionRefresh = new JButton("Refresh Sections");
    private final JButton btnSectionDelete = new JButton("Delete Section"); // new

    public AdminCourseSectionPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));
        

        JTabbedPane tabs = new JTabbedPane();

        // ---------- Courses Tab ----------
        JPanel pCourses = new JPanel(new BorderLayout());
        pCourses.setOpaque(false);
        JPanel courseForm = new JPanel(new GridBagLayout());
        btnSectionDelete.addActionListener(e -> deleteSelectedSection());

        courseForm.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.anchor = GridBagConstraints.WEST;

        int r = 0;
        gc.gridx = 0; gc.gridy = r;
        courseForm.add(new JLabel("Course Code:"), gc);
        gc.gridx = 1; courseForm.add(txtCourseCode, gc);

        gc.gridx = 2; courseForm.add(new JLabel("Title:"), gc);
        gc.gridx = 3; courseForm.add(txtCourseTitle, gc);

        r++;
        gc.gridy = r; gc.gridx = 0;
        courseForm.add(new JLabel("Credits:"), gc);
        gc.gridx = 1; courseForm.add(txtCourseCredits, gc);

        JPanel courseBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        courseBtns.setOpaque(false);
        courseBtns.add(btnCourseAdd);
        courseBtns.add(btnCourseRefresh);
        courseBtns.add(btnCourseDelete); // added to UI
        r++;
        gc.gridx = 0; gc.gridy = r; gc.gridwidth = 4;
        courseForm.add(courseBtns, gc);

        pCourses.add(courseForm, BorderLayout.NORTH);

        coursesModel = new DefaultTableModel(new String[]{"ID","Code","Title","Credits"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        coursesTable = new JTable(coursesModel);
        coursesTable.setAutoCreateRowSorter(true);
        coursesTable.setRowHeight(26);

        // double-click course -> open CourseEditorDialog if available
        coursesTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                int viewRow = coursesTable.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = coursesTable.convertRowIndexToModel(viewRow);
                Object idObj = coursesTable.getModel().getValueAt(modelRow, 0); // ID column assumed at 0
                final long courseId;
                try {
                    courseId = (idObj instanceof Number) ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Cannot open editor: invalid course id", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Window owner = SwingUtilities.getWindowAncestor(AdminCourseSectionPanel.this);
                try {
                    // If you have CourseEditorDialog class in project, this will open it:
                    CourseEditorDialog dlg = new CourseEditorDialog(owner, courseId, true);
                    dlg.setLocationRelativeTo(owner);
                    dlg.setModal(true);
                    dlg.setVisible(true);

                } catch (NoClassDefFoundError | Exception ex) {
                    // fallback: inform user
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Course editor not available: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                // refresh after dialog closes
                loadCourses();
                loadSectionsAndFillCombos();
            }
        });

        JScrollPane scCourses = new JScrollPane(coursesTable);
        scCourses.setPreferredSize(new Dimension(900, 320));
        pCourses.add(scCourses, BorderLayout.CENTER);

        // ---------- Sections Tab ----------
        JPanel pSections = new JPanel(new BorderLayout());
        pSections.setOpaque(false);

        JPanel secForm = new JPanel(new GridBagLayout());
        secForm.setOpaque(false);
        GridBagConstraints sg = new GridBagConstraints();
        sg.insets = new Insets(6, 8, 6, 8);
        sg.anchor = GridBagConstraints.WEST;

        int sr = 0;
        sg.gridx = 0; sg.gridy = sr;
        secForm.add(new JLabel("Course:"), sg);
        sg.gridx = 1; secForm.add(cbCourses, sg);

        sg.gridx = 2; secForm.add(new JLabel("Instructor:"), sg);
        sg.gridx = 3; secForm.add(cbInstructors, sg);

        sr++;
        sg.gridy = sr; sg.gridx = 0;
        secForm.add(new JLabel("Day/Time:"), sg);
        sg.gridx = 1; secForm.add(txtDayTime, sg);

        sg.gridx = 2; secForm.add(new JLabel("Room:"), sg);
        sg.gridx = 3; secForm.add(txtRoom, sg);

        sr++;
        sg.gridy = sr; sg.gridx = 0;
        secForm.add(new JLabel("Capacity:"), sg);
        sg.gridx = 1; secForm.add(txtCapacity, sg);
        installCapacityFilter(txtCapacity);


        sg.gridx = 2; secForm.add(new JLabel("Semester:"), sg);
        sg.gridx = 3; secForm.add(txtSemester, sg);

        sr++;
        sg.gridy = sr; sg.gridx = 0;
        secForm.add(new JLabel("Year:"), sg);
        sg.gridx = 1; secForm.add(txtYear, sg);

        // NOTE: upper drop-deadline spinner removed on purpose (user requested)

        JPanel secBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        secBtns.setOpaque(false);
        secBtns.add(btnSectionAdd);
        secBtns.add(btnSectionRefresh);
        secBtns.add(btnSectionDelete); // added to UI
        sr++;
        sg.gridx = 0; sg.gridy = sr; sg.gridwidth = 4;
        secForm.add(secBtns, sg);

        pSections.add(secForm, BorderLayout.NORTH);

        sectionsModel = new DefaultTableModel(new String[]{
                "Section ID","Course ID","Course Code","Instructor ID","Instructor","Day/Time","Room","Capacity","Semester","Year","Drop Deadline"
        }, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        sectionsTable = new JTable(sectionsModel);
        sectionsTable.setAutoCreateRowSorter(true);
        sectionsTable.setRowHeight(26);

        // double-click section -> open SectionEditorDialog (edit and assign instructor)
        sectionsTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                int viewRow = sectionsTable.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = sectionsTable.convertRowIndexToModel(viewRow);
                Object sectionIdObj = sectionsTable.getModel().getValueAt(modelRow, 0); // Section ID column index 0
                final long sectionId;
                try {
                    sectionId = (sectionIdObj instanceof Number) ? ((Number) sectionIdObj).longValue() : Long.parseLong(String.valueOf(sectionIdObj));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Cannot open editor: invalid section id", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Window owner = SwingUtilities.getWindowAncestor(AdminCourseSectionPanel.this);
                SectionEditorDialog sed = new SectionEditorDialog(owner, sectionId);
                sed.setLocationRelativeTo(owner);
                sed.setModal(true);
                sed.setVisible(true);
                // refresh
                loadSectionsAndFillCombos();
                loadCourses();
            }
        });

        JScrollPane scSections = new JScrollPane(sectionsTable);
        scSections.setPreferredSize(new Dimension(900, 320));
        pSections.add(scSections, BorderLayout.CENTER);

        // add tabs
        tabs.addTab("Courses", pCourses);
        tabs.addTab("Sections", pSections);

        add(tabs, BorderLayout.CENTER);

        // wire actions
        btnCourseRefresh.addActionListener(e -> loadCourses());
        btnCourseAdd.addActionListener(e -> addCourse());
        btnCourseDelete.addActionListener(e -> deleteCourse()); // new

        btnSectionRefresh.addActionListener(e -> loadSectionsAndFillCombos());
        btnSectionAdd.addActionListener(e -> addSection());

        // initial load
        SwingUtilities.invokeLater(() -> {
            loadCourses();
            loadSectionsAndFillCombos();
        });
    }

    // --- Helper types for combobox entries ---
    private static class CourseEntry {
        final long id;
        final String code;
        CourseEntry(long id, String code) { this.id = id; this.code = code; }
        @Override public String toString() { return code + " (" + id + ")"; }
    }
    private static class InstructorEntry {
        final Long id; // nullable -> Null means "None"
        final String name;
        InstructorEntry(Long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return (id == null ? "None" : name + " (" + id + ")"); }
    }

    // allow only digits (no minus, no letters). Empty string allowed.
private static void installCapacityFilter(final JTextField fld) {
    AbstractDocument doc = (AbstractDocument) fld.getDocument();
    doc.setDocumentFilter(new DocumentFilter() {
        private String filterDigits(String s) {
            if (s == null) return null;
            StringBuilder b = new StringBuilder();
            for (char c : s.toCharArray()) if (Character.isDigit(c)) b.append(c);
            return b.toString();
        }
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            super.insertString(fb, offset, filterDigits(string), attr);
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            super.replace(fb, offset, length, filterDigits(text), attrs);
        }
    });
}




    // ------------------ Courses ------------------
    private void loadCourses() {
        btnCourseRefresh.setEnabled(false);
        coursesModel.setRowCount(0);
        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                String sql = "SELECT course_id, code, title, credits FROM courses ORDER BY course_id ASC";
                try (Connection conn = DBConnection.getErpConnection();
                     PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Object id = rs.getObject("course_id");
                        Object code = rs.getObject("code");
                        Object title = rs.getObject("title");
                        Object credits = rs.getObject("credits");
                        coursesModel.addRow(new Object[]{id, code, title, credits});
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }
            @Override protected void done() {
                btnCourseRefresh.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Failed to load courses: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ----------------- Deletion helpers: show info if students are enrolled, allow proceed (force delete) -----------------

// Entry returned when an enrollment table + column is detected

/**
 * Called when Delete Section button is pressed.
 * Shows a message if students are enrolled, asks whether to continue. If admin confirms,
 * deletes enrollment rows (if detected) then deletes section in a transaction.
 */
private void deleteSelectedSection() {
    int viewRow = sectionsTable.getSelectedRow();
    if (viewRow < 0) {
        JOptionPane.showMessageDialog(this, "Select a section to delete");
        return;
    }
    int modelRow = sectionsTable.convertRowIndexToModel(viewRow);
    Object idObj = sectionsModel.getValueAt(modelRow, 0);
    final long sectionId;
    try {
        sectionId = (idObj instanceof Number) ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj));
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Invalid section id");
        return;
    }

    // Attempt to count enrolled students (returns null if detection failed)
    Integer enrolledCount = null;
    Exception detectErr = null;
    try (Connection conn = DBConnection.getErpConnection()) {
        enrolledCount = findEnrolledCount(conn, sectionId);
    } catch (Exception ex) {
        detectErr = ex;
    }

    String msg;
    if (detectErr != null) {
        msg = "Could not determine whether students are enrolled in this section (error: " + detectErr.getMessage() + ").\n" +
              "Proceeding may remove the section while enrollments (if any) remain. Do you want to continue and delete the section?";
    } else if (enrolledCount == null) {
        msg = "Could not detect enrollment table. Proceed to delete the section? (Enrollments could exist and won't be removed automatically.)";
    } else if (enrolledCount > 0) {
        msg = "There are currently " + enrolledCount + " student(s) enrolled in this section.\n" +
              "If you continue we will remove detected enrollment rows for this section and then delete the section.\n\n" +
              "Do you want to continue and delete the section?";
    } else {
        msg = "No students appear to be enrolled in this section. Delete the section?";
    }

    int ok = JOptionPane.showConfirmDialog(this, msg, "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (ok != JOptionPane.YES_OPTION) return;

    // proceed with deletion (background)
    performDeleteSectionWithOptionalEnrollmentCleanup(sectionId);
}

/**
 * Performs deletion in background: removes enrollment rows for section (if table detected) and deletes section.
 * All done inside a transaction for safety.
 */
private void performDeleteSectionWithOptionalEnrollmentCleanup(long sectionId) {
    btnSectionDelete.setEnabled(false);
    new SwingWorker<Void, Void>() {
        Exception err = null;
        @Override protected Void doInBackground() {
            Connection conn = null;
            PreparedStatement psDelEnroll = null;
            PreparedStatement psDelSection = null;
            try {
                conn = DBConnection.getErpConnection();
                conn.setAutoCommit(false);

                EnrollmentRef ref = detectEnrollmentTable(conn);
                if (ref != null) {
                    // remove enrollments referencing this section
                    String delEnrollSql = "DELETE FROM " + ref.tableName + " WHERE " + ref.sectionColumn + " = ?";
                    psDelEnroll = conn.prepareStatement(delEnrollSql);
                    psDelEnroll.setLong(1, sectionId);
                    psDelEnroll.executeUpdate();
                }

                // delete the section
                String delSectionSql = "DELETE FROM sections WHERE section_id = ?";
                psDelSection = conn.prepareStatement(delSectionSql);
                psDelSection.setLong(1, sectionId);
                int affected = psDelSection.executeUpdate();
                if (affected == 0) throw new SQLException("Section not found or already deleted.");

                conn.commit();
            } catch (Exception ex) {
                err = ex;
                if (conn != null) {
                    try { conn.rollback(); } catch (Exception ignore) {}
                }
            } finally {
                try { if (psDelEnroll != null) psDelEnroll.close(); } catch (Exception ignore) {}
                try { if (psDelSection != null) psDelSection.close(); } catch (Exception ignore) {}
                try { if (conn != null) conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { if (conn != null) conn.close(); } catch (Exception ignore) {}
            }
            return null;
        }

        @Override protected void done() {
            btnSectionDelete.setEnabled(true);
            if (err != null) {
                JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Failed to delete section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                loadSectionsAndFillCombos();
                loadCourses();
            }
        }
    }.execute();
}

/**
 * Detects a likely enrollment/registration table and returns EnrollmentRef(tableName, sectionColumn)
 * or null if none of the candidate tables/columns are found.
 */
private EnrollmentRef detectEnrollmentTable(Connection conn) throws SQLException {
    DatabaseMetaData md = conn.getMetaData();
    String[] candidateTables = new String[] {
            "registrations", "enrollments", "student_enrollments", "student_sections", "section_registrations", "course_registrations"
    };
    String[] candidateCols = new String[] { "section_id", "sec_id", "sectionid", "section" };

    // scan tables
    for (String t : candidateTables) {
        // try direct match first (some DBs are case-sensitive)
        try (ResultSet rs = md.getTables(null, null, t, new String[] {"TABLE"})) {
            if (rs.next()) {
                // found table; find matching column
                try (ResultSet cols = md.getColumns(null, null, t, "%")) {
                    java.util.List<String> available = new java.util.ArrayList<>();
                    while (cols.next()) available.add(cols.getString("COLUMN_NAME").toLowerCase());
                    for (String candCol : candidateCols) {
                        if (available.contains(candCol.toLowerCase())) {
                            // return exact column name
                            try (ResultSet cols2 = md.getColumns(null, null, t, "%")) {
                                while (cols2.next()) {
                                    String cn = cols2.getString("COLUMN_NAME");
                                    if (cn != null && cn.equalsIgnoreCase(candCol)) return new EnrollmentRef(t, cn);
                                }
                            }
                        }
                    }
                }
            } else {
                // try case-insensitive scan of all tables (fallback)
                try (ResultSet all = md.getTables(null, null, "%", new String[] {"TABLE"})) {
                    boolean matched = false;
                    String realName = null;
                    while (all.next()) {
                        String name = all.getString("TABLE_NAME");
                        if (name != null && name.equalsIgnoreCase(t)) { matched = true; realName = name; break; }
                    }
                    if (matched && realName != null) {
                        try (ResultSet cols = md.getColumns(null, null, realName, "%")) {
                            java.util.List<String> available = new java.util.ArrayList<>();
                            while (cols.next()) available.add(cols.getString("COLUMN_NAME").toLowerCase());
                            for (String candCol : candidateCols) {
                                if (available.contains(candCol.toLowerCase())) {
                                    try (ResultSet cols2 = md.getColumns(null, null, realName, "%")) {
                                        while (cols2.next()) {
                                            String cn = cols2.getString("COLUMN_NAME");
                                            if (cn != null && cn.equalsIgnoreCase(candCol)) return new EnrollmentRef(realName, cn);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return null;
}

/**
 * Count enrolled students for a section by probing detected enrollment table.
 * Returns:
 *   - integer count (0..n) if detection & query successful
 *   - null if detection failed (we couldn't locate a candidate enrollment table)
 */
private Integer findEnrolledCount(Connection conn, long sectionId) {
    try {
        EnrollmentRef ref = detectEnrollmentTable(conn);
        if (ref == null) return null; // unknown
        String cntSql = "SELECT COUNT(*) FROM " + ref.tableName + " WHERE " + ref.sectionColumn + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(cntSql)) {
            ps.setLong(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
    } catch (SQLException ex) {
        return null;
    }
    return 0;
}


    private void addCourse() {
        final String code = txtCourseCode.getText().trim();
        final String title = txtCourseTitle.getText().trim();
        final String creditsStr = txtCourseCredits.getText().trim();

        if (code.isEmpty() && title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Provide course code or title");
            return;
        }

        // parse credits - allow empty
        final Double creditsVal;
        if (creditsStr.isEmpty()) creditsVal = null;
        else {
            try { creditsVal = Double.parseDouble(creditsStr); } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid credits value"); return;
            }
        }

        btnCourseAdd.setEnabled(false);

        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                String sql = "INSERT INTO courses (code, title, credits, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
                try (Connection conn = DBConnection.getErpConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, code.isEmpty() ? null : code);
                    ps.setString(2, title.isEmpty() ? null : title);
                    if (creditsVal == null) ps.setNull(3, Types.DECIMAL);
                    else ps.setDouble(3, creditsVal);
                    ps.executeUpdate();
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }
            @Override protected void done() {
                btnCourseAdd.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Failed to add course: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    txtCourseCode.setText(""); txtCourseTitle.setText(""); txtCourseCredits.setText("");
                    loadCourses();
                    loadSectionsAndFillCombos(); // refresh courses dropdown in sections tab
                }
            }
        }.execute();
    }

    // new: delete selected course
    // new deleteCourse() - deletes sections for the course first, then deletes the course in one transaction
    private void deleteCourse() {
        int viewRow = coursesTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a course to delete");
            return;
        }
        int modelRow = coursesTable.convertRowIndexToModel(viewRow);
        Object idObj = coursesModel.getValueAt(modelRow, 0);
        final long courseId;
        try { courseId = (idObj instanceof Number) ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj)); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid course id"); return; }

        int ok = JOptionPane.showConfirmDialog(this,
                "Delete the course and ALL its sections? This will permanently remove all sections belonging to this course.",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        btnCourseDelete.setEnabled(false);
        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                Connection conn = null;
                PreparedStatement psDelSections = null;
                PreparedStatement psDelCourse = null;
                try {
                    conn = DBConnection.getErpConnection();
                    conn.setAutoCommit(false); // start txn

                    // 1) delete sections belonging to course
                    String delSectionsSql = "DELETE FROM sections WHERE course_id = ?";
                    psDelSections = conn.prepareStatement(delSectionsSql);
                    psDelSections.setLong(1, courseId);
                    psDelSections.executeUpdate();

                    // 2) delete course
                    String delCourseSql = "DELETE FROM courses WHERE course_id = ?";
                    psDelCourse = conn.prepareStatement(delCourseSql);
                    psDelCourse.setLong(1, courseId);
                    int affected = psDelCourse.executeUpdate();
                    if (affected == 0) throw new SQLException("Course not found or already deleted.");

                    conn.commit();
                } catch (Exception ex) {
                    err = ex;
                    if (conn != null) {
                        try { conn.rollback(); } catch (Exception ignore) {}
                    }
                } finally {
                    try { if (psDelSections != null) psDelSections.close(); } catch (Exception ignore) {}
                    try { if (psDelCourse != null) psDelCourse.close(); } catch (Exception ignore) {}
                    try { if (conn != null) conn.setAutoCommit(true); } catch (Exception ignore) {}
                    try { if (conn != null) conn.close(); } catch (Exception ignore) {}
                }
                return null;
            }
            @Override protected void done() {
                btnCourseDelete.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Failed to delete course: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    loadCourses();
                    loadSectionsAndFillCombos();
                }
            }
        }.execute();
    }


    // ------------------ Sections & combos ------------------
    // ------------------ Sections & combos (Course + Instructor filters) ------------------
    private void loadSectionsAndFillCombos() {
        btnSectionRefresh.setEnabled(false);
        sectionsModel.setRowCount(0);
        cbCourses.removeAllItems();
        cbInstructors.removeAllItems();

        new SwingWorker<LoadDataBundle, Void>() {
            Exception err = null;

            @Override protected LoadDataBundle doInBackground() {
                LoadDataBundle bundle = new LoadDataBundle();
                try (Connection conn = DBConnection.getErpConnection()) {
                    // courses (add <All Courses>)
                    String qCourses = "SELECT course_id, code FROM courses ORDER BY course_id";
                    try (PreparedStatement pc = conn.prepareStatement(qCourses);
                         ResultSet rc = pc.executeQuery()) {
                        bundle.courses.add(new CourseEntry(0L, "<All Courses>"));
                        while (rc.next()) {
                            long id = rc.getLong("course_id");
                            String code = rc.getString("code");
                            bundle.courses.add(new CourseEntry(id, code == null ? ("#" + id) : code));
                        }
                    }

                    // instructors (add None/All)
                    bundle.instructors.add(new InstructorEntry(null, "<All Instructors>"));
                    String qInstr = "SELECT instructor_id, full_name FROM instructors ORDER BY instructor_id";
                    try (PreparedStatement pi = conn.prepareStatement(qInstr);
                         ResultSet ri = pi.executeQuery()) {
                        while (ri.next()) {
                            long id = ri.getLong("instructor_id");
                            String name = ri.getString("full_name");
                            bundle.instructors.add(new InstructorEntry(id, name == null ? ("#" + id) : name));
                        }
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return bundle;
            }

            @Override protected void done() {
                btnSectionRefresh.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Failed to load sections/courses: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    LoadDataBundle bundle = get();
                    cbCourses.removeAllItems();
                    for (CourseEntry c : bundle.courses) cbCourses.addItem(c);

                    cbInstructors.removeAllItems();
                    for (InstructorEntry i : bundle.instructors) cbInstructors.addItem(i);

                    // ensure single listeners
                    safeSetListenerOnCoursesCombo();
                    safeSetListenerOnInstructorsCombo();

                    // initial load: show all
                    loadSections(null, null);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Internal error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void safeSetListenerOnCoursesCombo() {
        for (java.awt.event.ActionListener al : cbCourses.getActionListeners()) {
            cbCourses.removeActionListener(al);
        }
        cbCourses.addActionListener(ev -> {
            CourseEntry selCourse = (CourseEntry) cbCourses.getSelectedItem();
            Long courseId = (selCourse == null || selCourse.id == 0L) ? null : selCourse.id;

            InstructorEntry selInstr = (InstructorEntry) cbInstructors.getSelectedItem();
            Long instrId = (selInstr == null || selInstr.id == null) ? null : selInstr.id;

            loadSections(courseId, instrId);
        });
    }
    private void safeSetListenerOnInstructorsCombo() {
        for (java.awt.event.ActionListener al : cbInstructors.getActionListeners()) {
            cbInstructors.removeActionListener(al);
        }
        cbInstructors.addActionListener(ev -> {
            CourseEntry selCourse = (CourseEntry) cbCourses.getSelectedItem();
            Long courseId = (selCourse == null || selCourse.id == 0L) ? null : selCourse.id;

            InstructorEntry selInstr = (InstructorEntry) cbInstructors.getSelectedItem();
            Long instrId = (selInstr == null || selInstr.id == null || "<All Instructors>".equals(selInstr.name)) ? null : selInstr.id;

            loadSections(courseId, instrId);
        });
    }

    /**
     * Load sections, optionally filtering by courseId and/or instructorId.
     * If courseId == null -> all courses. If instrId == null -> all instructors.
     */
    private void loadSections(Long courseId, Long instrId) {
        btnSectionRefresh.setEnabled(false);
        sectionsModel.setRowCount(0);

        new SwingWorker<java.util.List<Object[]>, Void>() {
            Exception err = null;

            @Override protected java.util.List<Object[]> doInBackground() {
                java.util.List<Object[]> rows = new java.util.ArrayList<>();
                StringBuilder q = new StringBuilder(
"SELECT s.section_id, s.course_id, c.code AS course_code, " +
"s.instructor_id, i.full_name AS instructor_name, s.day_time, s.room, s.capacity, s.semester, s.year, s.drop_deadline " +
"FROM sections s LEFT JOIN courses c ON s.course_id = c.course_id " +
"LEFT JOIN instructors i ON s.instructor_id = i.instructor_id "
                );

                boolean whereAdded = false;
                if (courseId != null) {
                    q.append(" WHERE s.course_id = ? ");
                    whereAdded = true;
                }
                if (instrId != null) {
                    q.append(whereAdded ? " AND " : " WHERE ");
                    q.append(" s.instructor_id = ? ");
                }
                q.append(" ORDER BY s.section_id");

                try (Connection conn = DBConnection.getErpConnection();
                     PreparedStatement ps = conn.prepareStatement(q.toString())) {
                    int idx = 1;
                    if (courseId != null) ps.setLong(idx++, courseId);
                    if (instrId != null) ps.setLong(idx++, instrId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Object sectionId = rs.getObject("section_id");
                            Object cId = rs.getObject("course_id");
                            Object courseCode = rs.getObject("course_code");
                            Object instrIdObj = rs.getObject("instructor_id");
                            Object instrName = rs.getObject("instructor_name");
                            Object dayTime = rs.getObject("day_time");
                            Object room = rs.getObject("room");
                            Object cap = rs.getObject("capacity");
                            Object sem = rs.getObject("semester");
                            Object year = rs.getObject("year");

                            Timestamp ddlTs = rs.getTimestamp("drop_deadline"); // may be null
                            String ddlStr = "";
                            if (ddlTs != null) {
                                ddlStr = dropFmt.format(new java.util.Date(ddlTs.getTime())); // date-only
                            }

                            rows.add(new Object[]{ sectionId, cId, courseCode, instrIdObj, instrName, dayTime, room, cap, sem, year, ddlStr });
                        }
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return rows;
            }

            @Override protected void done() {
                btnSectionRefresh.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Failed to load sections: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    java.util.List<Object[]> rows = get();
                    SwingUtilities.invokeLater(() -> {
                        sectionsModel.setRowCount(0);
                        for (Object[] r : rows) sectionsModel.addRow(r);
                    });
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Internal error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // small holder for data transferred from background worker
    private static class LoadDataBundle {
        final java.util.List<CourseEntry> courses = new java.util.ArrayList<>();
        final java.util.List<InstructorEntry> instructors = new java.util.ArrayList<>();
    }
    
private void addSection() {
    final CourseEntry selectedCourse = (CourseEntry) cbCourses.getSelectedItem();
    final InstructorEntry selectedInstructor = (InstructorEntry) cbInstructors.getSelectedItem();
    final String dayTime = txtDayTime.getText().trim();
    final String room = txtRoom.getText().trim();
    final String capStr = txtCapacity.getText().trim();
    final String sem = txtSemester.getText().trim();
    final String yearStr = txtYear.getText().trim();

    if (selectedCourse == null) { JOptionPane.showMessageDialog(this, "Select a course"); return; }

    // --- validate capacity (must be integer >= 0) ---
    final Integer capVal;
    if (capStr.isEmpty()) {
        capVal = null;
    } else {
        Integer tmp;
        try {
            tmp = Integer.parseInt(capStr);
            if (tmp < 0) {
                JOptionPane.showMessageDialog(this, "Capacity cannot be negative.", "Validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid capacity. Enter a whole number.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        capVal = tmp;
    }

    // --- validate year (optional but if present must be reasonable) ---
    final Integer yearVal;
    if (yearStr.isEmpty()) {
        yearVal = null;
    } else {
        Integer tmp;
        try {
            tmp = Integer.parseInt(yearStr);
            if (tmp < 1900 || tmp > 2100) {
                JOptionPane.showMessageDialog(this, "Enter a valid year between 1900 and 2100.", "Validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid year. Enter a number like 2024.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        yearVal = tmp;
    }

    final Long instrId = (selectedInstructor == null || selectedInstructor.id == null) ? null : selectedInstructor.id;
    final long courseId = selectedCourse.id;

    // capture spinner snapshot here so background thread doesn't access UI components directly
    final Object spinnerSnapshot = spDropDeadline == null ? null : spDropDeadline.getValue();

    btnSectionAdd.setEnabled(false);

    new SwingWorker<Void, Void>() {
        Exception err = null;
        @Override protected Void doInBackground() {
            String sql = "INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year, drop_deadline, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            try (Connection conn = DBConnection.getErpConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                int idx = 1;

                ps.setLong(idx++, courseId);

                if (instrId == null) ps.setNull(idx++, Types.BIGINT);
                else ps.setLong(idx++, instrId);

                if (dayTime.isEmpty()) ps.setNull(idx++, Types.VARCHAR);
                else ps.setString(idx++, dayTime);

                if (room.isEmpty()) ps.setNull(idx++, Types.VARCHAR);
                else ps.setString(idx++, room);

                if (capVal == null) ps.setNull(idx++, Types.INTEGER);
                else ps.setInt(idx++, capVal);

                if (sem.isEmpty()) ps.setNull(idx++, Types.VARCHAR);
                else ps.setString(idx++, sem);

                if (yearVal == null) ps.setNull(idx++, Types.INTEGER);
                else ps.setInt(idx++, yearVal);

                // drop_deadline: store DATE only (if spinnerSnapshot holds a Date)
                if (spinnerSnapshot instanceof java.util.Date) {
                    java.util.Date dt = (java.util.Date) spinnerSnapshot;
                    LocalDate localDate = dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    ps.setDate(idx++, java.sql.Date.valueOf(localDate));
                } else {
                    ps.setNull(idx++, Types.DATE);
                }

                ps.executeUpdate();

            } catch (SQLException ex) {
                err = ex;
            } catch (Exception ex) {
                err = ex;
            }
            return null;
        }

        @Override protected void done() {
            btnSectionAdd.setEnabled(true);
            if (err != null) {
                JOptionPane.showMessageDialog(AdminCourseSectionPanel.this, "Failed to add section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                txtDayTime.setText(""); txtRoom.setText(""); txtCapacity.setText(""); txtSemester.setText(""); txtYear.setText("");
                loadSectionsAndFillCombos();
                loadCourses();
            }
        }
    }.execute();
}


    // ------------------- Inner dialog for editing a single section -------------------
    // (unchanged from your version except spinner uses date-only editor format)
    private static class SectionEditorDialog extends JDialog {
        private final long sectionId;

        private final JTextField txtDayTime = new JTextField(16);
        private final JTextField txtRoom = new JTextField(12);
        private final JTextField txtCapacity = new JTextField(6);
        private final JTextField txtSemester = new JTextField(8);
        private final JTextField txtYear = new JTextField(6);

        // spinner only inside dialog; date-only editor
        private final JSpinner spDropDeadline = new JSpinner(new SpinnerDateModel());
        private final JComboBox<InstructorEntry> cbInstructors = new JComboBox<>();

        private final JButton btnSave = new JButton("Save");
        private final JButton btnCancel = new JButton("Cancel");

        SectionEditorDialog(Window owner, long sectionId) {
            super(owner, "Edit Section", ModalityType.APPLICATION_MODAL);
            this.sectionId = sectionId;

            setSize(520, 360);
            setLocationRelativeTo(owner);

            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(12,12,12,12));
            p.setBackground(Theme.BACKGROUND);

            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(6, 8, 6, 8);
            gc.anchor = GridBagConstraints.WEST;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;

            int row = 0;

            // Row 0 — Day/Time
            gc.gridx = 0; gc.gridy = row; p.add(new JLabel("Day/Time:"), gc);
            gc.gridx = 1; p.add(txtDayTime, gc);

            // Row 1 — Room
            row++;
            gc.gridx = 0; gc.gridy = row; p.add(new JLabel("Room:"), gc);
            gc.gridx = 1; p.add(txtRoom, gc);

            // Row 2 — Capacity
            row++;
            gc.gridx = 0; gc.gridy = row; p.add(new JLabel("Capacity:"), gc);
            gc.gridx = 1; p.add(txtCapacity, gc);
            AdminCourseSectionPanel.installCapacityFilter(txtCapacity);

            


            // Row 3 — Semester
            row++;
            gc.gridx = 0; gc.gridy = row; p.add(new JLabel("Semester:"), gc);
            gc.gridx = 1; p.add(txtSemester, gc);

            // Row 4 — Year
            row++;
            gc.gridx = 0; gc.gridy = row; p.add(new JLabel("Year:"), gc);
            gc.gridx = 1; p.add(txtYear, gc);

            // Row 5 — Drop Deadline (date only)
            row++;
            gc.gridx = 0; gc.gridy = row; p.add(new JLabel("Drop Deadline:"), gc);
            gc.gridx = 1;
            spDropDeadline.setEditor(new JSpinner.DateEditor(spDropDeadline, "yyyy-MM-dd"));
            p.add(spDropDeadline, gc);

            // Row 6 — Instructor
            row++;
            gc.gridx = 0; gc.gridy = row; p.add(new JLabel("Instructor:"), gc);
            gc.gridx = 1; p.add(cbInstructors, gc);

            // Row 7 — Buttons
            row++;
            JPanel br = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            br.setOpaque(false);
            br.add(btnSave);
            br.add(btnCancel);

            gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.anchor = GridBagConstraints.EAST;
            p.add(br, gc);

            setContentPane(p);

            btnCancel.addActionListener(e -> dispose());
            btnSave.addActionListener(e -> saveSection());

            // load instructors then section data
            SwingUtilities.invokeLater(this::loadInstructors);
        }

        private void loadInstructors() {
            cbInstructors.removeAllItems();
            cbInstructors.addItem(new InstructorEntry(null, "<No Instructor>"));

            new SwingWorker<Void, Void>() {
                Exception err = null;
                java.util.List<InstructorEntry> loaded = new java.util.ArrayList<>();

                @Override protected Void doInBackground() {
                    String q = "SELECT instructor_id, full_name FROM instructors ORDER BY instructor_id";
                    try (Connection conn = DBConnection.getErpConnection();
                         PreparedStatement ps = conn.prepareStatement(q);
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("instructor_id");
                            String name = rs.getString("full_name");
                            loaded.add(new InstructorEntry(id, name == null ? "#" + id : name));
                        }
                    } catch (Exception ex) {
                        err = ex;
                    }
                    return null;
                }

                @Override protected void done() {
                    if (err != null) {
                        JOptionPane.showMessageDialog(SectionEditorDialog.this,
                                "Failed to load instructors: " + err.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        for (InstructorEntry ie : loaded) cbInstructors.addItem(ie);
                    }
                    // now load the section row (we have instructor list available to select)
                    loadSection();
                }
            }.execute();
        }

        private void loadSection() {
            new SwingWorker<Void, Void>() {
                Exception err = null;
                Object instrIdObj = null;
                Timestamp ddlTs = null;
                String dt = null, room = null, sem = null;
                Object cap = null, year = null;

                @Override protected Void doInBackground() {
                    String q = "SELECT day_time, room, capacity, semester, year, instructor_id, drop_deadline FROM sections WHERE section_id = ?";
                    try (Connection conn = DBConnection.getErpConnection();
                         PreparedStatement ps = conn.prepareStatement(q)) {
                        ps.setLong(1, sectionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                dt = rs.getString("day_time");
                                room = rs.getString("room");
                                cap = rs.getObject("capacity");
                                sem = rs.getString("semester");
                                year = rs.getObject("year");
                                instrIdObj = rs.getObject("instructor_id");
                                ddlTs = rs.getTimestamp("drop_deadline");
                            } else {
                                err = new SQLException("Section not found");
                            }
                        }
                    } catch (Exception ex) { err = ex; }
                    return null;
                }

                @Override protected void done() {
                    if (err != null) {
                        JOptionPane.showMessageDialog(SectionEditorDialog.this,
                                "Failed to load section: " + err.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    // update UI once (on EDT)
                    txtDayTime.setText(dt == null ? "" : dt);
                    txtRoom.setText(room == null ? "" : room);
                    txtCapacity.setText(cap == null ? "" : String.valueOf(cap));
                    txtSemester.setText(sem == null ? "" : sem);
                    txtYear.setText(year == null ? "" : String.valueOf(year));

                    if (ddlTs != null) {
                        spDropDeadline.setValue(new java.util.Date(ddlTs.getTime()));
                    }

                    // select instructor in combo (if present)
                    if (instrIdObj == null) {
                        if (cbInstructors.getItemCount() > 0) cbInstructors.setSelectedIndex(0);
                    } else {
                        Long iidVal = (instrIdObj instanceof Number) ? ((Number) instrIdObj).longValue() : Long.parseLong(String.valueOf(instrIdObj));
                        for (int i = 0; i < cbInstructors.getItemCount(); ++i) {
                            InstructorEntry it = cbInstructors.getItemAt(i);
                            if (it != null && it.id != null && it.id.equals(iidVal)) {
                                cbInstructors.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
            }.execute();
        }

private void saveSection() {
    final String dayTime = txtDayTime.getText().trim();
    final String room = txtRoom.getText().trim();
    final String capStr = txtCapacity.getText().trim();
    final String sem = txtSemester.getText().trim();
    final String yearStr = txtYear.getText().trim();

    // validate capacity (integer >= 0) on EDT before disabling button / starting background worker
    final Integer capVal;
    if (capStr.isEmpty()) {
        capVal = null;
    } else {
        Integer tmp;
        try {
            tmp = Integer.parseInt(capStr);
            if (tmp < 0) {
                JOptionPane.showMessageDialog(SectionEditorDialog.this, "Capacity cannot be negative.", "Validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(SectionEditorDialog.this, "Capacity must be a whole non-negative number.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        capVal = tmp;
    }

    // validate year (optional)
    final Integer yearVal;
    if (yearStr.isEmpty()) {
        yearVal = null;
    } else {
        Integer tmp;
        try {
            tmp = Integer.parseInt(yearStr);
            if (tmp < 1900 || tmp > 2100) {
                JOptionPane.showMessageDialog(SectionEditorDialog.this, "Enter a valid year between 1900 and 2100.", "Validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(SectionEditorDialog.this, "Year must be a number.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        yearVal = tmp;
    }

    final InstructorEntry selInstr = (InstructorEntry) cbInstructors.getSelectedItem();
    final Long instrId = selInstr == null ? null : selInstr.id;

    // snapshot spinner value (so background thread does not read UI)
    final Object spinnerSnapshot = spDropDeadline == null ? null : spDropDeadline.getValue();

    btnSave.setEnabled(false);

    new SwingWorker<Void, Void>() {
        Exception err = null;

        @Override protected Void doInBackground() {
            String sql = "UPDATE sections SET day_time=?, room=?, capacity=?, semester=?, year=?, instructor_id=?, drop_deadline=?, updated_at=NOW() WHERE section_id=?";
            try (Connection conn = DBConnection.getErpConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // 1 day_time
                if (dayTime.isEmpty()) ps.setNull(1, Types.VARCHAR); else ps.setString(1, dayTime);
                // 2 room
                if (room.isEmpty()) ps.setNull(2, Types.VARCHAR); else ps.setString(2, room);
                // 3 capacity
                if (capVal == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, capVal);
                // 4 semester
                if (sem.isEmpty()) ps.setNull(4, Types.VARCHAR); else ps.setString(4, sem);
                // 5 year
                if (yearVal == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, yearVal);
                // 6 instructor_id
                if (instrId == null) ps.setNull(6, Types.BIGINT); else ps.setLong(6, instrId);

                // 7 drop_deadline: store date / timestamp from spinner snapshot
                if (spinnerSnapshot instanceof java.util.Date) {
                    java.util.Date dt = (java.util.Date) spinnerSnapshot;
                    // use date-only if your DB column is DATE; if TIMESTAMP keep Timestamp:
                    // If your DB column is TIMESTAMP use: ps.setTimestamp(7, new Timestamp(dt.getTime()));
                    ps.setTimestamp(7, new Timestamp(dt.getTime()));
                } else {
                    ps.setNull(7, Types.TIMESTAMP);
                }

                // 8 section id
                ps.setLong(8, sectionId);

                ps.executeUpdate();

            } catch (Exception ex) {
                err = ex;
            }
            return null;
        }

        @Override protected void done() {
            btnSave.setEnabled(true);
            if (err != null)
                JOptionPane.showMessageDialog(SectionEditorDialog.this,
                        "Failed to save section: " + err.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            else
                dispose();
        }
    }.execute();
}

    }
}
