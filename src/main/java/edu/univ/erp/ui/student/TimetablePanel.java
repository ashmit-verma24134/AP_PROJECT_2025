package edu.univ.erp.ui.student;

import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TimetablePanel extends JPanel {
    private final TimetableModel model = new TimetableModel();
    private final JTable table = new JTable(model);
    private final JLabel statusLabel = new JLabel(" ");
    private String studentId;
    private boolean actionsEnabled = true;

    public TimetablePanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);

        // ===== Header =====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("📅 My Timetable");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);
        initUI();
    }

    private void initUI() {
        // ===== Table Style =====
        table.setModel(model);
        table.setRowHeight(56);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 6));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(Theme.DIVIDER);
        table.setBackground(Theme.SURFACE);
        table.setFont(Theme.BODY_FONT);
        table.setShowGrid(false);

        // Center align for all columns except first
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        // Wrapping HTML renderer for first column
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                String raw = value == null ? "" : value.toString();
                int colWidth = Math.max(tbl.getColumnModel().getColumn(column).getWidth() - 12, 80);
                l.setText("<html><div style='width:" + colWidth + "px; line-height:1.3em;'>" + raw + "</div></html>");
                l.setVerticalAlignment(SwingConstants.TOP);
                l.setBorder(new EmptyBorder(8, 8, 8, 8));

                if (!isSelected) {
                    l.setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.PRIMARY_LIGHT);
                }
                return l;
            }
        });

        // ===== Table Header =====
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.BODY_BOLD);
        header.setBackground(Theme.PRIMARY_DARK);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // ===== Scroll Pane Styling =====
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(0, 16, 16, 16));
        scrollPane.getViewport().setBackground(Theme.SURFACE);

        add(scrollPane, BorderLayout.CENTER);

        // ===== Status Bar =====
        statusLabel.setFont(Theme.BODY_FONT);
        statusLabel.setForeground(Theme.NEUTRAL_MED);
        statusLabel.setBorder(new EmptyBorder(0, 18, 8, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    // =====================================================
    // Public API (used by StudentPanel / navigation system)
    // =====================================================
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void setActionsEnabled(boolean enabled) {
        this.actionsEnabled = enabled;
        table.setEnabled(enabled);
        repaint();
    }

    public void reloadForStudent() {
        if (studentId == null) return;

        setActionsEnabled(false);
        statusLabel.setText("Loading timetable... ⏳");

        new SwingWorker<List<Row>, Void>() {
            @Override
            protected List<Row> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    String sql = """
                        SELECT c.code, c.title, s.section_id AS section_no,
                               IFNULL(i.full_name, 'TBA') AS instructor,
                               s.day_time AS schedule, CONCAT(s.semester, ' ', s.year) AS semester
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

            @Override
            protected void done() {
                try {
                    List<Row> rows = get();
                    model.setRows(rows);
                    statusLabel.setText(rows.isEmpty()
                            ? "No courses found for this semester."
                            : "Loaded " + rows.size() + " enrolled courses ✅");
                } catch (Exception ex) {
                    statusLabel.setText("❌ Failed to load timetable: " + ex.getMessage());
                    JOptionPane.showMessageDialog(TimetablePanel.this,
                            "Failed to load timetable: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setActionsEnabled(true);
                }
            }
        }.execute();
    }

    // =====================================================
    // Table Model & Row Data
    // =====================================================
    static class Row {
        final String code, title, sectionNo, instructor, schedule, semester;

        Row(String code, String title, String sectionNo, String instructor, String schedule, String semester) {
            this.code = code;
            this.title = title;
            this.sectionNo = sectionNo;
            this.instructor = instructor;
            this.schedule = schedule;
            this.semester = semester;
        }
    }

    static class TimetableModel extends javax.swing.table.AbstractTableModel {
        private final String[] cols = {"Course", "Section", "Instructor", "Schedule", "Semester"};
        private final List<Row> rows = new ArrayList<>();

        public void setRows(List<Row> r) {
            rows.clear();
            rows.addAll(r);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override public Object getValueAt(int r, int c) {
            Row row = rows.get(r);
            return switch (c) {
                case 0 -> "<html><b>" + row.code + "</b><br/><small>" + row.title + "</small></html>";
                case 1 -> row.sectionNo;
                case 2 -> row.instructor;
                case 3 -> row.schedule;
                case 4 -> row.semester;
                default -> "";
            };
        }

        @Override public boolean isCellEditable(int row, int column) { return false; }
    }
}
