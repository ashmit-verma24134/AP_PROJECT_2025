package edu.univ.erp.ui.Instructor;

import edu.univ.erp.data.SectionDaoImpl;
import edu.univ.erp.data.SectionRow;
import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;

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
 * MyCoursesPanel — lists all courses taught by instructor.
 * Loads real data from DB via SectionDaoImpl and is maintenance-aware.
 *
 * Use:
 *   myCoursesPanel.loadForInstructor(instructorId, "Spring 2025");
 */
public class MyCoursesPanel extends JPanel {
    private final DefaultTableModel model;
    private final JComboBox<String> semesterFilter;
    private final JTable table;
    private final JTextField searchField;
    private final JLabel maintenanceBanner;

    // state for the current load
    private long currentInstructorId = 0L;
    private String currentTerm = null;
    private volatile boolean maintenanceOn = false;

    public MyCoursesPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // === HEADER ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.PRIMARY);
        headerPanel.setPreferredSize(new Dimension(100, 65));

        JLabel title = new JLabel("My Courses and Sections");
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 15));

        headerPanel.add(title, BorderLayout.WEST);

        // maintenance banner (hidden by default)
        maintenanceBanner = new JLabel("MAINTENANCE MODE: system is read-only", SwingConstants.CENTER);
        maintenanceBanner.setOpaque(true);
        maintenanceBanner.setBackground(new Color(200, 50, 50));
        maintenanceBanner.setForeground(Color.WHITE);
        maintenanceBanner.setFont(new Font("Segoe UI", Font.BOLD, 14));
        maintenanceBanner.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        maintenanceBanner.setVisible(false);

        // top container
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.PRIMARY);
        top.add(headerPanel, BorderLayout.NORTH);
        top.add(maintenanceBanner, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);

        // === SCROLLABLE CONTENT ===
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Theme.BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // === FILTER BAR ===
        JPanel filterPanel = new RoundedPanel(20);
        filterPanel.setBackground(Theme.CARD_BG);
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblSemester = new JLabel("Semester:");
        lblSemester.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // start with only "All" and then populate from DB asynchronously
        semesterFilter = new JComboBox<>(new String[]{"All"});
        semesterFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        semesterFilter.addActionListener(e -> applyFilters());

        // load semesters from DB
        loadSemesterOptionsAsync();

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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

        // === TABLE SECTION ===
        RoundedPanel tableCard = new RoundedPanel(25);
        tableCard.setBackground(Theme.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel lblTableTitle = new JLabel("Courses List");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        tableCard.add(lblTableTitle, BorderLayout.NORTH);

        // Columns: Course Code, Title, Semester, Day/Time, Room, Capacity, Students
        String[] cols = {
                "Course Code", "Title", "Semester",
                "Day / Time", "Room", "Capacity", "Students"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false; // nothing editable in this view
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 5 || columnIndex == 6) return Integer.class;
                return String.class;
            }
        };

        table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setGridColor(new Color(230, 230, 230));
        table.setAutoCreateRowSorter(true);

        // make table non-focusable when maintenance on (toggle in load)
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        tableCard.add(sp, BorderLayout.CENTER);

        contentPanel.add(tableCard);
        contentPanel.add(Box.createVerticalStrut(30));

        // === Initial placeholder row ===
        model.addRow(new Object[]{"—", "No data loaded", "—", "-", "-", 0, 0});
    }

    /** Load semester dropdown options from DB asynchronously (robust & tolerant) */
    private void loadSemesterOptionsAsync() {
        new SwingWorker<List<String>, Void>() {
            Exception error = null;

            @Override
            protected List<String> doInBackground() {
                List<String> terms = new ArrayList<>();
                String sql = "SELECT DISTINCT semester, year FROM sections WHERE semester IS NOT NULL AND semester <> '' " +
                        "ORDER BY year DESC, semester DESC";
                try (Connection conn = DBConnection.getErpConnection();
                     PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String sem = rs.getString("semester");
                        int yr = rs.getInt("year");
                        if (sem == null) sem = "";
                        String term = (sem.trim().isEmpty() ? String.valueOf(yr) : (sem.trim() + " " + yr));
                        if (!term.trim().isEmpty() && !terms.contains(term)) terms.add(term);
                    }
                } catch (Exception ex) {
                    error = ex;
                }
                return terms;
            }

            @Override
            protected void done() {
                try {
                    List<String> terms = get();
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    model.addElement("All");
                    if (terms != null && !terms.isEmpty()) {
                        for (String t : terms) model.addElement(t);
                    }
                    semesterFilter.setModel(model);

                } catch (Exception ex) {
                    // keep "All" only if DB failed
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    model.addElement("All");
                    semesterFilter.setModel(model);
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    /** Applies search and semester filters */
    private void applyFilters() {
        final String searchText = searchField.getText().trim().toLowerCase();
        final String semesterSelected = (String) semesterFilter.getSelectedItem();

        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
        }

        final TableRowSorter<TableModel> finalSorter = sorter;
        sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                // search against course code (col 0) and title (col 1)
                String code = entry.getStringValue(0);
                String title = entry.getStringValue(1);
                String sem = entry.getStringValue(2);

                boolean matchesSearch = searchText.isEmpty() ||
                        (code != null && code.toLowerCase().contains(searchText)) ||
                        (title != null && title.toLowerCase().contains(searchText));

                boolean matchesSemester = semesterSelected == null || semesterSelected.equals("All") || (sem != null && sem.equals(semesterSelected));
                return matchesSearch && matchesSemester;
            }
        });
    }

    // ------------------ Public API ------------------

    /**
     * Load sections for a given instructor and optional term (e.g. "Fall 2025").
     * This runs the DB query in background and populates the table when done.
     */
    public void loadForInstructor(long instructorId, String term) {
        this.currentInstructorId = instructorId;
        this.currentTerm = term;
        loadFromDbAsync();
    }

    /** Simple refresh of last loaded instructor/term */
    public void refresh() {
        loadFromDbAsync();
    }

    // ------------------ DB access (background) ------------------

    private void loadFromDbAsync() {
        // show a quick loading row
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            model.addRow(new Object[]{"—", "Loading...", "—", "-", "-", 0, 0});
            maintenanceBanner.setVisible(false);
        });

        new SwingWorker<List<SectionRow>, Void>() {
            private Exception error = null;

            @Override
            protected List<SectionRow> doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    SectionDaoImpl dao = new SectionDaoImpl(conn);
                    // check maintenance
                    maintenanceOn = dao.isMaintenanceOn();
                    return dao.getSectionsByInstructor(currentInstructorId, currentTerm);
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    model.setRowCount(0);
                    model.addRow(new Object[]{"—", "Failed to load: " + error.getMessage(), "—", "-", "-", 0, 0});
                    error.printStackTrace();
                    // show maintenance banner if DAO says so (best effort)
                    maintenanceBanner.setVisible(maintenanceOn);
                    toggleReadOnly(maintenanceOn);
                    return;
                }
                try {
                    List<SectionRow> rows = get();
                    model.setRowCount(0);

                    // If combo only has "All", build term list from loaded rows and populate combo
                    boolean comboHasOnlyAll = (semesterFilter.getItemCount() <= 1);
                    List<String> builtTerms = new ArrayList<>();

                    if (rows == null || rows.isEmpty()) {
                        model.addRow(new Object[]{"—", "No sections found", "—", "-", "-", 0, 0});
                    } else {
                        for (SectionRow r : rows) {
                            // build term string exactly as we will show it
                            String termStr = (r.semester == null || r.semester.isBlank()) ? String.valueOf(r.year) : (r.semester + " " + r.year);
                            // collect for combo if needed
                            if (comboHasOnlyAll && !builtTerms.contains(termStr)) builtTerms.add(termStr);

                            String dayTime = r.dayTime == null ? "-" : r.dayTime;
                            String room = r.room == null ? "-" : r.room;
                            int capacity = r.capacity;
                            int seatsLeft = r.seatsLeft;
                            int enrolled = capacity - seatsLeft;
                            if (enrolled < 0) enrolled = 0;

                            model.addRow(new Object[]{
                                    r.courseCode == null ? "-" : r.courseCode,
                                    r.title == null ? "-" : r.title,
                                    termStr,
                                    dayTime,
                                    room,
                                    capacity,
                                    enrolled
                            });
                        }
                    }

                    // if combo had only All, populate with terms found from rows (so filter works)
                    if (comboHasOnlyAll && !builtTerms.isEmpty()) {
                        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
                        comboModel.addElement("All");
                        for (String t : builtTerms) comboModel.addElement(t);
                        semesterFilter.setModel(comboModel);
                    }

                    // show maintenance banner & toggle read-only state
                    maintenanceBanner.setVisible(maintenanceOn);
                    toggleReadOnly(maintenanceOn);

                    // apply filters (keep previous search/semester selection)
                    applyFilters();

                } catch (Exception ex) {
                    model.setRowCount(0);
                    model.addRow(new Object[]{"—", "Failed to load: " + ex.getMessage(), "—", "-", "-", 0, 0});
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    /** Toggles read-only visuals/interaction when maintenance is ON */
    private void toggleReadOnly(boolean readOnly) {
        // show/hide banner already done by caller. Additional effects:
        table.setEnabled(!readOnly);
        searchField.setEnabled(!readOnly);
        semesterFilter.setEnabled(!readOnly);
        // make header banner prominent when readOnly
        maintenanceBanner.setVisible(readOnly);
    }
}
