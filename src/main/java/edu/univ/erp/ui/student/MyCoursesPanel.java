package edu.univ.erp.ui.student;

import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.StudentDaoImpl;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * MyCoursesPanel — lists current courses for the logged-in student.
 * Implements RegistrationListener so other panels can notify it to refresh.
 */
public class MyCoursesPanel extends JPanel implements RegistrationListener {

    private String studentId;
    private DefaultTableModel model;
    private JTextField txtSearch;

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

        // Table setup
        String[] cols = {"Course Code", "Course Name", "Instructor", "Schedule", "Credits", "Status"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setGridColor(new Color(230,230,230));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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
                    List<Map<String,Object>> rows = get();
                    if (rows == null) return;
                    for (Map<String,Object> c : rows) {
                        model.addRow(new Object[]{
                            c.get("course_code"),
                            c.get("course_name"),
                            c.get("instructor"),
                            c.get("schedule"),
                            c.get("credits"),
                            c.get("status")
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
}
