package edu.univ.erp.ui.Instructor;

import edu.univ.erp.data.SectionRow;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * MyCoursesPanel — lists all courses taught by instructor.
 * Now using SectionService and adapted to your SectionRow fields.
 */
public class MyCoursesPanel extends JPanel {

    private final DefaultTableModel model;
    private final JComboBox<String> semesterFilter;
    private final JTable table;
    private final JTextField searchField;

    private long currentInstructorId = 0L;
    private String currentTerm = null;

    private final SectionService sectionService;  // SERVICE ADDED ✔
    private boolean maintenanceOn = false;

    public MyCoursesPanel(SectionService sectionService) {
        this.sectionService = sectionService;

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

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.PRIMARY);
        top.add(headerPanel, BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);

        // === CONTENT ===
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

        semesterFilter = new JComboBox<>(new String[]{"All"});
        semesterFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        semesterFilter.addActionListener(e -> applyFilters());

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    applyFilters();
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

        // === TABLE ===
        RoundedPanel tableCard = new RoundedPanel(25);
        tableCard.setBackground(Theme.CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel lblTableTitle = new JLabel("Courses List");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        tableCard.add(lblTableTitle, BorderLayout.NORTH);

        // Columns (adapted to your SectionRow)
        String[] cols = {
                "Course Code",
                "Title",
                "Semester",
                "Credits",
                "Capacity",
                "Students"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 3 || col == 4 || col == 5) return Integer.class;
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

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        tableCard.add(sp, BorderLayout.CENTER);

        contentPanel.add(tableCard);
        contentPanel.add(Box.createVerticalStrut(30));

        model.addRow(new Object[]{"—", "No data loaded", "—", "—", 0, 0});
    }

    // ----------------- PUBLIC API -----------------

    public void loadForInstructor(long instructorId, String term) {
        this.currentInstructorId = instructorId;
        this.currentTerm = term;
        loadFromService();
    }

    public void refresh() {
        loadFromService();
    }

    // ----------------- LOAD USING SERVICE -----------------

    private void loadFromService() {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            model.addRow(new Object[]{"—", "Loading...", "—", "—", 0, 0});
        });

        new SwingWorker<List<SectionRow>, Void>() {
            Exception error = null;

            @Override
            protected List<SectionRow> doInBackground() {
                try {
                    return sectionService.getSectionsByInstructor(
                            currentInstructorId, currentTerm
                    );
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    model.setRowCount(0);
                    model.addRow(new Object[]{"—", "Load failed: " + error.getMessage(), "—", "—", 0, 0});
                    error.printStackTrace();
                    return;
                }

                try {
                    List<SectionRow> rows = get();
                    model.setRowCount(0);

                    if (rows == null || rows.isEmpty()) {
                        model.addRow(new Object[]{"—", "No sections found", "—", "—", 0, 0});
                        return;
                    }

                    List<String> newTerms = new ArrayList<>();

                    for (SectionRow r : rows) {

                        // semester already stored as single String: "Fall 2025"
                        String termStr = (r.semester == null ? "Unknown" : r.semester);

                        if (!newTerms.contains(termStr)) newTerms.add(termStr);

                        int enrolled = r.capacity - r.seatsLeft;
                        if (enrolled < 0) enrolled = 0;

                        model.addRow(new Object[]{
                                r.code,
                                r.title,
                                termStr,
                                r.credits,
                                r.capacity,
                                enrolled
                        });
                    }

                    // Populate semester filter if it was empty
                    if (semesterFilter.getItemCount() <= 1) {
                        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
                        comboModel.addElement("All");
                        for (String t : newTerms) comboModel.addElement(t);
                        semesterFilter.setModel(comboModel);
                    }

                    applyFilters();

                } catch (Exception ex) {
                    model.setRowCount(0);
                    model.addRow(new Object[]{"—", "Load failed: " + ex.getMessage(), "—", "—", 0, 0});
                }
            }
        }.execute();
    }

    // ----------------- FILTERING -----------------

    private void applyFilters() {
        String search = searchField.getText().trim().toLowerCase();
        String sem = (String) semesterFilter.getSelectedItem();

        TableRowSorter<TableModel> sorter =
                (TableRowSorter<TableModel>) table.getRowSorter();

        if (sorter == null) {
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
        }

        String finalSearch = search;
        String finalSem = sem;

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> e) {
                String code = e.getStringValue(0);
                String title = e.getStringValue(1);
                String semester = e.getStringValue(2);

                boolean matchesSearch =
                        finalSearch.isEmpty() ||
                                (code != null && code.toLowerCase().contains(finalSearch)) ||
                                (title != null && title.toLowerCase().contains(finalSearch));

                boolean matchesSemester =
                        finalSem.equals("All") ||
                                (semester != null && semester.equals(finalSem));

                return matchesSearch && matchesSemester;
            }
        });
    }
}
