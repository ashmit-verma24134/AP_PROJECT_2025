package edu.univ.erp.ui.student;

import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TranscriptPanel extends JPanel {
    private final TranscriptModel model = new TranscriptModel();
    private final JTable table = new JTable(model);
    private String studentId;
    private boolean actionsEnabled = true;

    public TranscriptPanel() {
        setLayout(new BorderLayout(8,8));
        JLabel title = new JLabel("Transcript / Download CSV");
        title.setFont(title.getFont().deriveFont(18f));
        add(title, BorderLayout.NORTH);

        initUI();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnExport = new JButton("Export CSV");
        btnExport.addActionListener(e -> exportCsv());
        footer.add(btnExport);
        add(footer, BorderLayout.SOUTH);
    }

    private void initUI() {
        table.setRowHeight(56);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0,6));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        SwingUtilities.invokeLater(() -> {
            if (table.getColumnModel().getColumnCount() >= 6) {
                table.getColumnModel().getColumn(0).setPreferredWidth(320); // course col (html)
                table.getColumnModel().getColumn(1).setPreferredWidth(80);  // section
                table.getColumnModel().getColumn(2).setPreferredWidth(120); // semester
                table.getColumnModel().getColumn(3).setPreferredWidth(80);  // year
                table.getColumnModel().getColumn(4).setPreferredWidth(120); // status
                table.getColumnModel().getColumn(5).setPreferredWidth(140); // final grade
            }
        });

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
    }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setActionsEnabled(boolean enabled) { this.actionsEnabled = enabled; table.setEnabled(enabled); repaint(); }

    public void reloadForStudent() {
        if (studentId == null) return;

        new SwingWorker<List<TranscriptModel.Row>, Void>() {
            @Override protected List<TranscriptModel.Row> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    String sql = """
                        SELECT c.code, c.title, s.section_id AS section_no, s.semester, s.year,
                               e.status, e.enrolled_at, g.final_grade
                        FROM enrollments e
                        JOIN sections s ON s.section_id = e.section_id
                        JOIN courses c ON c.course_id = s.course_id
                        LEFT JOIN grades g ON g.enrollment_id = e.enrollment_id
                        WHERE e.student_id = ?
                        ORDER BY s.year DESC, s.semester DESC
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, Long.parseLong(studentId));
                        ResultSet rs = ps.executeQuery();
                        List<TranscriptModel.Row> out = new ArrayList<>();
                        while (rs.next()) {
                            out.add(new TranscriptModel.Row(
                                    rs.getString("code"),
                                    rs.getString("title"),
                                    rs.getString("section_no"),
                                    rs.getString("semester"),
                                    rs.getInt("year"),
                                    rs.getString("status"),
                                    rs.getTimestamp("enrolled_at"),
                                    rs.getString("final_grade")
                            ));
                        }
                        return out;
                    }
                }
            }
            @Override protected void done() {
                try {
                    List<TranscriptModel.Row> rows = get();
                    model.setRows(rows);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TranscriptPanel.this,
                            "Failed to load transcript: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void exportCsv() {
        if (studentId == null) { JOptionPane.showMessageDialog(this, "No student set.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("transcript.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File f = chooser.getSelectedFile();

        try (FileWriter fw = new FileWriter(f)) {
            fw.append("CourseCode,Title,Section,Semester,Year,Status,EnrolledAt,FinalGrade\n");
            for (TranscriptModel.Row r : model.getRows()) {
                fw.append(escapeCsv(r.code)).append(',')
                  .append(escapeCsv(r.title)).append(',')
                  .append(escapeCsv(r.sectionNo)).append(',')
                  .append(escapeCsv(r.semester)).append(',')
                  .append(String.valueOf(r.year)).append(',')
                  .append(escapeCsv(r.status)).append(',')
                  .append(r.enrolledAt == null ? "" : r.enrolledAt.toString()).append(',')
                  .append(escapeCsv(r.finalGrade)).append('\n');
            }
            JOptionPane.showMessageDialog(this, "Saved CSV to: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\"") || out.contains("\n")) out = "\"" + out + "\"";
        return out;
    }

    // model
    static class TranscriptModel extends javax.swing.table.AbstractTableModel {
        public static class Row {
            final String code, title, sectionNo, semester, status, finalGrade;
            final int year;
            final java.sql.Timestamp enrolledAt;
            Row(String code,String title,String sectionNo,String semester,int year,String status,java.sql.Timestamp enrolledAt,String finalGrade){
                this.code=code;this.title=title;this.sectionNo=sectionNo;this.semester=semester;this.year=year;this.status=status;this.enrolledAt=enrolledAt;this.finalGrade=finalGrade;
            }
        }
        private final String[] cols = {"Course","Section","Semester","Year","Status","Final Grade"};
        private final List<Row> rows = new ArrayList<>();
        public void setRows(List<Row> r){ rows.clear(); rows.addAll(r); fireTableDataChanged(); }
        public List<Row> getRows(){ return rows; }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r,int c){
            Row row = rows.get(r);
            switch(c){
                case 0: return "<html><b>"+row.code+"</b><br/><small>"+row.title+"</small></html>";
                case 1: return row.sectionNo;
                case 2: return row.semester;
                case 3: return row.year;
                case 4: return row.status;
                case 5: return row.finalGrade == null ? "N/A" : row.finalGrade;
                default: return "";
            }
        }
    }
}
