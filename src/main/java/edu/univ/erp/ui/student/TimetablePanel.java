package edu.univ.erp.ui.student;

import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TimetablePanel extends JPanel {
    private final TimetableModel model = new TimetableModel();
    private final JTable table = new JTable(model);
    private String studentId;
    private boolean actionsEnabled = true;

    public TimetablePanel() {
        setLayout(new BorderLayout(8,8));
        JLabel title = new JLabel("My Timetable");
        title.setFont(title.getFont().deriveFont(18f));
        add(title, BorderLayout.NORTH);

        initUI();
    }

    private void initUI() {
        table.setRowHeight(56);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0,6));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // set preferred column widths after UI created
        SwingUtilities.invokeLater(() -> {
            if (table.getColumnModel().getColumnCount() >= 5) {
                table.getColumnModel().getColumn(0).setPreferredWidth(320); // Course (HTML wrap)
                table.getColumnModel().getColumn(1).setPreferredWidth(80);  // Section
                table.getColumnModel().getColumn(2).setPreferredWidth(220); // Instructor
                table.getColumnModel().getColumn(3).setPreferredWidth(220); // Schedule
                table.getColumnModel().getColumn(4).setPreferredWidth(140); // Semester
            }
        });

        // wrapping HTML renderer for first column
        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) { table.repaint(); }
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });

        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column){
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                String raw = value == null ? "" : value.toString();
                int colWidth = Math.max(tbl.getColumnModel().getColumn(column).getWidth() - 12, 80);
                l.setText("<html><div style='width:" + colWidth + "px;'>" + raw + "</div></html>");
                l.setVerticalAlignment(SwingConstants.TOP);
                l.setOpaque(true);
                return l;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setActionsEnabled(boolean enabled) { this.actionsEnabled = enabled; table.setEnabled(enabled); repaint(); }

    public void reloadForStudent() {
        if (studentId == null) return;

        new SwingWorker<List<Row>, Void>() {
            @Override protected List<Row> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    String sql = """
                        SELECT c.code, c.title, s.section_id AS section_no,
                               IFNULL(i.full_name,'TBA') AS instructor,
                               s.day_time AS schedule, CONCAT(s.semester,' ',s.year) AS semester
                        FROM enrollments e
                        JOIN sections s ON s.section_id = e.section_id
                        JOIN courses c ON c.course_id = s.course_id
                        LEFT JOIN instructors i ON i.instructor_id = s.instructor_id
                        WHERE e.student_id = ?
                          AND e.status = 'ENROLLED'
                        ORDER BY s.year DESC, s.semester DESC
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, Long.parseLong(studentId));
                        ResultSet rs = ps.executeQuery();
                        List<Row> out = new ArrayList<>();
                        while (rs.next()) {
                            out.add(new Row(
                                    rs.getString("code"),
                                    rs.getString("title"),
                                    rs.getString("section_no"),
                                    rs.getString("instructor"),
                                    rs.getString("schedule"),
                                    rs.getString("semester")
                            ));
                        }
                        return out;
                    }
                }
            }
            @Override protected void done() {
                try {
                    List<Row> rows = get();
                    model.setRows(rows);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TimetablePanel.this,
                            "Failed to load timetable: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // model
    static class Row {
        final String code, title, sectionNo, instructor, schedule, semester;
        Row(String code,String title,String sectionNo,String instructor,String schedule,String semester){
            this.code=code;this.title=title;this.sectionNo=sectionNo;this.instructor=instructor;this.schedule=schedule;this.semester=semester;
        }
    }

    static class TimetableModel extends javax.swing.table.AbstractTableModel {
        private final String[] cols = {"Course","Section","Instructor","Schedule","Semester"};
        private final java.util.List<Row> rows = new ArrayList<>();
        public void setRows(java.util.List<Row> r){ rows.clear(); rows.addAll(r); fireTableDataChanged(); }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r,int c){
            Row row = rows.get(r);
            switch(c){
                case 0: return "<html><b>"+row.code+"</b><br/><small>"+row.title+"</small></html>";
                case 1: return row.sectionNo;
                case 2: return row.instructor;
                case 3: return row.schedule;
                case 4: return row.semester;
                default: return "";
            }
        }
    }
}
