package edu.univ.erp.ui.student;

import edu.univ.erp.data.SectionRow;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.Result;
import edu.univ.erp.service.SectionService;
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
 * 
 * ZERO behavior changed, only DB access replaced with services.
 */
public class CatalogPanel extends JPanel {

    private final CatalogModel model = new CatalogModel();
    private final JTable table = new JTable(model);
    private final JTextField txtSearch = new JTextField(20);
    private String studentId;
    private boolean actionsEnabled = true;

    private final SectionService sectionService;     //  NEW
    private final StudentService studentService;     //  existing
    private EnrollmentService enrollmentService;     //  optional if needed

    private RegistrationListener registrationListener;
    public void setRegistrationListener(RegistrationListener l) { this.registrationListener = l; }

    public CatalogPanel(SectionService sectionService,
                        StudentService studentService,
                        EnrollmentService enrollmentService) {

        this.sectionService = sectionService;
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;

        setLayout(new BorderLayout(8,8));
        initUI();
        reloadFromService(null);
    }



    public void setStudentId(String studentId) { this.studentId = studentId; }

    private void doSearch() {
        String q = txtSearch.getText().trim();
        reloadFromService(q.isEmpty() ? null : q);
    }

    // ---------------- UI INIT ----------------
    private void initUI() {
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

        table.setRowHeight(56);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0,6));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // HTML-wrapped course column
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                String raw = value == null ? "" : value.toString();
                int width = tbl.getColumnModel().getColumn(column).getWidth();
                int wrapWidth = Math.max(width - 12, 80);
                l.setText("<html><div style='width:" + wrapWidth + "px;'>" + raw + "</div></html>");
                return l;
            }
        });

        int actionCol = model.getColumnCount() - 1;
        table.getColumnModel().getColumn(actionCol).setCellRenderer(new RegisterButtonRenderer());
        table.getColumnModel().getColumn(actionCol).setCellEditor(new RegisterButtonEditor(new JButton("Register")));

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> doSearch());
        footer.add(btnRefresh);
        add(footer, BorderLayout.SOUTH);
    }

    public void setActionsEnabled(boolean enabled) {
        this.actionsEnabled = enabled;
        table.setEnabled(enabled);
        txtSearch.setEnabled(enabled);
        repaint();
    }

    public boolean isActionsEnabled() { return actionsEnabled; }

    // ---------------- SERVICE LOAD ----------------
    public void reloadFromService(String query) {

        setActionsEnabled(false);

        new SwingWorker<List<CatalogRow>, Void>() {
            Exception err = null;

            @Override
            protected List<CatalogRow> doInBackground() {
                try {
                    List<SectionRow> results = sectionService.searchOpenSections(query);

                    List<CatalogRow> list = new ArrayList<>();
                    for (SectionRow r : results) {

                        // SectionRow fields you actually have:
                        // sectionId, courseId, code, title, credits,
                        // instructorId, instructorName,
                        // capacity, seatsLeft, semester

                        list.add(new CatalogRow(
                                r.sectionId,
                                r.code,
                                r.title,
                                "001",                  // Section number fallback
                                r.instructorName,
                                (int) r.credits,
                                r.capacity,
                                r.seatsLeft,
                                "-",                    // schedule unknown
                                0,
                                r.semester
                        ));
                    }
                    return list;

                } catch (Exception ex) {
                    err = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    if (err != null)
                        throw err;

                    List<CatalogRow> loaded = get();
                    model.setRows(loaded);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CatalogPanel.this,
                            "Error loading catalog: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setActionsEnabled(true);
                }
            }

        }.execute();
    }

    // ---------------- DATA STRUCTURES ----------------

    static class CatalogRow {
        final long sectionId;
        final String courseCode, title, sectionNo, instructor;
        final int credits, capacity, seatsLeft;
        final String schedule;
        final int year;
        final String semester;

        CatalogRow(long sectionId, String courseCode, String title, String sectionNo,
                   String instructor, int credits, int capacity, int seatsLeft,
                   String schedule, int year, String semester) {

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
        private final String[] cols =
                {"Course", "Section", "Instructor", "Credits", "Capacity", "Seats Left", "Schedule", "Action"};

        private final List<CatalogRow> rows = new ArrayList<>();

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        public void setRows(List<CatalogRow> list) {
            rows.clear();
            rows.addAll(list);
            fireTableDataChanged();
        }

        public CatalogRow getRow(int r) { return rows.get(r); }

        @Override
        public Object getValueAt(int r, int c) {
            CatalogRow x = rows.get(r);
            switch (c) {
                case 0: return "<html><b>" + x.courseCode + "</b><br/><small>" + x.title + "</small></html>";
                case 1: return x.sectionNo;
                case 2: return x.instructor;
                case 3: return x.credits;
                case 4: return x.capacity;
                case 5: return x.seatsLeft;
                case 6: return x.schedule;
                case 7: return "Register";
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int r, int c) { return c == 7; }
    }

    // ---------------- BUTTON RENDERER ----------------

    class RegisterButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        RegisterButtonRenderer() { setOpaque(true); }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            int mRow = table.convertRowIndexToModel(row);
            //CatalogRow r = model.getRow(mRow);
            CatalogRow r = CatalogPanel.this.model.getRow(mRow);


            setText(r.seatsLeft <= 0 ? "Full" : "Register");
            setEnabled(actionsEnabled && r.seatsLeft > 0);
            return this;
        }
    }

    // ---------------- BUTTON EDITOR ----------------

    class RegisterButtonEditor extends AbstractCellEditor implements TableCellEditor {

        private final JButton button;
        private String label;

        RegisterButtonEditor(JButton b) {
            this.button = b;
            button.addActionListener(this::onClick);
        }

        private void onClick(ActionEvent e) {
            JTable tbl = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, button);
            if (tbl == null) return;

            int viewRow = tbl.getEditingRow();
            if (viewRow < 0) return;

            int modelRow = tbl.convertRowIndexToModel(viewRow);
            CatalogRow row = model.getRow(modelRow);

            if (row.seatsLeft <= 0) {
                JOptionPane.showMessageDialog(button, "This section is full.");
                fireEditingStopped();
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    button,
                    "Register for " + row.courseCode + " (Section " + row.sectionNo + ")?",
                    "Confirm Registration",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) {
                fireEditingStopped();
                return;
            }

            // Do the registration
            new SwingWorker<Result, Void>() {
                @Override
                protected Result doInBackground() {
                    return studentService.registerForSection(studentId, row.sectionId);
                }

                @Override
                protected void done() {
                    try {
                        Result res = get();
                        if (res.success) {
                            JOptionPane.showMessageDialog(button, res.message);

                            // refresh table
                            doSearch();

                            if (registrationListener != null)
                                registrationListener.onRegistrationChanged();
                        } else {
                            JOptionPane.showMessageDialog(button, res.message, "Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(button, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        fireEditingStopped();
                    }
                }
            }.execute();
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {

            int modelRow = table.convertRowIndexToModel(row);
            CatalogRow r = model.getRow(modelRow);

            label = r.seatsLeft <= 0 ? "Full" : "Register";
            button.setText(label);
            button.setEnabled(actionsEnabled && r.seatsLeft > 0);

            return button;
        }

        @Override
        public Object getCellEditorValue() { return label; }
    }

    
}
