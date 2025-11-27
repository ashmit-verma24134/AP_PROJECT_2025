package edu.univ.erp.ui.Instructor;

import edu.univ.erp.data.SectionDaoImpl;
import edu.univ.erp.data.SectionRow;
import edu.univ.erp.service.CourseService;
import edu.univ.erp.service.EnrollmentService;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Instructor MyCoursesPanel — now supports CourseService & EnrollmentService.
 * Fully backwards compatible with original SectionDaoImpl logic.
 *
 * Usage:
 *   new MyCoursesPanel(courseService, enrollmentService)
 *   or old constructor new MyCoursesPanel()
 */
public class MyCoursesPanel extends JPanel {

    private final DefaultTableModel model;
    private final JComboBox<String> semesterFilter;
    private final JTable table;
    private final JTextField searchField;

    private long currentInstructorId = 0L;
    private String currentTerm = null;
    private volatile boolean maintenanceOn = false;

    /** Optional services (null → fallback to DAO) */
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    // ------------------------ CONSTRUCTORS ------------------------

    /** Legacy constructor (no services) */
    public MyCoursesPanel() {
        this(null, null);
    }

    /** New constructor supporting CourseService + EnrollmentService */
    public MyCoursesPanel(CourseService courseService, EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ========================================
        // HEADER
        // ========================================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.PRIMARY);
        headerPanel.setPreferredSize(new Dimension(100, 65));

        JLabel title = new JLabel("My Courses and Sections");
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 15));

        headerPanel.add(title, BorderLayout.WEST);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.PRIMARY);
        top.add(headerPanel, BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);

        // ========================================
        // CONTENT SCROLL PANEL
        // ========================================
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Theme.BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // ========================================
        // FILTER BAR
        // ========================================
        JPanel filterPanel = new RoundedPanel(20);
        filterPanel.setBackground(Theme.CARD_BG);
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblSemester = new JLabel("Semester:");
        semesterFilter = new JComboBox<>(new String[]{"All"});
        semesterFilter.addActionListener(e -> applyFilters());

        loadSemesterOptionsAsync();

        JLabel lblSearch = new JLabel("Search:");
        searchField = new JTextField(20);
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) applyFilters();
            }
        });

        JButton btnSearch = new JButton("Search");
        btnSearch.addActionListener(e -> applyFilters());

        filterPanel.add(lblSemester);
        filterPanel.add(semesterFilter);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(lblSearch);
        filterPanel.add(searchField);
        filterPanel.add(btnSearch);

        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(filterPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // ========================================
        // TABLE
        // ========================================
        RoundedPanel tableCard = new RoundedPanel(25);
        tableCard.setBackground(Theme.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel lblTableTitle = new JLabel("Courses List");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableCard.add(lblTableTitle, BorderLayout.NORTH);

        String[] cols = {
                "Course Code", "Title", "Semester",
                "Day / Time", "Room", "Capacity", "Students"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }

            @Override public Class<?> getColumnClass(int i) {
                return (i == 5 || i == 6) ? Integer.class : String.class;
            }
        };

        table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setAutoCreateRowSorter(true);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        tableCard.add(sp, BorderLayout.CENTER);

        contentPanel.add(tableCard);
        contentPanel.add(Box.createVerticalStrut(30));

        model.addRow(new Object[]{"—", "No data loaded", "—", "-", "-", 0, 0});
    }

    // ================================================================
    // SEMESTER DROPDOWN LOADING
    // ================================================================
    private void loadSemesterOptionsAsync() {
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() {
                List<String> out = new ArrayList<>();
                String sql = """
                    SELECT DISTINCT semester, year
                    FROM sections
                    WHERE semester IS NOT NULL AND semester <> ''
                    ORDER BY year DESC, semester DESC
                """;

                try (Connection c = DBConnection.getErpConnection();
                     PreparedStatement ps = c.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        String sem = rs.getString("semester");
                        int yr = rs.getInt("year");
                        String term = sem == null ? "" : sem.trim();
                        if (!term.isEmpty()) term += " " + yr;
                        if (!term.isEmpty()) out.add(term);
                    }
                } catch (Exception ignored) {}
                return out;
            }

            @Override protected void done() {
                try {
                    List<String> terms = get();
                    DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
                    m.addElement("All");
                    for (String t : terms) m.addElement(t);
                    semesterFilter.setModel(m);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ================================================================
    // PUBLIC API
    // ================================================================
    public void loadForInstructor(long instructorId, String term) {
        this.currentInstructorId = instructorId;
        this.currentTerm = term;
        loadFromDbAsync();
    }

    public void refresh() {
        loadFromDbAsync();
    }

    // ================================================================
    // DB LOADING (service or fallback to DAO)
    // ================================================================
    private void loadFromDbAsync() {

        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            model.addRow(new Object[]{"—", "Loading...", "—", "-", "-", 0, 0});
            table.setEnabled(false);
        });

        new SwingWorker<List<SectionRow>, Void>() {

            Exception error = null;

            @Override
            protected List<SectionRow> doInBackground() {

                try {
                    // -------------------------
                    // SERVICE MODE
                    // -------------------------
                    if (enrollmentService != null) {
                        maintenanceOn = false; // Instructor view maintenance handled globally

                        // enrollmentService.getInstructorSections() already returns SectionRow[]
                        return enrollmentService.getInstructorSections(currentInstructorId, currentTerm);
                    }

                    // -------------------------
                    // DAO FALLBACK
                    // -------------------------
                    try (Connection conn = DBConnection.getErpConnection()) {
                        SectionDaoImpl dao = new SectionDaoImpl(conn);
                        maintenanceOn = dao.isMaintenanceOn();
                        return dao.getSectionsByInstructor(currentInstructorId, currentTerm);
                    }

                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                model.setRowCount(0);

                if (error != null) {
                    model.addRow(new Object[]{"—", "Failed: " + error.getMessage(), "—", "-", "-", 0, 0});
                    error.printStackTrace();
                    toggleReadOnly(maintenanceOn);
                    return;
                }

                try {
                    List<SectionRow> rows = get();

                    if (rows == null || rows.isEmpty()) {
                        model.addRow(new Object[]{"—", "No sections found", "—", "-", "-", 0, 0});
                        toggleReadOnly(maintenanceOn);
                        return;
                    }

                    List<String> newTerms = new ArrayList<>();

                    for (SectionRow r : rows) {
                        String termStr = (r.semester == null || r.semester.isBlank())
                                ? String.valueOf(r.year)
                                : r.semester + " " + r.year;

                        if (!newTerms.contains(termStr)) newTerms.add(termStr);

                        int enrolled = r.capacity - r.seatsLeft;
                        if (enrolled < 0) enrolled = 0;

                        model.addRow(new Object[]{
                                r.courseCode,
                                r.title,
                                termStr,
                                r.dayTime == null ? "-" : r.dayTime,
                                r.room == null ? "-" : r.room,
                                r.capacity,
                                enrolled
                        });
                    }

                    // If combo had only "All", populate from data
                    if (semesterFilter.getItemCount() <= 1 && !newTerms.isEmpty()) {
                        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
                        m.addElement("All");
                        for (String t : newTerms) m.addElement(t);
                        semesterFilter.setModel(m);
                    }

                    toggleReadOnly(maintenanceOn);
                    applyFilters();

                } catch (Exception ex) {
                    model.addRow(new Object[]{"—", "Failed: " + ex.getMessage(), "—", "-", "-", 0, 0});
                }
            }
        }.execute();
    }

    // ================================================================
    // TABLE FILTERING
    // ================================================================
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String filterTerm = (String) semesterFilter.getSelectedItem();

        TableRowSorter<TableModel> sorter =
                (TableRowSorter<TableModel>) table.getRowSorter();

        if (sorter == null) {
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
        }

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> e) {
                String code = e.getStringValue(0).toLowerCase();
                String title = e.getStringValue(1).toLowerCase();
                String term = e.getStringValue(2);

                boolean matchSearch =
                        searchText.isEmpty() ||
                        code.contains(searchText) ||
                        title.contains(searchText);

                boolean matchTerm =
                        filterTerm.equals("All") ||
                        term.equals(filterTerm);

                return matchSearch && matchTerm;
            }
        });
    }

    // ================================================================
    // MAINTENANCE READ-ONLY MODE
    // ================================================================
    private void toggleReadOnly(boolean on) {
        table.setEnabled(!on);
        searchField.setEnabled(!on);
        semesterFilter.setEnabled(!on);
    }
}
