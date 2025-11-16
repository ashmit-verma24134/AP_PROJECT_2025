package edu.univ.erp.ui.student;

import edu.univ.erp.data.RegistrationDAO;
import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.StudentDaoImpl;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

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
import edu.univ.erp.service.RegistrationEventBus;

import java.util.Map;

/**
 * MyCoursesPanel — lists current courses for the logged-in student and allows dropping before deadline.
 */
public class MyCoursesPanel extends JPanel implements RegistrationListener {

    private String studentId;
    private DefaultTableModel model;
    private JTextField txtSearch;

    // Backing rows so we can access original values (section_id, raw drop_deadline, etc.)
    private java.util.List<Map<String, Object>> rowsList;

    // Formatter for displayed deadline
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public MyCoursesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BACKGROUND);

        // Header
        JLabel header = new JLabel("🎓 My Courses");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setForeground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        add(header, BorderLayout.NORTH);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        searchPanel.setBackground(Theme.BACKGROUND);
        txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("Search");
        JButton btnRefresh = new JButton("Refresh");

        btnSearch.addActionListener(e -> reloadFromDb(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> reloadFromDb(null));

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh);
        add(searchPanel, BorderLayout.SOUTH);

        // Table setup - added Drop Deadline and Action column
        String[] cols = {"Course Code", "Course Name", "Instructor", "Schedule", "Credits", "Status", "Drop Deadline", "Action"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                // Only Action column (last) is editable to host the button editor
                return c == (getColumnCount() - 1);
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setGridColor(new Color(230,230,230));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Header styling
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);

        // Set preferred widths
        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(100); // code
        colModel.getColumn(1).setPreferredWidth(300); // name
        colModel.getColumn(2).setPreferredWidth(140); // instructor
        colModel.getColumn(3).setPreferredWidth(140); // schedule
        colModel.getColumn(4).setPreferredWidth(60);  // credits
        colModel.getColumn(5).setPreferredWidth(80);  // status
        colModel.getColumn(6).setPreferredWidth(120); // drop deadline
        colModel.getColumn(7).setPreferredWidth(80);  // action

        // Add button renderer/editor to last column
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

    /** Called by StudentPanel after login to set the active student and load courses. */
    public void setStudentId(String id) {
        this.studentId = id;
        reloadFromDb(null);
    }

    /**
     * Public reload method — used by UI and by the registration listener.
     * Query may be null to fetch all current courses.
     */
    public void reloadFromDb(String query) {
        model.setRowCount(0);
        if (studentId == null || studentId.isEmpty()) return;

        new SwingWorker<List<Map<String,Object>>, Void>() {
            @Override
            protected List<Map<String,Object>> doInBackground() throws Exception {
                try (Connection conn = DBConnection.getErpConnection()) {
                    StudentDao dao = new StudentDaoImpl(conn);
                    return dao.getCurrentCourses(studentId, query);
                }
            }

            @Override
            protected void done() {
                try {
                    rowsList = get();
                    if (rowsList == null) return;
                    model.setRowCount(0);
                    for (Map<String,Object> c : rowsList) {
                        // extract drop_deadline (could be java.sql.Date or String or null)
                        Object ddRaw = c.get("drop_deadline");
                        String ddStr = "N/A";
                        if (ddRaw != null) {
                            if (ddRaw instanceof Date) {
                                LocalDate ld = ((Date) ddRaw).toLocalDate();
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
                                "Drop" // button label
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

    /** RegistrationListener impl — called when user registers/drops a course elsewhere. */
    @Override
    public void onRegistrationChanged() {
        // refresh visible course list
        reloadFromDb(null);
    }

    // ---------- Button cell renderer/editor & helpers ----------

    private boolean canDropRow(int rowIndex) {
        if (rowsList == null || rowIndex < 0 || rowIndex >= rowsList.size()) return false;
        Map<String, Object> r = rowsList.get(rowIndex);
        Object ddRaw = r.get("drop_deadline");
        if (ddRaw == null) {
            // policy: allow drop when drop_deadline is NULL. Change to `return false;` to disallow.
            return true;
        }
        LocalDate deadline;
        if (ddRaw instanceof Date) deadline = ((Date) ddRaw).toLocalDate();
        else if (ddRaw instanceof LocalDate) deadline = (LocalDate) ddRaw;
        else {
            try { deadline = LocalDate.parse(ddRaw.toString()); } catch (Exception ex) { return true; }
        }
        return !LocalDate.now().isAfter(deadline); // allow on or before deadline
    }

    private long getSectionIdAt(int rowIndex) {
        Map<String, Object> r = rowsList.get(rowIndex);
        Object sid = r.get("section_id");
        if (sid instanceof Number) return ((Number) sid).longValue();
        return Long.parseLong(String.valueOf(sid));
    }

    // Renderer: show a JButton-looking cell (disabled when cannot drop)
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            setEnabled(canDropRow(row));
            return this;
        }
    }

    // Editor: handles the drop action when button clicked
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button = new JButton();
        private int currentRow;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentRow = row;
            button.setText(value == null ? "" : value.toString());
            button.setEnabled(canDropRow(row));
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // perform drop in background
                long sectionId = getSectionIdAt(currentRow);
                long studId;
                try {
                    studId = Long.parseLong(studentId);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(MyCoursesPanel.this, "Invalid student id: " + studentId);
                    return "Drop";
                }

                int confirm = JOptionPane.showConfirmDialog(MyCoursesPanel.this,
                        "Are you sure you want to drop this section?",
                        "Confirm Drop",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return "Drop";
                }

                // run drop in background
                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        RegistrationDAO dao = new RegistrationDAO();
                        return dao.dropEnrollment(studId, sectionId);
                    }

                    @Override
                    protected void done() {
                        try {
                            boolean ok = get();
                            if (ok) {
    JOptionPane.showMessageDialog(MyCoursesPanel.this, "Dropped successfully.");
    reloadFromDb(null); // refresh this panel
    RegistrationEventBus.get().notifyChange(); // 
                            } else {
                                JOptionPane.showMessageDialog(MyCoursesPanel.this,
                                        "Could not drop: deadline passed or not enrolled.",
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
}
