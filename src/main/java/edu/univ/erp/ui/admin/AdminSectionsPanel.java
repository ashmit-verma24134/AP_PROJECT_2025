package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminSectionsPanel
 * - Shows a simple list of all sections (joined with course code if available)
 * - Allows creating a section (choose course from dropdown) and deleting sections
 */
public class AdminSectionsPanel extends JPanel {

    private final JComboBox<CourseItem> cbCourse = new JComboBox<>();
    private final JTextField txtSectionCode = new JTextField(10);
    private final JTextField txtDayTime = new JTextField(16);
    private final JTextField txtRoom = new JTextField(10);
    private final JTextField txtCapacity = new JTextField(6);
    private final JTextField txtSemester = new JTextField(8);
    private final JTextField txtYear = new JTextField(6);

    private final JButton btnAdd = new JButton("Add Section");
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnDelete = new JButton("Delete Selected");

    private final DefaultTableModel model;
    private final JTable table;

    public AdminSectionsPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(12,12,12,12));

        // form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,8,6,8);
        gc.anchor = GridBagConstraints.WEST;

        int r = 0;
        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Course:"), gc);
        gc.gridx = 1; form.add(cbCourse, gc);
        gc.gridx = 2; form.add(new JLabel("Section Code:"), gc);
        gc.gridx = 3; form.add(txtSectionCode, gc);

        r++; gc.gridy = r; gc.gridx = 0; form.add(new JLabel("Day/Time:"), gc);
        gc.gridx = 1; form.add(txtDayTime, gc);
        gc.gridx = 2; form.add(new JLabel("Room:"), gc);
        gc.gridx = 3; form.add(txtRoom, gc);

        r++; gc.gridy = r; gc.gridx = 0; form.add(new JLabel("Capacity:"), gc);
        gc.gridx = 1; form.add(txtCapacity, gc);
        gc.gridx = 2; form.add(new JLabel("Semester:"), gc);
        gc.gridx = 3; form.add(txtSemester, gc);

        r++; gc.gridy = r; gc.gridx = 0; form.add(new JLabel("Year:"), gc);
        gc.gridx = 1; form.add(txtYear, gc);

        r++; gc.gridy = r; gc.gridx = 0; gc.gridwidth = 4;
        JPanel br = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); br.setOpaque(false);
        br.add(btnAdd); br.add(btnRefresh); br.add(btnDelete);
        form.add(br, gc);

        add(form, BorderLayout.NORTH);

        // table
        model = new DefaultTableModel(new String[]{"Section ID","Course ID","Course Code","Section Code","Day/Time","Room","Capacity","Semester","Year"}, 0) {
            @Override public boolean isCellEditable(int r1, int c1) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // actions
        btnRefresh.addActionListener(e -> refreshCourseListAndSections());
        btnAdd.addActionListener(e -> addSection());
        btnDelete.addActionListener(e -> deleteSelected());

        // initial load
        SwingUtilities.invokeLater(this::refreshCourseListAndSections);
    }

    private void refreshCourseListAndSections() {
        btnRefresh.setEnabled(false);
        btnAdd.setEnabled(false);
        btnDelete.setEnabled(false);

        new SwingWorker<Void,Void>() {
            Exception err = null;
            List<CourseItem> courses = new ArrayList<>();
            List<Object[]> rows = new ArrayList<>();

            @Override protected Void doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    DatabaseMetaData md = conn.getMetaData();
                    // get course list (id+code)
                    String courseIdCol = detectColumn(md, "courses", new String[]{"course_id","id"});
                    String courseCodeCol = detectColumn(md, "courses", new String[]{"course_code","code"});
                    if (courseIdCol != null) {
                        String q = "SELECT " + courseIdCol + ", " + (courseCodeCol != null ? courseCodeCol : "NULL") + " FROM courses ORDER BY " + courseIdCol;
                        try (PreparedStatement ps = conn.prepareStatement(q);
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Object id = rs.getObject(1);
                                Object code = rs.getObject(2);
                                long cid = (id instanceof Number) ? ((Number)id).longValue() : Long.parseLong(String.valueOf(id));
                                courses.add(new CourseItem(cid, code == null ? ("C" + cid) : String.valueOf(code)));
                            }
                        }
                    }

                    // now load sections joined with course code if possible
                    String secTbl = detectSectionsTable(md);
                    String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
                    String secCodeCol = detectColumn(md, secTbl, new String[]{"section_code","code"});
                    String courseIdColInSec = detectColumn(md, secTbl, new String[]{"course_id","courseid","course"});
                    String timeCol = detectColumn(md, secTbl, new String[]{"day_time","time","slot","schedule"});
                    String roomCol = detectColumn(md, secTbl, new String[]{"room","location"});
                    String capCol = detectColumn(md, secTbl, new String[]{"capacity","cap"});
                    String semCol = detectColumn(md, secTbl, new String[]{"semester","term"});
                    String yearCol = detectColumn(md, secTbl, new String[]{"year"});

if (secTbl != null && secIdCol != null && courseIdColInSec != null) {
    StringBuilder q = new StringBuilder();
    q.append("SELECT s.").append(secIdCol)
     .append(", s.").append(courseIdColInSec)
     .append(", ").append(secCodeCol != null ? "s." + secCodeCol : "NULL AS section_code")
     .append(", ").append(timeCol != null ? "s." + timeCol : "NULL AS day_time")
     .append(", ").append(roomCol != null ? "s." + roomCol : "NULL AS room")
     .append(", ").append(capCol != null ? "s." + capCol : "NULL AS capacity")
     .append(", ").append(semCol != null ? "s." + semCol : "NULL AS semester")
     .append(", ").append(yearCol != null ? "s." + yearCol : "NULL AS year")
     .append(" FROM ").append(secTbl).append(" s ORDER BY s.").append(secIdCol);


                        try (PreparedStatement ps = conn.prepareStatement(q.toString());
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Object sid = rs.getObject(1);
                                Object cid = rs.getObject(2);
                                Object scode = rs.getObject(3);
                                Object sched = rs.getObject(4);
                                Object room = rs.getObject(5);
                                Object cap = rs.getObject(6);
                                Object sem = rs.getObject(7);
                                Object yr = rs.getObject(8);

                                // find course code from courses list to show in table
                                String cc = null;
                                if (cid != null) {
                                    long cl = (cid instanceof Number) ? ((Number)cid).longValue() : Long.parseLong(String.valueOf(cid));
                                    for (CourseItem ci : courses) if (ci.id == cl) { cc = ci.code; break; }
                                }
                                rows.add(new Object[]{sid, cid, cc == null ? "" : cc, scode, sched, room, cap, sem, yr});
                            }
                        }
                    }
                } catch (Exception ex) {
                    err = ex;
                }
                return null;
            }

            @Override protected void done() {
                btnRefresh.setEnabled(true);
                btnAdd.setEnabled(true);
                btnDelete.setEnabled(true);
                if (err != null) {
                    JOptionPane.showMessageDialog(AdminSectionsPanel.this, "Failed to load sections/courses: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // populate combo
                cbCourse.removeAllItems();
                for (CourseItem ci : courses) cbCourse.addItem(ci);

                // populate table
                model.setRowCount(0);
                for (Object[] r : rows) model.addRow(r);
            }
        }.execute();
    }

    private void addSection() {
        CourseItem selCourse = (CourseItem) cbCourse.getSelectedItem();
        if (selCourse == null) { JOptionPane.showMessageDialog(this, "Select course"); return; }
        final long courseId = selCourse.id;
        final String scode = txtSectionCode.getText().trim();
        final String sched = txtDayTime.getText().trim();
        final String room = txtRoom.getText().trim();
        final String cap = txtCapacity.getText().trim();
        final String sem = txtSemester.getText().trim();
        final String yr = txtYear.getText().trim();

        btnAdd.setEnabled(false);
        new SwingWorker<Void, Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    DatabaseMetaData md = conn.getMetaData();
                    String secTbl = detectSectionsTable(md);
                    String courseIdCol = detectColumn(md, secTbl, new String[]{"course_id","courseid","course"});
                    if (secTbl == null || courseIdCol == null) throw new SQLException("Sections table or course_id missing");

                    List<String> cols = new ArrayList<>();
                    List<String> placeholders = new ArrayList<>();
                    List<Object> params = new ArrayList<>();

                    cols.add(courseIdCol); placeholders.add("?"); params.add(courseId);
                    String secCodeCol = detectColumn(md, secTbl, new String[]{"section_code","code"});
                    if (secCodeCol != null) { cols.add(secCodeCol); placeholders.add("?"); params.add(scode.isEmpty()? null : scode); }
                    String timeCol = detectColumn(md, secTbl, new String[]{"day_time","time","slot","schedule"});
                    if (timeCol != null) { cols.add(timeCol); placeholders.add("?"); params.add(sched.isEmpty()? null : sched); }
                    String roomCol = detectColumn(md, secTbl, new String[]{"room","location"});
                    if (roomCol != null) { cols.add(roomCol); placeholders.add("?"); params.add(room.isEmpty()? null : room); }
                    String capCol = detectColumn(md, secTbl, new String[]{"capacity","cap"});
                    if (capCol != null) { cols.add(capCol); placeholders.add("?"); params.add(cap.isEmpty()? null : Integer.parseInt(cap)); }
                    String semCol = detectColumn(md, secTbl, new String[]{"semester","term"});
                    if (semCol != null) { cols.add(semCol); placeholders.add("?"); params.add(sem.isEmpty()? null : sem); }
                    String yearCol = detectColumn(md, secTbl, new String[]{"year"});
                    if (yearCol != null) { cols.add(yearCol); placeholders.add("?"); params.add(yr.isEmpty()? null : Integer.parseInt(yr)); }

                    String sql = "INSERT INTO " + secTbl + " (" + String.join(", ", cols) + ") VALUES (" + String.join(", ", placeholders) + ")";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (int i=0;i<params.size();++i) {
                            Object p = params.get(i);
                            if (p == null) ps.setNull(i+1, Types.VARCHAR);
                            else if (p instanceof Integer) ps.setInt(i+1, (Integer)p);
                            else if (p instanceof Long) ps.setLong(i+1, (Long)p);
                            else ps.setString(i+1, String.valueOf(p));
                        }
                        ps.executeUpdate();
                    }
                } catch (Exception ex) { err = ex; }
                return null;
            }
            @Override protected void done() {
                btnAdd.setEnabled(true);
                if (err != null) JOptionPane.showMessageDialog(AdminSectionsPanel.this, "Failed to add section: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                else {
                    txtSectionCode.setText(""); txtDayTime.setText(""); txtRoom.setText(""); txtCapacity.setText(""); txtSemester.setText(""); txtYear.setText("");
                    refreshCourseListAndSections();
                }
            }
        }.execute();
    }

    private void deleteSelected() {
        int[] sel = table.getSelectedRows();
        if (sel == null || sel.length == 0) { JOptionPane.showMessageDialog(this, "Select section(s) to delete"); return; }
        int ok = JOptionPane.showConfirmDialog(this, "Delete selected section(s)?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void,Void>() {
            Exception err = null;
            @Override protected Void doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    DatabaseMetaData md = conn.getMetaData();
                    String secTbl = detectSectionsTable(md);
                    String secIdCol = detectColumn(md, secTbl, new String[]{"section_id","id","sec_id"});
                    if (secTbl == null || secIdCol == null) throw new SQLException("Sections table or id column missing");

                    String sql = "DELETE FROM " + secTbl + " WHERE " + secIdCol + " = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (int r : sel) {
                            Object idv = model.getValueAt(r, 0);
                            long sid = (idv instanceof Number) ? ((Number)idv).longValue() : Long.parseLong(String.valueOf(idv));
                            ps.setLong(1, sid);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                } catch (Exception ex) { err = ex; }
                return null;
            }
            @Override protected void done() {
                if (err != null) JOptionPane.showMessageDialog(AdminSectionsPanel.this, "Failed to delete: " + err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                refreshCourseListAndSections();
            }
        }.execute();
    }

    // small helpers similar to CourseEditorDialog
    private static class CourseItem {
        final long id;
        final String code;
        CourseItem(long id, String code) { this.id = id; this.code = code; }
        public String toString() { return code == null ? ("C" + id) : code; }
    }

    private static String detectSectionsTable(DatabaseMetaData md) throws SQLException {
        try (ResultSet tables = md.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME").toLowerCase();
                if ("sections".equals(name) || "course_sections".equals(name)) return tables.getString("TABLE_NAME");
            }
        }
        return "sections"; // fallback
    }

    private static String detectColumn(DatabaseMetaData md, String table, String[] cand) throws SQLException {
        if (table == null) return null;
        try (ResultSet rs = md.getColumns(null, null, table, null)) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME").toLowerCase();
                for (String c : cand) if (c.equals(col)) return rs.getString("COLUMN_NAME");
            }
        } catch (SQLException ignored) {}
        return null;
    }
}
