package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;                // keep for Window, Dimension, GridBagConstraints, Insets, etc.
import java.sql.*;               // keep SQL imports

// explicit java.util imports to avoid ambiguity with java.awt.List
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;

/**
 * CourseEditorDialog
 * - Opens as modal and shows the course fields (editable) + sections table for that course.
 * - Allows editing course (basic fields) and adding/editing/deleting sections.
 *
 * Usage:
 *   Window owner = SwingUtilities.getWindowAncestor(parentPanel);
 *   CourseEditorDialog dlg = new CourseEditorDialog(owner, courseId);
 *   dlg.setVisible(true);
 */
public class CourseEditorDialog extends JDialog {

    private final long courseId;

    // course fields (only common ones)
    private final JTextField txtCode = new JTextField(12);
    private final JTextField txtTitle = new JTextField(30);
    private final JTextField txtCredits = new JTextField(6);

    // sections controls (kept final like before)
    private final DefaultTableModel sectionsModel;
    private final JTable sectionsTable;
    private final JButton btnAddSection = new JButton("Add Section");
    private final JButton btnEditSection = new JButton("Edit Section");
    private final JButton btnDeleteSection = new JButton("Delete Section");
    private final JButton btnSaveCourse = new JButton("Save Course");
    private final JButton btnClose = new JButton("Close");

    /**
     * Original one-arg constructor preserved for backward compatibility (shows full dialog).
     */
    public CourseEditorDialog(Window owner, long courseId) {
        this(owner, courseId, false);
    }

    /**
     * Overloaded constructor: when onlyCourseForm==true the dialog will show
     * only the top course form area (no sections table or section action buttons).
     *
     * This keeps DB logic intact and ensures final fields are initialized in all cases.
     */
    public CourseEditorDialog(Window owner, long courseId, boolean onlyCourseForm) {
        super(owner, "Edit Courses", ModalityType.APPLICATION_MODAL);
        this.courseId = courseId;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(920, 175);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(12,12,12,12));
        root.setBackground(Theme.BACKGROUND);
        setContentPane(root);

        // --- course form (extracted into a helper)
        JPanel courseForm = buildCourseFormPanel();
        root.add(courseForm, BorderLayout.NORTH);

        // initialize sectionsModel / table in both cases (final fields must be assigned)
        if (!onlyCourseForm) {
            // center: sections table + actions
            sectionsModel = new DefaultTableModel(new String[]{"Section ID","Section Code","Day/Time","Room","Capacity","Semester","Year","Instructor"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            sectionsTable = new JTable(sectionsModel);
            sectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            sectionsTable.setAutoCreateRowSorter(true); // allow sorting
            JScrollPane sc = new JScrollPane(sectionsTable);
            root.add(sc, BorderLayout.CENTER);

            JPanel secButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            secButtons.setOpaque(false);
            secButtons.add(btnAddSection);
            secButtons.add(btnEditSection);
            secButtons.add(btnDeleteSection);
            root.add(secButtons, BorderLayout.SOUTH);

            // actions for sections (same as original)
            btnAddSection.addActionListener(e -> openSectionEditor(-1));
            btnEditSection.addActionListener(e -> {
                int r = sectionsTable.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(this, "Select a section to edit");
                    return;
                }
                int modelRow = sectionsTable.convertRowIndexToModel(r);
                Object idv = sectionsModel.getValueAt(modelRow, 0);
                long sid = (idv instanceof Number) ? ((Number)idv).longValue() : Long.parseLong(String.valueOf(idv));
                openSectionEditor(sid);
            });
            btnDeleteSection.addActionListener(e -> deleteSelectedSection());

            // double click edit (same as original)
            sectionsTable.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int r = sectionsTable.getSelectedRow();
                        if (r >= 0) {
                            int modelRow = sectionsTable.convertRowIndexToModel(r);
                            Object idv = sectionsModel.getValueAt(modelRow, 0);
                            long sid = (idv instanceof Number) ? ((Number)idv).longValue() : Long.parseLong(String.valueOf(idv));
                            openSectionEditor(sid);
                        }
                    }
                }
            });
        } else {
            // onlyCourseForm == true -> create in-memory model/table but do NOT add to UI
            sectionsModel = new DefaultTableModel(new String[]{"Section ID","Section Code","Day/Time","Room","Capacity","Semester","Year","Instructor"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            sectionsTable = new JTable(sectionsModel);
            sectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            sectionsTable.setAutoCreateRowSorter(true);
            // no UI addition, no listeners attached for section actions
        }

        // actions for buttons that exist in both variants
        btnClose.addActionListener(e -> dispose());
        btnSaveCourse.addActionListener(e -> saveCourse());

        // load data (the loader is safe when sections area is absent)
        SwingUtilities.invokeLater(this::loadCourseAndSections);
    }

    /**
     * Builds and returns the top course form panel (same layout as original).
     * Extracted so we can show only this area when requested.
     */
    private JPanel buildCourseFormPanel() {
        JPanel courseForm = new JPanel(new GridBagLayout());
        courseForm.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,8,6,8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        courseForm.add(new JLabel("Course Code:"), gc);
        gc.gridx = 1; courseForm.add(txtCode, gc);

        gc.gridx = 2; courseForm.add(new JLabel("Title:"), gc);
        gc.gridx = 3; courseForm.add(txtTitle, gc);

        gc.gridx = 0; gc.gridy = 1;
        courseForm.add(new JLabel("Credits:"), gc);
        gc.gridx = 1; courseForm.add(txtCredits, gc);

        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 4;
        JPanel courseButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        courseButtons.setOpaque(false);
        courseButtons.add(btnSaveCourse);
        courseButtons.add(btnClose);
        courseForm.add(courseButtons, gc);

        return courseForm;
    }

    private void loadCourseAndSections() {
        btnSaveCourse.setEnabled(false);
        btnAddSection.setEnabled(false);
        btnEditSection.setEnabled(false);
        btnDeleteSection.setEnabled(false);

        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    // detect columns for courses table and pick common names
                    DatabaseMetaData md = conn.getMetaData();
                    String courseIdCol = detectCourseIdCol(md);

                    // load course row (we attempt to fetch typical columns)
                    String sql = "SELECT * FROM courses WHERE " + courseIdCol + " = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, courseId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                // pick likely columns
                                String[] codeCols = new String[]{"course_code","code","courseid","course"};
                                String[] titleCols = new String[]{"title","course_title","name"};
                                String[] creditCols = new String[]{"credits","credit"};

                                String code = findFirstString(rs, codeCols);
                                String title = findFirstString(rs, titleCols);
                                String cr = findFirstString(rs, creditCols);

                                SwingUtilities.invokeLater(() -> {
                                    txtCode.setText(code == null ? "" : code);
                                    txtTitle.setText(title == null ? "" : title);
                                    txtCredits.setText(cr == null ? "" : cr);
                                });
                            }
                        }
                    }

                    // load sections: we expect a sections table with course_id/section_id etc.
                    String secTbl = detectSectionsTable(md);
                    if (secTbl != null) {
                        // try to pick common column names
                        String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
                        String secCodeCol = detectColumn(md, secTbl, new String[]{"section_code","code","sec_code"});
                        String courseIdColInSec = detectColumn(md, secTbl, new String[]{"course_id","courseid","course"});
                        String timeCol = detectColumn(md, secTbl, new String[]{"day_time","time","schedule","slot"});
                        String roomCol = detectColumn(md, secTbl, new String[]{"room","location"});
                        String capCol = detectColumn(md, secTbl, new String[]{"capacity","cap"});
                        String semCol = detectColumn(md, secTbl, new String[]{"semester","term"});
                        String yearCol = detectColumn(md, secTbl, new String[]{"year"});
                        String instrCol = detectColumn(md, secTbl, new String[]{"instructor_id","instructorid","teacher_id"});

                        // prepare select fields; use aliases so we can read consistently
                        List<String> sel = new ArrayList<>();
                        sel.add((secIdCol != null ? secIdCol : "NULL") + " AS section_id");
                        sel.add((secCodeCol != null ? secCodeCol : "NULL") + " AS section_code");
                        sel.add((timeCol != null ? timeCol : "NULL") + " AS day_time");
                        sel.add((roomCol != null ? roomCol : "NULL") + " AS room");
                        sel.add((capCol != null ? capCol : "NULL") + " AS capacity");
                        sel.add((semCol != null ? semCol : "NULL") + " AS semester");
                        sel.add((yearCol != null ? yearCol : "NULL") + " AS year");
                        if (instrCol != null) sel.add(instrCol + " AS instructor_id");
                        else sel.add("NULL AS instructor_id");

                        StringBuilder q = new StringBuilder("SELECT ").append(String.join(", ", sel))
                                .append(" FROM ").append(secTbl)
                                .append(" WHERE ").append(courseIdColInSec).append(" = ?");

                        // order by section id if present
                        if (secIdCol != null) q.append(" ORDER BY ").append(secIdCol);

                        try (PreparedStatement ps = conn.prepareStatement(q.toString())) {
                            ps.setLong(1, courseId);
                            try (ResultSet rs = ps.executeQuery()) {
                                final List<Object[]> rows = new ArrayList<>();
                                while (rs.next()) {
                                    Object sid = rs.getObject("section_id");
                                    Object scode = rs.getObject("section_code");
                                    Object sched = rs.getObject("day_time");
                                    Object room = rs.getObject("room");
                                    Object cap = rs.getObject("capacity");
                                    Object sem = rs.getObject("semester");
                                    Object yr = rs.getObject("year");
                                    Object instrId = rs.getObject("instructor_id");
                                    rows.add(new Object[]{sid, scode, sched, room, cap, sem, yr, instrId});
                                }
                                SwingUtilities.invokeLater(() -> {
                                    synchronized (sectionsModel) {
                                        sectionsModel.setRowCount(0);
                                        for (Object[] r : rows) {
                                            // instructor id will be shown later as name in SectionEditor; here just show id if present
                                            Object instrDisplay = r[7] == null ? "" : String.valueOf(r[7]);
                                            sectionsModel.addRow(new Object[]{r[0], r[1], r[2], r[3], r[4], r[5], r[6], instrDisplay});
                                        }
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }
            @Override protected void done() {
                btnSaveCourse.setEnabled(true);
                btnAddSection.setEnabled(true);
                btnEditSection.setEnabled(true);
                btnDeleteSection.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(CourseEditorDialog.this, "Failed to load course/sections: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // save basic course fields back to DB
    private void saveCourse() {
        final String code = txtCode.getText().trim();
        final String title = txtTitle.getText().trim();
        final String credits = txtCredits.getText().trim();

        btnSaveCourse.setEnabled(false);
        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    DatabaseMetaData md = conn.getMetaData();
                    String courseIdCol = detectCourseIdCol(md);

                    // pick likely course column names
                    String codeCol = detectColumn(md, "courses", new String[]{"course_code","code","courseid","course"});
                    String titleCol = detectColumn(md, "courses", new String[]{"title","course_title","name"});
                    String creditsCol = detectColumn(md, "courses", new String[]{"credits","credit"});

                    StringBuilder update = new StringBuilder("UPDATE courses SET ");
                    java.util.List<String> sets = new java.util.ArrayList<>();
                    if (codeCol != null) sets.add(codeCol + " = ?");
                    if (titleCol != null) sets.add(titleCol + " = ?");
                    if (creditsCol != null) sets.add(creditsCol + " = ?");
                    if (sets.isEmpty()) throw new SQLException("No writable columns detected on courses table");

                    update.append(String.join(", ", sets));
                    update.append(" WHERE ").append(courseIdCol).append(" = ?");

                    try (PreparedStatement ps = conn.prepareStatement(update.toString())) {
                        int idx = 1;
                        if (codeCol != null) ps.setString(idx++, code.isEmpty()? null : code);
                        if (titleCol != null) ps.setString(idx++, title.isEmpty() ? null : title);
                        if (creditsCol != null) {
                            if (credits.isEmpty()) ps.setNull(idx++, Types.DECIMAL);
                            else ps.setBigDecimal(idx++, new java.math.BigDecimal(credits));
                        }
                        ps.setLong(idx, courseId);
                        ps.executeUpdate();
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }
            @Override protected void done() {
                btnSaveCourse.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(CourseEditorDialog.this, "Failed to save course: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(CourseEditorDialog.this, "Course saved");
                    loadCourseAndSections();
                }
            }
        }.execute();
    }

    // open section editor (sid == -1 for new)
    private void openSectionEditor(long sid) {
        SectionEditorDialog sed = new SectionEditorDialog(this, courseId, sid);
        sed.setVisible(true);
        // after closing, refresh table
        loadCourseAndSections();
    }

    private void deleteSelectedSection() {
        int r = sectionsTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a section to delete"); return; }
        int modelRow = sectionsTable.convertRowIndexToModel(r);
        Object idv = sectionsModel.getValueAt(modelRow, 0);
        long sid = (idv instanceof Number) ? ((Number)idv).longValue() : Long.parseLong(String.valueOf(idv));
        int ok = JOptionPane.showConfirmDialog(this, "Delete section?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    DatabaseMetaData md = conn.getMetaData();
                    String secTbl = detectSectionsTable(md);
                    String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
                    if (secTbl == null || secIdCol == null) throw new SQLException("Cannot find sections table or id column");
                    String delSql = "DELETE FROM " + secTbl + " WHERE " + secIdCol + " = ?";
                    try (PreparedStatement ps = conn.prepareStatement(delSql)) {
                        ps.setLong(1, sid);
                        ps.executeUpdate();
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }
            @Override protected void done() {
                if (err != null) JOptionPane.showMessageDialog(CourseEditorDialog.this, "Failed to delete section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                loadCourseAndSections();
            }
        }.execute();
    }

    // ------------------- small schema helpers -------------------
    private static String findFirstString(ResultSet rs, String[] candidates) throws SQLException {
        for (String c : candidates) {
            try {
                String v = null;
                try { v = rs.getString(c); } catch (SQLException ignore) { }
                if (v != null) return v;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String detectCourseIdCol(DatabaseMetaData md) throws SQLException {
        try (ResultSet rs = md.getColumns(null, null, "courses", null)) {
            while (rs.next()) {
                String c = rs.getString("COLUMN_NAME").toLowerCase();
                if ("course_id".equals(c) || "id".equals(c)) return rs.getString("COLUMN_NAME");
            }
        }
        return "course_id"; // fallback to sensible default
    }

    private static String detectSectionsTable(DatabaseMetaData md) throws SQLException {
        // look for common names: sections, course_sections
        try (ResultSet tables = md.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME").toLowerCase();
                if ("sections".equals(name) || "course_sections".equals(name) || "sections_tbl".equals(name)) return tables.getString("TABLE_NAME");
            }
        }
        // fallback to "sections" even if not found; higher-level code will fail cleanly if missing
        return "sections";
    }

    private static String detectColumn(DatabaseMetaData md, String table, String[] candidates) throws SQLException {
        if (table == null) return null;
        try (ResultSet rs = md.getColumns(null, null, table, null)) {
            while (rs.next()) {
                String c = rs.getString("COLUMN_NAME").toLowerCase();
                for (String s : candidates) if (s.equals(c)) return rs.getString("COLUMN_NAME");
            }
        } catch (SQLException ignored) {}
        return null;
    }

    // ------------------- SectionEditorDialog inner class -------------------
    private static class SectionEditorDialog extends JDialog {
        private final long courseId;
        private final long sectionId; // -1 for create

        private final JTextField txtSectionCode = new JTextField(12);
        private final JTextField txtDayTime = new JTextField(16);
        private final JTextField txtRoom = new JTextField(12);
        private final JTextField txtCapacity = new JTextField(6);
        private final JTextField txtSemester = new JTextField(8);
        private final JTextField txtYear = new JTextField(6);
        private final JComboBox<InstructorItem> cbInstructor = new JComboBox<>();
        private final JButton btnSave = new JButton("Save");
        private final JButton btnCancel = new JButton("Cancel");

        SectionEditorDialog(Window owner, long courseId, long sectionId) {
            super(owner, sectionId < 0 ? "Add Section" : "Edit Section", ModalityType.APPLICATION_MODAL);
            this.courseId = courseId;
            this.sectionId = sectionId;
            setSize(560, 320);
            setLocationRelativeTo(owner);

            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(12,12,12,12));
            p.setBackground(Theme.BACKGROUND);
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(6,8,6,8);
            gc.anchor = GridBagConstraints.WEST;

            gc.gridx = 0; gc.gridy = 0; p.add(new JLabel("Section Code:"), gc);
            gc.gridx = 1; p.add(txtSectionCode, gc);

            gc.gridx = 0; gc.gridy = 1; p.add(new JLabel("Day/Time:"), gc);
            gc.gridx = 1; p.add(txtDayTime, gc);

            gc.gridx = 0; gc.gridy = 2; p.add(new JLabel("Room:"), gc);
            gc.gridx = 1; p.add(txtRoom, gc);

            gc.gridx = 0; gc.gridy = 3; p.add(new JLabel("Capacity:"), gc);
            gc.gridx = 1; p.add(txtCapacity, gc);

            gc.gridx = 0; gc.gridy = 4; p.add(new JLabel("Semester:"), gc);
            gc.gridx = 1; p.add(txtSemester, gc);

            gc.gridx = 0; gc.gridy = 5; p.add(new JLabel("Year:"), gc);
            gc.gridx = 1; p.add(txtYear, gc);

            gc.gridx = 0; gc.gridy = 6; p.add(new JLabel("Instructor:"), gc);
            gc.gridx = 1; p.add(cbInstructor, gc);

            JPanel br = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
            br.setOpaque(false);
            br.add(btnSave); br.add(btnCancel);
            gc.gridx = 0; gc.gridy = 7; gc.gridwidth = 2; p.add(br, gc);

            setContentPane(p);

            btnCancel.addActionListener(e -> dispose());
            btnSave.addActionListener(e -> saveSection());

            // load instructors and (if editing) section data
            SwingUtilities.invokeLater(() -> {
                loadInstructors();
                if (sectionId >= 0) loadSection();
            });
        }

        private void loadInstructors() {
            // populate cbInstructor with valid instructors from erp_db.instructors
            new SwingWorker<List<InstructorItem>, Void>() {
                Exception err = null;
                @Override protected List<InstructorItem> doInBackground() {
                    List<InstructorItem> list = new ArrayList<>();
                    try (Connection conn = DBConnection.getErpConnection()) {
                        // detect columns
                        DatabaseMetaData md = conn.getMetaData();
                        String instrTbl = null;
                        try (ResultSet tables = md.getTables(null, null, "%", new String[]{"TABLE"})) {
                            while (tables.next()) {
                                String name = tables.getString("TABLE_NAME").toLowerCase();
                                if ("instructors".equals(name) || "instructor".equals(name) || "teachers".equals(name)) {
                                    instrTbl = tables.getString("TABLE_NAME");
                                    break;
                                }
                            }
                        }
                        if (instrTbl == null) {
                            // no instructors table found; return empty list but include <No Instructor>
                            list.add(new InstructorItem(null, "<No Instructor>"));
                            return list;
                        }

                        String idCol = detectColumn(md, instrTbl, new String[]{"instructor_id","id"});
                        String nameCol = detectColumn(md, instrTbl, new String[]{"full_name","name","first_name"});

                        String sql = "SELECT " + idCol + ", " + (nameCol != null ? nameCol : idCol) + " FROM " + instrTbl + " ORDER BY " + (nameCol != null ? nameCol : idCol);
                        try (PreparedStatement ps = conn.prepareStatement(sql);
                             ResultSet rs = ps.executeQuery()) {
                            list.add(new InstructorItem(null, "<No Instructor>"));
                            while (rs.next()) {
                                Long id = rs.getObject(1) == null ? null : rs.getLong(1);
                                String nm = rs.getString(2);
                                list.add(new InstructorItem(id, nm == null ? ("Instructor " + id) : nm));
                            }
                        }
                    } catch (Exception ex) {
                        err = ex;
                    }
                    if (list.isEmpty()) list.add(new InstructorItem(null, "<No Instructor>"));
                    return list;
                }
                @Override protected void done() {
                    try {
                        List<InstructorItem> list = get();
                        cbInstructor.removeAllItems();
                        for (InstructorItem it : list) cbInstructor.addItem(it);
                    } catch (Exception ex) {
                        cbInstructor.removeAllItems();
                        cbInstructor.addItem(new InstructorItem(null, "<No Instructor>"));
                    }
                }
            }.execute();
        }

        private void loadSection() {
            new SwingWorker<Void, Void>() {
                Exception err = null;
                @Override protected Void doInBackground() {
                    try (Connection conn = DBConnection.getErpConnection()) {
                        DatabaseMetaData md = conn.getMetaData();
                        String secTbl = detectSectionsTable(md);
                        String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
                        if (secTbl == null || secIdCol == null) throw new SQLException("Sections table/ID not found");

                        String sql = "SELECT * FROM " + secTbl + " WHERE " + secIdCol + " = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setLong(1, sectionId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    final String sCode = getFirst(rs, new String[]{"section_code","code"});
                                    final String sDay = getFirst(rs, new String[]{"day_time","time","schedule","slot"});
                                    final String sRoom = getFirst(rs, new String[]{"room","location"});
                                    final String sCap = getFirst(rs, new String[]{"capacity","cap"});
                                    final String sSem = getFirst(rs, new String[]{"semester","term"});
                                    final String sYear = getFirst(rs, new String[]{"year"});
                                    final Object instrObj = getObjectSafe(rs, new String[]{"instructor_id","instructorid","teacher_id"});

                                    SwingUtilities.invokeLater(() -> {
                                        txtSectionCode.setText(sCode);
                                        txtDayTime.setText(sDay);
                                        txtRoom.setText(sRoom);
                                        txtCapacity.setText(sCap);
                                        txtSemester.setText(sSem);
                                        txtYear.setText(sYear);

                                        // select instructor in combo
                                        if (instrObj == null) {
                                            // leave "<No Instructor>"
                                            cbInstructor.setSelectedIndex(0);
                                        } else {
                                            Long iid = (instrObj instanceof Number) ? ((Number) instrObj).longValue() : null;
                                            if (iid != null) {
                                                for (int i = 0; i < cbInstructor.getItemCount(); ++i) {
                                                    InstructorItem it = cbInstructor.getItemAt(i);
                                                    if (it != null && it.id != null && it.id.equals(iid)) {
                                                        cbInstructor.setSelectedIndex(i);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    throw new SQLException("Section not found");
                                }
                            }
                        }
                    } catch (Exception ex) {
                        err = ex;
                    }
                    return null;
                }
                @Override protected void done() {
                    if (err != null) JOptionPane.showMessageDialog(SectionEditorDialog.this, "Failed to load section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }.execute();
        }

        private void saveSection() {
            final String sCode = txtSectionCode.getText().trim();
            final String dayTime = txtDayTime.getText().trim();
            final String room = txtRoom.getText().trim();
            final String cap = txtCapacity.getText().trim();
            final String sem = txtSemester.getText().trim();
            final String year = txtYear.getText().trim();
            final InstructorItem instrSel = (InstructorItem) cbInstructor.getSelectedItem();
            final Long instrId = instrSel == null ? null : instrSel.id;

            btnSave.setEnabled(false);
            new SwingWorker<Void, Void>() {
                Exception err = null;
                @Override protected Void doInBackground() {
                    try (Connection conn = DBConnection.getErpConnection()) {
                        DatabaseMetaData md = conn.getMetaData();
                        String secTbl = detectSectionsTable(md);
                        if (secTbl == null) throw new SQLException("Sections table not found");

                        String courseIdCol = detectColumn(md, secTbl, new String[]{"course_id","courseid","course"});
                        if (courseIdCol == null) throw new SQLException("Sections table missing course_id column");

                        // pick columns
                        String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
                        String codeCol = detectColumn(md, secTbl, new String[]{"section_code","code"});
                        String timeCol = detectColumn(md, secTbl, new String[]{"day_time","time","schedule","slot"});
                        String roomCol = detectColumn(md, secTbl, new String[]{"room","location"});
                        String capCol = detectColumn(md, secTbl, new String[]{"capacity","cap"});
                        String semCol = detectColumn(md, secTbl, new String[]{"semester","term"});
                        String yearCol = detectColumn(md, secTbl, new String[]{"year"});
                        String instrCol = detectColumn(md, secTbl, new String[]{"instructor_id","instructorid","teacher_id"});

                        if (sectionId < 0) {
                            // insert
                            StringBuilder cols = new StringBuilder();
                            StringBuilder vals = new StringBuilder();
                            java.util.List<Object> params = new java.util.ArrayList<>();

                            // course id (required)
                            cols.append(courseIdCol); vals.append("?"); params.add(courseId);

                            if (codeCol != null) { cols.append(", ").append(codeCol); vals.append(", ?"); params.add(sCode.isEmpty()? null : sCode); }
                            if (timeCol != null) { cols.append(", ").append(timeCol); vals.append(", ?"); params.add(dayTime.isEmpty()? null : dayTime); }
                            if (roomCol != null) { cols.append(", ").append(roomCol); vals.append(", ?"); params.add(room.isEmpty()? null : room); }
                            if (capCol != null) { cols.append(", ").append(capCol); vals.append(", ?"); params.add(cap.isEmpty()? null : Integer.parseInt(cap)); }
                            if (semCol != null) { cols.append(", ").append(semCol); vals.append(", ?"); params.add(sem.isEmpty()? null : sem); }
                            if (yearCol != null) { cols.append(", ").append(yearCol); vals.append(", ?"); params.add(year.isEmpty()? null : Integer.parseInt(year)); }
                            if (instrCol != null) { cols.append(", ").append(instrCol); vals.append(", ?"); params.add(instrId); }

                            String sql = "INSERT INTO " + secTbl + " (" + cols.toString() + ") VALUES (" + vals.toString() + ")";
                            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                                for (int i=0;i<params.size();++i) {
                                    Object p = params.get(i);
                                    if (p == null) {
                                        ps.setNull(i+1, Types.VARCHAR);
                                    } else if (p instanceof Integer) {
                                        ps.setInt(i+1, (Integer)p);
                                    } else if (p instanceof Long) {
                                        ps.setLong(i+1, (Long)p);
                                    } else {
                                        ps.setString(i+1, String.valueOf(p));
                                    }
                                }
                                ps.executeUpdate();
                            }
                        } else {
                            // update
                            java.util.List<String> sets = new java.util.ArrayList<>();
                            java.util.List<Object> params = new java.util.ArrayList<>();
                            if (codeCol != null) { sets.add(codeCol + " = ?"); params.add(sCode.isEmpty()? null : sCode); }
                            if (timeCol != null) { sets.add(timeCol + " = ?"); params.add(dayTime.isEmpty()? null : dayTime); }
                            if (roomCol != null) { sets.add(roomCol + " = ?"); params.add(room.isEmpty()? null : room); }
                            if (capCol != null) { sets.add(capCol + " = ?"); params.add(cap.isEmpty()? null : Integer.parseInt(cap)); }
                            if (semCol != null) { sets.add(semCol + " = ?"); params.add(sem.isEmpty()? null : sem); }
                            if (yearCol != null) { sets.add(yearCol + " = ?"); params.add(year.isEmpty()? null : Integer.parseInt(year)); }
                            if (instrCol != null) { sets.add(instrCol + " = ?"); params.add(instrId); }

                            if (sets.isEmpty()) throw new SQLException("No writable columns on sections table");

                            String sql = "UPDATE " + secTbl + " SET " + String.join(", ", sets) + " WHERE " + secIdCol + " = ?";
                            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                                int idx = 1;
                                for (Object p : params) {
                                    if (p == null) ps.setNull(idx++, Types.VARCHAR);
                                    else if (p instanceof Integer) ps.setInt(idx++, (Integer)p);
                                    else if (p instanceof Long) ps.setLong(idx++, (Long)p);
                                    else ps.setString(idx++, String.valueOf(p));
                                }
                                ps.setLong(idx, sectionId);
                                ps.executeUpdate();
                            }
                        }
                    } catch (Exception ex) { err = ex; }
                    return null;
                }
                @Override protected void done() {
                    btnSave.setEnabled(true);
                    if (err != null) JOptionPane.showMessageDialog(SectionEditorDialog.this, "Failed to save section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    else dispose();
                }
            }.execute();
        }

        // small helpers
        private static String getFirst(ResultSet rs, String[] cand) {
            for (String c : cand) {
                try {
                    String v = rs.getString(c);
                    if (v != null) return v;
                } catch (SQLException ignored) {}
            }
            return "";
        }

        private static Object getObjectSafe(ResultSet rs, String[] cand) {
            for (String c : cand) {
                try {
                    Object v = rs.getObject(c);
                    if (v != null) return v;
                } catch (SQLException ignored) {}
            }
            return null;
        }
    }

    // small wrapper for instructor combo box
    private static class InstructorItem {
        final Long id;
        final String label;
        InstructorItem(Long id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label == null ? "<instructor>" : label; }
    }
}
