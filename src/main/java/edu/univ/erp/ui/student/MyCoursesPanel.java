package edu.univ.erp.ui.student;

import edu.univ.erp.data.RegistrationDAO;
import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.StudentDaoImpl;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.service.RegistrationEventBus;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * MyCoursesPanel — lists current courses for the logged-in student and allows dropping before deadline.
 */
public class MyCoursesPanel extends JPanel implements RegistrationEventBus.Listener {

    private final EnrollmentService enrollmentService; // <-- added
    private String studentId;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private boolean actionsEnabled = true;
    private boolean maintenanceMode = false;

    private java.util.List<Map<String, Object>> rowsList;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private JTable tableReference;
    private JTextField txtSearchReference;

    // BACKWARD COMPATIBLE CONSTRUCTOR (no service)
    public MyCoursesPanel() {
        this(null);
    }

    // NEW CONSTRUCTOR WITH SERVICE
    public MyCoursesPanel(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
        initUI();
    }

    private void initUI() {
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

        String[] cols = {
                "Course Code", "Course Name", "Instructor", "Schedule",
                "Credits", "Status", "Drop Deadline", "Action"
        };

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

    public void reloadFromDb(String query) {
        model.setRowCount(0);
        if (studentId == null || studentId.isEmpty()) return;

        new SwingWorker<List<Map<String,Object>>, Void>() {
            @Override protected List<Map<String,Object>> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    StudentDao dao = new StudentDaoImpl(conn);
                    return dao.getCurrentCourses(studentId, query);
                }
            }

            @Override protected void done() {
                try {
                    rowsList = get();
                    if (rowsList == null) return;

                    model.setRowCount(0);
                    for (Map<String,Object> c : rowsList) {
                        Object ddRaw = c.get("drop_deadline");
                        String ddStr = "N/A";
                        if (ddRaw != null) {
                            if (ddRaw instanceof Date)
                                ddStr = ((Date) ddRaw).toLocalDate().format(fmt);
                            else if (ddRaw instanceof LocalDate)
                                ddStr = ((LocalDate) ddRaw).format(fmt);
                            else ddStr = ddRaw.toString();
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
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    @Override
    public void onRegistrationChanged() {
        reloadFromDb(null);
    }

    private boolean canDropRow(int viewRow) {
        if (rowsList == null || viewRow < 0) return false;
        int modelRow = tableReference.convertRowIndexToModel(viewRow);

        Map<String, Object> r = rowsList.get(modelRow);
        Object ddRaw = r.get("drop_deadline");
        if (ddRaw == null) return true;

        LocalDate deadline;
        try {
            if (ddRaw instanceof Date)
                deadline = ((Date) ddRaw).toLocalDate();
            else if (ddRaw instanceof LocalDate)
                deadline = (LocalDate) ddRaw;
            else
                deadline = LocalDate.parse(ddRaw.toString());
        } catch (Exception ex) {
            return true;
        }

        return !LocalDate.now().isAfter(deadline);
    }

    private long getSectionIdAt(int viewRowIndex) {
        int modelRow = tableReference.convertRowIndexToModel(viewRowIndex);
        Map<String,Object> r = rowsList.get(modelRow);
        Object sid = r.get("section_id");
        if (sid instanceof Number) return ((Number) sid).longValue();
        return Long.parseLong(String.valueOf(sid));
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
              boolean isSelected, boolean hasFocus, int row, int col) {

            setText(value == null ? "" : value.toString());

            if (maintenanceMode || !actionsEnabled) {
                setEnabled(false);
                setBackground(new Color(220,220,220));
                setForeground(Color.DARK_GRAY);
            } else {
                boolean ok = canDropRow(row);
                setEnabled(ok);
                if (!ok) {
                    setBackground(new Color(220,220,220));
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
                boolean isSelected, int row, int col) {

            currentRow = row;
            button.setText(value == null ? "" : value.toString());

            button.setEnabled(!maintenanceMode && actionsEnabled && canDropRow(row));
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {

            if (!isPushed || maintenanceMode || !actionsEnabled)
                return "Drop";

            long sectionId = getSectionIdAt(currentRow);
            long studId;
            try { studId = Long.parseLong(studentId); }
            catch (Exception ex) { return "Drop"; }

            int confirm = JOptionPane.showConfirmDialog(MyCoursesPanel.this,
                    "Drop this section?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return "Drop";

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {

                    // SERVICE path
                    if (enrollmentService != null) {
                        try {
                            return enrollmentService.drop(studId, sectionId);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    // FALLBACK
                    RegistrationDAO dao = new RegistrationDAO();
                    return dao.dropEnrollment(studId, sectionId);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(MyCoursesPanel.this, "Dropped successfully.");
                            reloadFromDb(null);
                            RegistrationEventBus.get().notifyChange();
                        } else {
                            JOptionPane.showMessageDialog(MyCoursesPanel.this,
                                    "Failed: deadline passed or not enrolled.",
                                    "Drop Failed", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(MyCoursesPanel.this,
                                "Error: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();

            isPushed = false;
            return "Drop";
        }
    }

    public void setActionsEnabled(boolean enabled) {
        this.actionsEnabled = enabled;
        repaint();
    }

    public void setMaintenanceMode(boolean on) {
        this.maintenanceMode = on;
        setActionsEnabled(!on);
        if (tableReference != null) tableReference.repaint();
    }

    @Override public void addNotify() {
        super.addNotify();
        RegistrationEventBus.get().register(this);
    }

    @Override public void removeNotify() {
        RegistrationEventBus.get().unregister(this);
        super.removeNotify();
    }
}
