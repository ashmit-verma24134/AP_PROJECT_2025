package edu.univ.erp.ui.student;

import edu.univ.erp.service.Result;
import edu.univ.erp.service.StudentService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple CatalogPanel:
 * - Shows list of available course sections (mock data for now)
 * - Search box (client-side)
 * - Register button per row which calls StudentService.registerForSection(...)
 *
 * Replace mock-loading with a SectionDao/Service call later.
 */
public class CatalogPanel extends JPanel {
    private final CatalogModel model = new CatalogModel();
    private final JTable table = new JTable(model);
    private final JTextField txtSearch = new JTextField(20);
    private String studentId; // set by StudentPanel after login
    private boolean actionsEnabled = true;

    public CatalogPanel() {
        setLayout(new BorderLayout(8,8));
        initUI();
        reloadMockData();
    }

    private void initUI() {
        // top: title + search
        JPanel top = new JPanel(new BorderLayout(8,8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        JLabel title = new JLabel("Course Catalog");
        title.setFont(title.getFont().deriveFont(18f));
        left.add(title);
        top.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,6));
        txtSearch.setToolTipText("Search by code, title, or instructor");
        JButton btnSearch = new JButton("Search");
        btnSearch.addActionListener(e -> doSearch());
        right.add(new JLabel("Search:"));
        right.add(txtSearch);
        right.add(btnSearch);
        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // table setup
        table.setRowHeight(36);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true); // allow sorting by columns

        // renderers and editor for action column
        table.getColumnModel().getColumn(0).setCellRenderer(new HtmlRenderer());
        // action column index is last
        int actionCol = model.getColumnCount()-1;
        table.getColumnModel().getColumn(actionCol).setCellRenderer(new RegisterButtonRenderer());
        table.getColumnModel().getColumn(actionCol).setCellEditor(new RegisterButtonEditor(new JButton("Register")));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // small footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> reloadMockData()); // later: reload from DB
        footer.add(btnRefresh);
        add(footer, BorderLayout.SOUTH);
    }

    private void doSearch() {
        String q = txtSearch.getText().trim().toLowerCase();
        model.filter(q);
    }

    /** Called by StudentPanel after login */
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    /** Enable/disable interactive actions (register) */
    public void setActionsEnabled(boolean enabled) {
        this.actionsEnabled = enabled;
        table.setEnabled(enabled);
        repaint();
    }

    public boolean isActionsEnabled() { return actionsEnabled; }

    // --- mock data loader (replace with DB call) ---
public void reloadMockData() {
    SwingUtilities.invokeLater(() -> {
        List<CatalogRow> rows = new ArrayList<>();
        // USE real section_id values that exist in DB (1,2,3)
        rows.add(new CatalogRow(1L, "CS101", "Intro to Computer Science", "001", "Dr. Johnson", 3, 30, 27, "Mon/Wed 09:30-11:00", 2025, "Spring"));
        rows.add(new CatalogRow(2L, "MATH201", "Calculus II", "002", "Prof. Smith", 4, 40, 38, "Tue/Thu 11:30-13:00", 2025, "Spring"));
        rows.add(new CatalogRow(3L, "ENG102", "English Composition", "003", "Dr. Williams", 3, 30, 0, "Mon 14:00-15:30", 2025, "Spring"));
        model.setRows(rows);
    });
}

    // --- model + rows ---
    static class CatalogRow {
        final long sectionId;
        final String courseCode, title, sectionNo, instructor;
        final int credits, capacity, seatsLeft;
        final String schedule;
        final int year;
        final String semester;

        CatalogRow(long sectionId, String courseCode, String title, String sectionNo, String instructor,
                   int credits, int capacity, int seatsLeft, String schedule, int year, String semester) {
            this.sectionId = sectionId;
            this.courseCode = courseCode;
            this.title = title;
            this.sectionNo = sectionNo;
            this.instructor = instructor;
            this.credits = credits;
            this.capacity = capacity;
            this.seatsLeft = seatsLeft;
            this.schedule = schedule;
            this.year = year;
            this.semester = semester;
        }
    }

    static class CatalogModel extends AbstractTableModel {
        private final String[] cols = {"Course","Section","Instructor","Credits","Capacity","Seats Left","Schedule","Action"};
        private final List<CatalogRow> all = new ArrayList<>();
        private final List<CatalogRow> rows = new ArrayList<>();

        public void setRows(List<CatalogRow> r) { all.clear(); all.addAll(r); rows.clear(); rows.addAll(r); fireTableDataChanged(); }
        public CatalogRow getRow(int r) { return rows.get(r); }

        public void filter(String q) {
            rows.clear();
            if (q == null || q.isEmpty()) rows.addAll(all);
            else {
                for (CatalogRow cr : all) {
                    if (cr.courseCode.toLowerCase().contains(q) || cr.title.toLowerCase().contains(q)
                            || cr.instructor.toLowerCase().contains(q) || cr.sectionNo.toLowerCase().contains(q)) {
                        rows.add(cr);
                    }
                }
            }
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int col) { return cols[col]; }
        @Override public Object getValueAt(int r, int c) {
            CatalogRow row = rows.get(r);
            switch (c) {
                case 0: return "<html><b>" + row.courseCode + "</b><br/><small>" + row.title + "</small></html>";
                case 1: return row.sectionNo;
                case 2: return row.instructor;
                case 3: return row.credits;
                case 4: return row.capacity;
                case 5: return row.seatsLeft;
                case 6: return row.schedule;
                case 7: return "Register";
                default: return "";
            }
        }
        @Override public boolean isCellEditable(int row, int col) { return col==7; }
        @Override public Class<?> getColumnClass(int col) {
            if (getRowCount() == 0) return String.class;
            Object v = getValueAt(0,col);
            return v == null ? String.class : v.getClass();
        }
    }

    // Render HTML in first column
    static class HtmlRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, foc, r, c);
            l.setText(value == null ? "" : value.toString());
            l.setVerticalAlignment(SwingConstants.TOP);
            return l;
        }
    }

    // Register button renderer/editor
// Register button renderer/editor
class RegisterButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
    public RegisterButtonRenderer() { setOpaque(true); }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int col) {
        // when table asks renderer for header or other non-row, row might be -1
        if (row < 0) {
            setText(""); 
            setEnabled(false);
            return this;
        }

        // convert view index -> model index (important when sorting enabled)
        int modelRow = row;
        try {
            modelRow = table.convertRowIndexToModel(row);
        } catch (Exception ignored) {}

        // explicitly reference outer class model to avoid shadowing
        CatalogRow r = CatalogPanel.this.model.getRow(modelRow);

        setText(r.seatsLeft <= 0 ? "Full" : "Register");
        setEnabled(actionsEnabled && r.seatsLeft > 0);
        return this;
    }
}


    class RegisterButtonEditor extends AbstractCellEditor implements TableCellEditor {
        protected final JButton button;
        private String label;

        public RegisterButtonEditor(JButton b) {
            this.button = b;
            button.setOpaque(true);
            button.addActionListener(this::onClick);
        }

        private void onClick(ActionEvent ev) {
            // guard
            if (!actionsEnabled) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            JTable t = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, button);
            if (t == null) return;
            int r = t.getSelectedRow();
            if (r < 0) r = t.getEditingRow(); // fallback
            if (r < 0) return;
            final CatalogRow row = model.getRow(r);

            // confirm
            int conf = JOptionPane.showConfirmDialog(button,
                    "Register for " + row.courseCode + " (Section " + row.sectionNo + ")?",
                    "Confirm Registration", JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION) return;

            // disable UI and run registration in background
            setActionsEnabled(false);

            // SwingWorker to call StudentService
            new SwingWorker<Result, Void>() {
                @Override protected Result doInBackground() {
                    StudentService svc = new StudentService();
                    // studentId must be set by parent before calling registration
                    if (studentId == null || studentId.trim().isEmpty()) {
                        return Result.error("No student logged in.");
                    }
                    return svc.registerForSection(studentId, row.sectionId);
                }
                @Override protected void done() {
                    try {
                        Result res = get();
if (res.isSuccess()) {
    JOptionPane.showMessageDialog(button, res.getMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
} else {
    JOptionPane.showMessageDialog(button, res.getMessage(), "Failed", JOptionPane.ERROR_MESSAGE);
}

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(button, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setActionsEnabled(true);
                    }
                }
            }.execute();
        }

@Override
public Component getTableCellEditorComponent(JTable table, Object value,
                                             boolean isSelected, int row, int column) {
    int viewRow = row;
    if (viewRow < 0) viewRow = table.getEditingRow();
    if (viewRow < 0) return button;

    int modelRow = viewRow;
    try {
        modelRow = table.convertRowIndexToModel(viewRow);
    } catch (Exception ignored) {}

    CatalogRow r = CatalogPanel.this.model.getRow(modelRow);  // <-- explicit

    label = (r.seatsLeft <= 0) ? "Full" : "Register";
    button.setText(label);
    button.setEnabled(actionsEnabled && r.seatsLeft > 0);
    return button;
}


        @Override public Object getCellEditorValue() { return label; }
    }
}
