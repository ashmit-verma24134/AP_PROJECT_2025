package edu.univ.erp.ui.student;

import edu.univ.erp.util.DBConnection;
import edu.univ.erp.util.TranscriptPdfExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * TranscriptPanel - compact UI: Course | Credits | Semester | Year | Final Grade
 * - computes CGPA (weighted by credits) and displays it in the top-right
 * - CSV export contains the same columns
 * - PDF export calls TranscriptPdfExporter.exportPremium(...) with cgpa
 */
public class TranscriptPanel extends JPanel {
    private final TranscriptModel model = new TranscriptModel();
    private final JTable table = new JTable(model);

    // set by caller (MainFrame) before reloadForStudent()
    private String studentId;

    // metadata loaded from DB (or set by caller)
    private String studentName;
    private String program;
    private String department;
    private String batch;

    // CGPA label
    private final JLabel cgpaLabel = new JLabel("CGPA: -");

    private boolean actionsEnabled = true;

    public TranscriptPanel() {
        setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("Transcript / Download CSV");
        title.setFont(title.getFont().deriveFont(18f));
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // top panel with title (left) and cgpa (right)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        topPanel.add(title, BorderLayout.WEST);

        cgpaLabel.setFont(cgpaLabel.getFont().deriveFont(Font.BOLD, 14f));
        cgpaLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
        topPanel.add(cgpaLabel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        initUI();

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Footer with both Export CSV and Export PDF buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JButton btnExportCsv = new JButton("Export CSV");
        btnExportCsv.addActionListener(e -> exportCsv());
        footer.add(btnExportCsv);

        JButton btnExportPdf = new JButton("Export PDF");
        btnExportPdf.addActionListener(e -> exportPdf());
        footer.add(btnExportPdf);

        add(footer, BorderLayout.SOUTH);
    }

    /** set numeric DB id as string (e.g. "45") */
    public void setStudentId(String studentId) { this.studentId = studentId; }

    /** caller can set full name if already resolved */
    public void setStudentFullName(String fullName) { this.studentName = fullName; }

    public void setActionsEnabled(boolean enabled) {
        this.actionsEnabled = enabled;
        table.setEnabled(enabled);
        repaint();
    }

    public int getLoadedRowCount() { return model.getRowCount(); }

    public void reloadForStudent() {
        System.out.println("[DEBUG TranscriptPanel.beforeStart] studentId=" + studentId + " studentName(before)=" + studentName);
        if (studentId == null) return;

        new SwingWorker<List<TranscriptModel.Row>, Void>() {
            @Override
            protected List<TranscriptModel.Row> doInBackground() throws Exception {
                List<TranscriptModel.Row> out = new ArrayList<>();
                try (Connection conn = DBConnection.getErpConnection()) {
                    // metadata
                    try (PreparedStatement psInfo = conn.prepareStatement(
                            "SELECT full_name, program, department, year AS batch FROM students WHERE student_id = ?")) {
                        try { psInfo.setLong(1, Long.parseLong(studentId)); }
                        catch (NumberFormatException ex) { psInfo.setString(1, studentId); }
                        try (ResultSet rsInfo = psInfo.executeQuery()) {
                            if (rsInfo.next()) {
                                String dbName = rsInfo.getString("full_name");
                                if (dbName != null && !dbName.isBlank()) studentName = dbName;
                                program = rsInfo.getString("program");
                                department = rsInfo.getString("department");
                                Object b = rsInfo.getObject("batch");
                                batch = b == null ? null : String.valueOf(b);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("[DEBUG TranscriptPanel] metadata fetch failed: " + ex.getMessage());
                    }

                    // transcript rows (note: we only extract fields we need)
                    String sql = """
                        SELECT c.code, c.title, COALESCE(c.credits,0) AS credits,
                               s.semester, s.year, g.final_grade
                        FROM enrollments e
                        JOIN sections s ON s.section_id = e.section_id
                        JOIN courses c ON c.course_id = s.course_id
                        LEFT JOIN grades g ON g.enrollment_id = e.enrollment_id
                        WHERE e.student_id = ?
                        ORDER BY s.year DESC, s.semester DESC
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        try { ps.setLong(1, Long.parseLong(studentId)); }
                        catch (NumberFormatException ex) { ps.setString(1, studentId); }
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                out.add(new TranscriptModel.Row(
                                        rs.getString("code"),
                                        rs.getString("title"),
                                        rs.getInt("credits"),
                                        rs.getString("semester"),
                                        rs.getInt("year"),
                                        rs.getString("final_grade")
                                ));
                            }
                        }
                    }
                }
                return out;
            }

            @Override
            protected void done() {
                try {
                    List<TranscriptModel.Row> rows = get();
                    model.setRows(rows);

                    if (studentName == null || studentName.isBlank()) studentName = "Student #" + studentId;
                    if (program == null || program.isBlank()) program = "Program";
                    if (department == null || department.isBlank()) department = "IIIT-Delhi";
                    if (batch == null || batch.isBlank()) batch = "Batch";

                    // compute CGPA weighted by credits (ignore ungraded rows)
                    double sumPointsTimesCredits = 0.0;
                    double sumCreditsForGraded = 0.0;
                    for (TranscriptModel.Row rr : rows) {
                        Double pts = gradeToPoints(rr.finalGrade);
                        if (pts != null) {
                            sumPointsTimesCredits += pts * (double) rr.credits;
                            sumCreditsForGraded += (double) rr.credits;
                        }
                    }
                    String cgpaText = "-";
                    if (sumCreditsForGraded > 0.0) {
                        double cgpa = sumPointsTimesCredits / sumCreditsForGraded;
                        cgpa = Math.round(cgpa * 100.0) / 100.0;
                        cgpaText = String.valueOf(cgpa);
                    }
                    cgpaLabel.setText("CGPA: " + cgpaText);

                    System.out.println("[DEBUG TranscriptPanel.afterDone] studentId=" + studentId
                            + " studentName(final)=\"" + studentName + "\" program=" + program
                            + " dept=" + department + " batch=" + batch + " rows=" + rows.size());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(TranscriptPanel.this,
                            "Failed to load transcript: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // grade mapping helper (10-pt scale)
    private static Double gradeToPoints(String grade) {
        if (grade == null) return null;
        String g = grade.trim().toUpperCase();
        switch (g) {
            case "A+": case "A": return 10.0;
            case "A-": return 9.0;
            case "B+": return 8.0;
            case "B": return 7.0;
            case "B-": return 6.0;
            case "C+": return 5.0;
            case "C": return 4.0;
            case "C-": return 3.0;
            case "D": return 2.0;
            case "F": return 0.0;
            default:
                try { return Double.parseDouble(g); } catch (Exception e) { return null; }
        }
    }

    private void initUI() {
        table.setRowHeight(56);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 6));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        SwingUtilities.invokeLater(() -> {
            if (table.getColumnModel().getColumnCount() >= 5) {
                table.getColumnModel().getColumn(0).setPreferredWidth(420); // course
                table.getColumnModel().getColumn(1).setPreferredWidth(60);  // credits
                table.getColumnModel().getColumn(2).setPreferredWidth(120); // semester
                table.getColumnModel().getColumn(3).setPreferredWidth(80);  // year
                table.getColumnModel().getColumn(4).setPreferredWidth(120); // final grade
            }
        });

        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) { table.repaint(); }
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });

        // custom renderer for course column (HTML wrapped)
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
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

    // ---- CSV export (only the five columns) ----
    private void exportCsv() {
        if (studentId == null) {
            JOptionPane.showMessageDialog(this, "No student set.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("transcript.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File f = chooser.getSelectedFile();

        try (FileWriter fw = new FileWriter(f)) {
            // metadata header
            fw.append("StudentName,StudentID,Program,Department,Batch,CGPA\n");
            fw.append(escapeCsv(studentName)).append(',').append(escapeCsv(studentId)).append(',')
                    .append(escapeCsv(program)).append(',').append(escapeCsv(department)).append(',')
                    .append(escapeCsv(batch)).append(',').append(escapeCsv(getCgpaText())).append('\n');

            // transcript header (only the requested columns)
            fw.append("CourseCode,Title,Credits,Semester,Year,FinalGrade\n");
            for (TranscriptModel.Row r : model.getRows()) {
                fw.append(escapeCsv(r.code)).append(',')
                        .append(escapeCsv(r.title)).append(',')
                        .append(String.valueOf(r.credits)).append(',')
                        .append(escapeCsv(r.semester)).append(',')
                        .append(String.valueOf(r.year)).append(',')
                        .append(escapeCsv(r.finalGrade)).append('\n');
            }
            JOptionPane.showMessageDialog(this, "Saved CSV to: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getCgpaText() {
        String txt = cgpaLabel.getText();
        if (txt == null) return "-";
        if (txt.startsWith("CGPA:")) return txt.substring(5).trim();
        return txt;
    }

    // ---- PDF export (calls updated exporter with cgpa param) ----
    private void exportPdf() {
        if (studentId == null) {
            JOptionPane.showMessageDialog(this, "No student selected.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("transcript.pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File out = chooser.getSelectedFile();

        try {
            List<TranscriptPdfExporter.TranscriptRow> list = new ArrayList<>();
            for (TranscriptModel.Row r : model.getRows()) {
                list.add(new TranscriptPdfExporter.TranscriptRow(
                        r.code, r.title, r.credits, r.semester, r.year, r.finalGrade
                ));
            }

            File logo = new File("src/main/resources/iiitd_logo.png");
            if (!logo.exists()) logo = null;

            String nameToUse = (studentName == null || studentName.isBlank()) ? ("Student #" + studentId) : studentName;
            String prog = (program == null || program.isBlank()) ? "Program" : program;
            String dept = (department == null || department.isBlank()) ? "IIIT-Delhi" : department;
            String batchVal = (batch == null || batch.isBlank()) ? "Batch" : batch;
            String issueDate = java.time.LocalDate.now().toString();
            String cgpaStr = getCgpaText();

            TranscriptPdfExporter.exportPremium(
                    list, out, nameToUse, studentId, prog, dept, batchVal, logo, issueDate, cgpaStr
            );

            JOptionPane.showMessageDialog(this, "PDF exported!\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\"") || out.contains("\n")) out = "\"" + out + "\"";
        return out;
    }

    // ---- table model ----
    static class TranscriptModel extends javax.swing.table.AbstractTableModel {
        public static class Row {
            final String code;
            final String title;
            final int credits;
            final String semester;
            final int year;
            final String finalGrade;

            Row(String code, String title, int credits, String semester, int year, String finalGrade) {
                this.code = code;
                this.title = title;
                this.credits = credits;
                this.semester = semester;
                this.year = year;
                this.finalGrade = finalGrade;
            }
        }

        // only five columns in the UI table
        private final String[] cols = {"Course", "Credits", "Semester", "Year", "Final Grade"};
        private final List<Row> rows = new ArrayList<>();

        public void setRows(List<Row> r) {
            rows.clear();
            rows.addAll(r);
            fireTableDataChanged();
        }

        public List<Row> getRows() { return rows; }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Row row = rows.get(r);
            switch (c) {
                case 0:
                    return "<html><b>" + row.code + "</b><br/><small>" + row.title + "</small></html>";
                case 1:
                    return row.credits;
                case 2:
                    return row.semester;
                case 3:
                    return row.year;
                case 4:
                    return row.finalGrade == null ? "N/A" : row.finalGrade;
                default:
                    return "";
            }
        }
    }
}
