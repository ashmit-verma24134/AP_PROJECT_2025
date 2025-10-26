package edu.univ.erp.ui.student;

import edu.univ.erp.data.SectionDao;
import edu.univ.erp.data.SectionDaoImpl;
import edu.univ.erp.data.SectionRow;
import edu.univ.erp.service.Result;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * CatalogPanel — wired to DB via SectionDao.
 */
public class CatalogPanel extends JPanel {
    private final CatalogModel model = new CatalogModel();
    private final JTable table = new JTable(model);
    private final JTextField txtSearch = new JTextField(20);
    private String studentId; // set by StudentPanel after login
    private boolean actionsEnabled = true;

    // inside CatalogPanel class
private RegistrationListener registrationListener;
public void setRegistrationListener(RegistrationListener l) { this.registrationListener = l; }


    public CatalogPanel() {
        setLayout(new BorderLayout(8,8));
        initUI();
        reloadFromDb(null); // initial load
    }

    private void doSearch() {
        String q = txtSearch.getText().trim();
        reloadFromDb(q.isEmpty() ? null : q);
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
    btnSearch.addActionListener(e -> {
        String q = txtSearch.getText().trim();
        reloadFromDb(q.isEmpty() ? null : q);
    });
    right.add(new JLabel("Search:"));
    right.add(txtSearch);
    right.add(btnSearch);
    top.add(right, BorderLayout.EAST);

    add(top, BorderLayout.NORTH);

    // table setup
    table.setRowHeight(56);                      // base height (increase for multi-line)
    table.setFillsViewportHeight(true);
    table.setIntercellSpacing(new Dimension(0,6)); // vertical breathing room
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setAutoCreateRowSorter(true);          // allow sorting by columns

    // IMPORTANT: allow column widths to be honored
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    // set preferred widths (adjust numbers to taste)
    SwingUtilities.invokeLater(() -> {
        int colCount = table.getColumnModel().getColumnCount();
        if (colCount >= 8) {
            table.getColumnModel().getColumn(0).setPreferredWidth(320); // Course (HTML)
            table.getColumnModel().getColumn(1).setPreferredWidth(80);  // Section
            table.getColumnModel().getColumn(2).setPreferredWidth(220); // Instructor
            table.getColumnModel().getColumn(3).setPreferredWidth(80);  // Credits
            table.getColumnModel().getColumn(4).setPreferredWidth(90);  // Capacity
            table.getColumnModel().getColumn(5).setPreferredWidth(90);  // Seats Left
            table.getColumnModel().getColumn(6).setPreferredWidth(220); // Schedule
            table.getColumnModel().getColumn(7).setPreferredWidth(140); // Action
        }
    });

    // Replace plain HtmlRenderer with wrapping-aware renderer for column 0
    table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            String raw = value == null ? "" : value.toString();

            // calculate available pixel width for this column
            int colWidth = tbl.getColumnModel().getColumn(column).getWidth();
            int wrapWidth = Math.max(colWidth - 12, 80); // leave padding, keep a sensible minimum

            // insert a div with exact width so Swing wraps the HTML
            l.setText("<html><div style='width:" + wrapWidth + "px;'>" + raw + "</div></html>");
            l.setVerticalAlignment(SwingConstants.TOP);
            l.setOpaque(true);
            return l;
        }
    });

    // action column setup (keep your existing renderers/editors)
    int actionCol = model.getColumnCount() - 1;
    table.getColumnModel().getColumn(actionCol).setCellRenderer(new RegisterButtonRenderer());
    table.getColumnModel().getColumn(actionCol).setCellEditor(new RegisterButtonEditor(new JButton("Register")));

    // add the table to scroll pane AFTER column config
    JScrollPane scroll = new JScrollPane(table);
    add(scroll, BorderLayout.CENTER);

    // When user resizes columns, repaint so the wrap-width renderer updates
    table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
        @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) { table.repaint(); }
        @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
        @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
        @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
        @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
    });

    // small footer
    JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton btnRefresh = new JButton("Refresh");
    btnRefresh.addActionListener(e -> {
        String q = txtSearch.getText().trim();
        reloadFromDb(q.isEmpty() ? null : q);
    });
    footer.add(btnRefresh);
    add(footer, BorderLayout.SOUTH);
}


    /** Called by StudentPanel after login */
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public void setActionsEnabled(boolean enabled) {
        this.actionsEnabled = enabled;
        table.setEnabled(enabled);
        repaint();
    }

    public boolean isActionsEnabled() { return actionsEnabled; }

    // --- DB loader ---
    public void reloadFromDb(String query) {
        setActionsEnabled(false);

        new SwingWorker<java.util.List<CatalogRow>, Void>() {
            @Override
            protected java.util.List<CatalogRow> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    SectionDao dao = new SectionDaoImpl(conn);
                    java.util.List<SectionRow> dbRows = dao.searchOpenSections(query);
                    java.util.List<CatalogRow> list = new java.util.ArrayList<>();
                    for (SectionRow r : dbRows) {
                        // Map DB SectionRow -> UI CatalogRow
                        String sectionNo = (r.sectionNo == null || r.sectionNo.isEmpty()) ? "001" : r.sectionNo;
                        list.add(new CatalogRow(r.sectionId, r.courseCode, r.title, sectionNo,
                                r.instructorName, r.credits, r.capacity, r.seatsLeft,
                                r.dayTime, r.year, r.semester));
                    }
                    return list;
                }
            }

            @Override
            protected void done() {
                try {
                    java.util.List<CatalogRow> loaded = get();
                    model.setRows(loaded);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CatalogPanel.this,
                            "Database error: " + ex.getMessage(), "Failed", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setActionsEnabled(true);
                }
            }
        }.execute();
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
        private final List<CatalogRow> rows = new ArrayList<>();

        public void setRows(List<CatalogRow> r) { rows.clear(); rows.addAll(r); fireTableDataChanged(); }
        public CatalogRow getRow(int r) { return rows.get(r); }

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
    class RegisterButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public RegisterButtonRenderer() { setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            int modelRow = row;
            if (table != null) {
                try { modelRow = table.convertRowIndexToModel(row); } catch (Exception ignored) {}
            }
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
            if (!actionsEnabled) { Toolkit.getDefaultToolkit().beep(); return; }

            JTable t = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, button);
            if (t == null) return;

            int viewRow = t.getEditingRow();
            if (viewRow < 0) viewRow = t.getSelectedRow();
            if (viewRow < 0) return;

            final int modelRow = t.convertRowIndexToModel(viewRow);
            final CatalogRow row = CatalogPanel.this.model.getRow(modelRow);

            if (row.seatsLeft <= 0) {
                JOptionPane.showMessageDialog(button, "This section is already full.", "Failed", JOptionPane.ERROR_MESSAGE);
                fireEditingStopped();
                return;
            }

            int conf = JOptionPane.showConfirmDialog(button,
                    "Register for " + row.courseCode + " (Section " + row.sectionNo + ")?",
                    "Confirm Registration", JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION) { fireEditingStopped(); return; }

            setActionsEnabled(false);

            new SwingWorker<Result, Void>() {
                @Override
                protected Result doInBackground() {
                    StudentService svc = new StudentService();
                    if (studentId == null || studentId.trim().isEmpty()) {
                        return Result.error("No student logged in.");
                    }
                    return svc.registerForSection(studentId, row.sectionId);
                }

@Override
protected void done() {
    try {
        Result res = get();
        if (res.success) {
            JOptionPane.showMessageDialog(button, res.message, "Success", JOptionPane.INFORMATION_MESSAGE);
            // reload catalog (preserve search)
            String q = txtSearch.getText().trim();
            reloadFromDb(q.isEmpty() ? null : q);

            // notify outer panels to refresh timetable/transcript
            if (registrationListener != null) {
                // call on EDT (we're already on EDT here) — safe
                registrationListener.onRegistrationChanged();
            }
        } else {
            JOptionPane.showMessageDialog(button, res.message, "Failed", JOptionPane.ERROR_MESSAGE);
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(button, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    } finally {
        setActionsEnabled(true);
        fireEditingStopped();
    }
}

            }.execute();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            int modelRow = row;
            try { modelRow = table.convertRowIndexToModel(row); } catch (Exception ignored) {}
            CatalogRow r = CatalogPanel.this.model.getRow(modelRow);
            label = (r.seatsLeft <= 0) ? "Full" : "Register";
            button.setText(label);
            button.setEnabled(actionsEnabled && r.seatsLeft > 0);
            return button;
        }

        @Override public Object getCellEditorValue() { return label; }
    }
}
