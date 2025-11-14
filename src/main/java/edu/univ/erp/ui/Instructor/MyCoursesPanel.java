package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MyCoursesPanel — lists all courses taught by instructor.
 * Professional ERP-style design with filters, actions, and consistent theme.
 */
public class MyCoursesPanel extends JPanel {
    private final DefaultTableModel model;
    private final JComboBox<String> semesterFilter;
    private final JTable table;
    private final JTextField searchField;

    public MyCoursesPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // === HEADER ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.PRIMARY);
        headerPanel.setPreferredSize(new Dimension(100, 65));

        JLabel title = new JLabel("📘 My Courses");
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 15));

        headerPanel.add(title, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

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
        semesterFilter = new JComboBox<>(new String[]{"All", "Fall 2025", "Spring 2025", "Monsoon 2024"});
        semesterFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        semesterFilter.addActionListener(e -> applyFilters());

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton btnSearch = new JButton("🔍");
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

        JLabel lblTableTitle = new JLabel("📚 Courses List");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        tableCard.add(lblTableTitle, BorderLayout.NORTH);

        String[] cols = {"Course Code", "Title", "Semester", "Section", "Students", "Actions"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };

        table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setGridColor(new Color(230, 230, 230));
        table.setAutoCreateRowSorter(true);

        // === ACTION BUTTONS (custom renderer/editor) ===
        TableColumn actionColumn = table.getColumn("Actions");
        actionColumn.setCellRenderer(new ActionRenderer());
        actionColumn.setCellEditor(new ActionEditor(new JCheckBox()));

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        tableCard.add(sp, BorderLayout.CENTER);

        contentPanel.add(tableCard);
        contentPanel.add(Box.createVerticalStrut(30));

        // === Load Mock Data ===
        loadMockData();
    }

    /** Applies search and semester filters */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String semesterSelected = (String) semesterFilter.getSelectedItem();

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                String title = entry.getStringValue(1).toLowerCase();
                String sem = entry.getStringValue(2);
                boolean matchesSearch = searchText.isEmpty() || title.contains(searchText);
                boolean matchesSemester = semesterSelected.equals("All") || sem.equals(semesterSelected);
                return matchesSearch && matchesSemester;
            }
        });
    }

    /** Loads mock data to populate the table */
    private void loadMockData() {
        List<Map<String, Object>> courses = List.of(
                Map.of("code", "CS201", "title", "Data Structures", "semester", "Fall 2025", "section", "A", "students", 65),
                Map.of("code", "CS301", "title", "Algorithms", "semester", "Spring 2025", "section", "B", "students", 60),
                Map.of("code", "CS401", "title", "Artificial Intelligence", "semester", "Fall 2025", "section", "A", "students", 55),
                Map.of("code", "CS110", "title", "Intro to Programming", "semester", "Monsoon 2024", "section", "C", "students", 72)
        );

        for (Map<String, Object> c : courses) {
            model.addRow(new Object[]{
                    c.get("code"),
                    c.get("title"),
                    c.get("semester"),
                    c.get("section"),
                    c.get("students"),
                    "Actions"
            });
        }
    }

    /** Renders "View Students" and "Enter Grades" buttons */
    private static class ActionRenderer extends JPanel implements TableCellRenderer {
        private final JButton btnView = new JButton("👩‍🎓 View Students");
        private final JButton btnGrades = new JButton("🧾 Enter Grades");

        public ActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
            setOpaque(true);
            setupButton(btnView, new Color(33, 150, 243));
            setupButton(btnGrades, new Color(76, 175, 80));
        }

        private void setupButton(JButton btn, Color color) {
            btn.setBackground(color);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setBackground(isSelected ? new Color(230, 240, 255) : Color.WHITE);
            return this;
        }
    }

    /** Editor for action buttons */
    private static class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        private final JButton btnView = new JButton("👩‍🎓 View Students");
        private final JButton btnGrades = new JButton("🧾 Enter Grades");

        public ActionEditor(JCheckBox checkBox) {
            panel.setOpaque(true);
            setupButton(btnView, new Color(33, 150, 243));
            setupButton(btnGrades, new Color(76, 175, 80));
            panel.add(btnView);
            panel.add(btnGrades);

            btnView.addActionListener(e -> {
                JOptionPane.showMessageDialog(panel, "Opening Student List...");
                stopCellEditing();
            });

            btnGrades.addActionListener(e -> {
                JOptionPane.showMessageDialog(panel, "Opening Gradebook...");
                stopCellEditing();
            });
        }

        private void setupButton(JButton btn, Color color) {
            btn.setBackground(color);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            panel.setBackground(new Color(245, 245, 245));
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }
}
