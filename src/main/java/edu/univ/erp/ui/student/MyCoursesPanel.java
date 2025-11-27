package edu.univ.erp.ui.student;

import edu.univ.erp.service.CourseService;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.RegistrationEventBus;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.service.Result;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * MyCoursesPanel — lists current courses for the logged-in student and allows dropping before deadline.
 * This version DOES NOT talk to the DB directly: it uses StudentService.
 */
public class MyCoursesPanel extends JPanel implements RegistrationEventBus.Listener {

    private String studentId;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private boolean actionsEnabled = true;
    private boolean maintenanceMode = false;
    private java.util.List<Map<String, Object>> rowsList;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private JTable tableReference;
    private JTextField txtSearchReference;

    // Service injected from outside (MainFrame / StudentPanel)
    private final StudentService studentService;

    public MyCoursesPanel(StudentService studentService) {
        this.studentService = studentService;
        initUi();
    }


    public MyCoursesPanel(EnrollmentService enrollmentService, CourseService courseService,
            SectionService sectionService) {
                this.studentService = null;

                
        //TODO Auto-generated constructor stub
    }


    private void initUi() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BACKGROUND);

        JLabel header = new JLabel("🎓 My Courses");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setForeground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        add(header, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        searchPanel.setBackground(Theme.BACKGROUND);
        txtSearch = new JTextField(20);
        this.txtSearchReference = txtSearch;

        JButton btnSearch = new JButton("Search");
        JButton btnRefresh = new JButton("Refresh");

        btnSearch.addActionListener(e -> reloadFromDb(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> reloadFromDb(null));

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh);
        add(searchPanel, BorderLayout.SOUTH);

        String[] cols = {"Course Code", "Course Name", "Instructor", "Schedule", "Credits", "Status", "Drop Deadline", "Action"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == (getColumnCount() - 1) && !maintenanceMode && actionsEnabled;
            }
        };

        JTable table = new JTable(model);
        this.tableReference = table;

        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setGridColor(new Color(230,230,230));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);

        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(100);
        colModel.getColumn(1).setPreferredWidth(300);
        colModel.getColumn(2).setPreferredWidth(140);
        colModel.getColumn(3).setPreferredWidth(140);
        colModel.getColumn(4).setPreferredWidth(60);
        colModel.getColumn(5).setPreferredWidth(80);
        colModel.getColumn(6).setPreferredWidth(120);
        colModel.getColumn(7).setPreferredWidth(80);

        int actionColumnIndex = cols.length - 1;
        table.getColumnModel().getColumn(actionColumnIndex).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(actionColumnIndex).setCellEditor(new ButtonEditor(new JCheckBox()));

        RoundedPanel tablePanel = new RoundedPanel(20);
        tablePanel.setBackground(Theme.CARD_BG);
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    public void setStudentId(String id) {
        this.studentId = id;
        reloadFromDb(null);
    }

    /**
     * Now calls StudentService.getCurrentCourses(...) in background.
     */
    public void reloadFromDb(String query) {
        model.setRowCount(0);
        if (studentId == null || studentId.isEmpty()) return;

        new SwingWorker<List<Map<String,Object>>, Void>() {
            @Override
            protected List<Map<String,Object>> doInBackground() throws Exception {
                // UI no longer accesses DB or DAOs directly
                return studentService.getCurrentCourses(studentId, query);
            }

            @Override
            protected void done() {
                try {
                    rowsList = get();
                    if (rowsList == null) return;
                    model.setRowCount(0);
                    for (Map<String,Object> c : rowsList) {
                        Object ddRaw = c.get("drop_deadline");
                        String ddStr = "N/A";
                        if (ddRaw != null) {
                            if (ddRaw instanceof java.sql.Date) {
                                LocalDate ld = ((java.sql.Date) ddRaw).toLocalDate();
                                ddStr = ld.format(fmt);
                            } else if (ddRaw instanceof java.time.LocalDate) {
                                ddStr = ((java.time.LocalDate) ddRaw).format(fmt);
                            } else {
                                ddStr = ddRaw.toString();
                            }
                        }

                        model.addRow(new Object[]{
                                c.get("course_code"),
                                c.get("course_name"),
                                c.get("instructor"),
                                c.get("schedule"),
                                c.get("credits"),
                                c.get("status"),
                                ddStr,
                                "Drop"
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MyCoursesPanel.this,
                            "Error loading courses: " + e.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    @Override
    public void onRegistrationChanged() {
        reloadFromDb(null);
    }

    // ---------- Button renderer/editor & helpers (unchanged, but drop action uses StudentService) ----------

    private boolean canDropRow(int viewRowIndex) {
        if (rowsList == null || viewRowIndex < 0) return false;
        int modelRowIndex = viewRowIndex;
        try {
            if (tableReference != null) modelRowIndex = tableReference.convertRowIndexToModel(viewRowIndex);
        } catch (Exception ignore) {}

        if (modelRowIndex < 0 || modelRowIndex >= rowsList.size()) return false;
        Map<String, Object> r = rowsList.get(modelRowIndex);
        Object ddRaw = r.get("drop_deadline");
        if (ddRaw == null) return true;
        LocalDate deadline;
        if (ddRaw instanceof java.sql.Date) {
            deadline = ((java.sql.Date) ddRaw).toLocalDate();
        } else if (ddRaw instanceof LocalDate) {
            deadline = (LocalDate) ddRaw;
        } else {
            try { deadline = LocalDate.parse(ddRaw.toString()); }
            catch (Exception ex) { return true; }
        }
        return !LocalDate.now().isAfter(deadline);
    }

    private long getSectionIdAt(int viewRowIndex) {
        int modelRowIndex = viewRowIndex;
        try {
            if (tableReference != null) modelRowIndex = tableReference.convertRowIndexToModel(viewRowIndex);
        } catch (Exception ignore) {}
        Map<String, Object> r = rowsList.get(modelRowIndex);
        Object sid = r.get("section_id");
        if (sid instanceof Number) return ((Number) sid).longValue();
        return Long.parseLong(String.valueOf(sid));
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            if (maintenanceMode || !actionsEnabled) {
                setEnabled(false);
                setBackground(new Color(220, 220, 220));
                setForeground(Color.DARK_GRAY);
            } else {
                boolean ok = canDropRow(row);
                setEnabled(ok);
                if (!ok) {
                    setBackground(new Color(220, 220, 220));
                    setForeground(Color.DARK_GRAY);
                } else {
                    setBackground(Theme.PRIMARY);
                    setForeground(Color.WHITE);
                }
            }
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button = new JButton();
        private int currentRow;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentRow = row;
            button.setText(value == null ? "" : value.toString());
            if (maintenanceMode || !actionsEnabled) button.setEnabled(false);
            else button.setEnabled(canDropRow(row));
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (maintenanceMode || !actionsEnabled) return "Drop";

            if (isPushed) {
                long sectionId = getSectionIdAt(currentRow);
                int confirm = JOptionPane.showConfirmDialog(MyCoursesPanel.this,
                        "Are you sure you want to drop this section?",
                        "Confirm Drop",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    isPushed = false;
                    return "Drop";
                }

                // call service in background
                new SwingWorker<Result, Void>() {
                    @Override
                    protected Result doInBackground() throws Exception {
                        // StudentService handles DB and business rules
                        return studentService.dropSection(studentId, sectionId);
                    }

                    @Override
                    protected void done() {
                        try {
                            Result res = get();
                            // Try common Result API: isOk/isSuccess + getMessage()
                            boolean ok = false;
                            String msg = null;
                            try {
                                ok = (boolean) res.getClass().getMethod("isOk").invoke(res);
                                msg = (String) res.getClass().getMethod("getMessage").invoke(res);
                            } catch (NoSuchMethodException nsme) {
                                // fallback: try isSuccess/getMessage
                                try {
                                    ok = (boolean) res.getClass().getMethod("isSuccess").invoke(res);
                                    msg = (String) res.getClass().getMethod("getMessage").invoke(res);
                                } catch (Exception ignored) {}
                            } catch (Exception ignored) {}

                            if (ok) {
                                JOptionPane.showMessageDialog(MyCoursesPanel.this, msg == null ? "Dropped successfully." : msg);
                                reloadFromDb(null);
                                RegistrationEventBus.get().notifyChange();
                            } else {
                                JOptionPane.showMessageDialog(MyCoursesPanel.this,
                                        msg == null ? "Could not drop the section." : msg,
                                        "Drop Failed",
                                        JOptionPane.WARNING_MESSAGE);
                                reloadFromDb(null);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(MyCoursesPanel.this,
                                    "Error while dropping: " + ex.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }.execute();
            }

            isPushed = false;
            return "Drop";
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    public void setActionsEnabled(boolean enabled) {
        this.actionsEnabled = enabled;
        tablePanelSetEnabled(enabled && !maintenanceMode);
        repaint();
    }

    private void tablePanelSetEnabled(boolean enabled) {
        try {
            JTable t = this.tableReference;
            if (t == null) return;

            t.setEnabled(enabled);
            TableCellEditor active = t.getCellEditor();
            if (active != null) {
                try { active.stopCellEditing(); } catch (Exception ignore) {}
            }

            for (int col = 0; col < t.getColumnCount(); col++) {
                TableCellEditor ed = t.getColumnModel().getColumn(col).getCellEditor();
                if (ed instanceof javax.swing.DefaultCellEditor) {
                    Component comp = ((javax.swing.DefaultCellEditor) ed).getComponent();
                    if (comp != null) comp.setEnabled(enabled);
                }
            }

            try { if (this.txtSearchReference != null) this.txtSearchReference.setEnabled(enabled); } catch (Exception ignore) {}

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private JTable findTable() {
        try {
            for (Component c : this.getComponents()) {
                if (c instanceof JPanel) {
                    for (Component inner : ((JPanel)c).getComponents()) {
                        if (inner instanceof JScrollPane) {
                            JScrollPane sp = (JScrollPane) inner;
                            JViewport vp = sp.getViewport();
                            Component view = vp.getView();
                            if (view instanceof JTable) return (JTable) view;
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        return null;
    }

    public void setMaintenanceMode(boolean on) {
        this.maintenanceMode = on;
        setActionsEnabled(!on);
        if (tableReference != null) tableReference.repaint();
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        try { RegistrationEventBus.get().register(this); } catch (Exception ignore) {}
    }

    @Override
    public void removeNotify() {
        try { RegistrationEventBus.get().unregister(this); } catch (Exception ignore) {}
        super.removeNotify();
    }
}