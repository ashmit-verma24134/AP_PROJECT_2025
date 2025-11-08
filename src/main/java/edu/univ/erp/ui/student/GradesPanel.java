package edu.univ.erp.ui.student;

import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GradesPanel extends JPanel {
    private String studentId;
    private DefaultTableModel model;
    private JLabel lblSemesterGPA, lblCumulativeGPA;
    private JComboBox<String> semesterSelector;
    private ChartPanel chartPanel;

    public GradesPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // === Header ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.PRIMARY);
        headerPanel.setPreferredSize(new Dimension(100, 60));
        JLabel header = new JLabel("📊 My Grades");
        header.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.add(header, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // === Scrollable content ===
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Theme.BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // === GPA summary row ===
        JPanel summaryRow = new JPanel(new GridLayout(1, 3, 20, 10));
        summaryRow.setBackground(Theme.BACKGROUND);
        summaryRow.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));

        lblSemesterGPA = createSummaryCard("📘 Semester GPA", "-");
        lblCumulativeGPA = createSummaryCard("⭐ Cumulative GPA", "-");

        JPanel semSelectPanel = new RoundedPanel(20);
        semSelectPanel.setBackground(Theme.CARD_BG);
        semSelectPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 15));
        semSelectPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel lblSelect = new JLabel("Select Semester:");
        lblSelect.setFont(new Font("Segoe UI", Font.BOLD, 14));

        semesterSelector = new JComboBox<>();
        semesterSelector.addItem("Overall");
        semesterSelector.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        semesterSelector.addActionListener(e -> updateChartForSelectedSemester());

        semSelectPanel.add(lblSelect);
        semSelectPanel.add(semesterSelector);

        summaryRow.add(lblSemesterGPA);
        summaryRow.add(lblCumulativeGPA);
        summaryRow.add(semSelectPanel);

        contentPanel.add(summaryRow);

        // === Grade Table Card ===
        RoundedPanel tablePanel = new RoundedPanel(25);
        tablePanel.setBackground(Theme.CARD_BG);
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel tableTitle = new JLabel("📚 Grade Details");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        tablePanel.add(tableTitle, BorderLayout.NORTH);

        String[] cols = {"Course Code", "Course Name", "Instructor", "Credits", "Semester", "Year", "Grade"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setGridColor(new Color(230, 230, 230));

        JScrollPane spTable = new JScrollPane(table);
        tablePanel.add(spTable, BorderLayout.CENTER);
        contentPanel.add(tablePanel);

        // === Chart Card ===
        RoundedPanel chartCard = new RoundedPanel(25);
        chartCard.setBackground(Theme.CARD_BG);
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel chartLabel = new JLabel("📈 Grade Distribution");
        chartLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        chartLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        chartPanel = new ChartPanel(createEmptyChart());
        chartPanel.setPreferredSize(new Dimension(700, 300));
        chartPanel.setBackground(Color.WHITE);

        chartCard.add(chartLabel, BorderLayout.NORTH);
        chartCard.add(chartPanel, BorderLayout.CENTER);
        chartCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(chartCard);

        // === Padding at bottom ===
        contentPanel.add(Box.createVerticalStrut(30));
    }

    /** Creates a GPA summary card */
    private JLabel createSummaryCard(String title, String valueText) {
        RoundedPanel card = new RoundedPanel(20);
        card.setBackground(Theme.CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel iconLabel = new JLabel(title.split(" ")[0]);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setForeground(Theme.PRIMARY);

        JLabel titleLabel = new JLabel(title.substring(title.indexOf(" ") + 1));
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(90, 90, 90));

        JLabel valueLabel = new JLabel(valueText);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(Theme.PRIMARY);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        textPanel.setBackground(Theme.CARD_BG);
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);

        card.add(iconLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        return valueLabel;
    }

    /** Called by StudentPanel after login */
    public void setStudentId(String id) {
        this.studentId = id;
        reloadFromDb();
    }

    private void reloadFromDb() {
        model.setRowCount(0);
        lblSemesterGPA.setText("Semester GPA: -");
        lblCumulativeGPA.setText("Cumulative GPA: -");

        if (studentId == null || studentId.isEmpty()) return;

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Thread.sleep(300); // simulate DB delay

                // Mock DB data
                return Map.of(
                        "grades", List.of(
                                Map.of("course_code", "CS201", "course_name", "Data Structures", "instructor", "Dr. Sharma", "credits", 4, "semester", "1", "year", 2024, "grade", "A+"),
                                Map.of("course_code", "EE203", "course_name", "Digital Circuits", "instructor", "Prof. Rao", "credits", 3, "semester", "1", "year", 2024, "grade", "A"),
                                Map.of("course_code", "MA102", "course_name", "Calculus II", "instructor", "Dr. Verma", "credits", 4, "semester", "2", "year", 2025, "grade", "B+"),
                                Map.of("course_code", "PH101", "course_name", "Physics", "instructor", "Dr. Nair", "credits", 3, "semester", "2", "year", 2025, "grade", "A"),
                                Map.of("course_code", "HS201", "course_name", "Professional Ethics", "instructor", "Dr. Das", "credits", 2, "semester", "3", "year", 2025, "grade", "A+")
                        ),
                        "semesterGPA", 8.9,
                        "cumulativeGPA", 9.1
                );
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    List<Map<String, Object>> grades = (List<Map<String, Object>>) data.get("grades");

                    // Fill table
                    for (Map<String, Object> g : grades) {
                        model.addRow(new Object[]{
                                g.get("course_code"),
                                g.get("course_name"),
                                g.get("instructor"),
                                g.get("credits"),
                                g.get("semester"),
                                g.get("year"),
                                g.get("grade")
                        });
                    }

                    lblSemesterGPA.setText("📘 Semester GPA: " + data.get("semesterGPA"));
                    lblCumulativeGPA.setText("⭐ Cumulative GPA: " + data.get("cumulativeGPA"));

                    // Populate semester selector
                    semesterSelector.removeAllItems();
                    semesterSelector.addItem("Overall");
                    grades.stream()
                            .map(g -> g.get("semester").toString())
                            .distinct()
                            .sorted()
                            .forEach(semesterSelector::addItem);

                    // Build overall chart initially
                    updateChart(grades);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(GradesPanel.this,
                            "Error loading grades: " + e.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    /** Creates pie chart dataset and updates chart panel */
    private void updateChart(List<Map<String, Object>> grades) {
        Map<String, Long> gradeCount = grades.stream()
                .collect(Collectors.groupingBy(g -> g.get("grade").toString(), Collectors.counting()));

        DefaultPieDataset dataset = new DefaultPieDataset();
        gradeCount.forEach(dataset::setValue);

        JFreeChart pieChart = ChartFactory.createPieChart(
                "Grade Distribution",
                dataset,
                true, true, false);

        chartPanel.setChart(pieChart);
    }

    /** Called when user changes semester dropdown */
    private void updateChartForSelectedSemester() {
        if (model.getRowCount() == 0) return;

        String selected = (String) semesterSelector.getSelectedItem();
        if (selected == null) return;

        List<Map<String, Object>> grades = extractGradesFromModel();

        if ("Overall".equals(selected)) {
            updateChart(grades);
        } else {
            List<Map<String, Object>> filtered = grades.stream()
                    .filter(g -> g.get("semester").equals(selected))
                    .collect(Collectors.toList());
            updateChart(filtered);
        }
    }

    /** Extracts table data back into a list for filtering */
    private List<Map<String, Object>> extractGradesFromModel() {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            list.add(Map.of(
                    "course_code", model.getValueAt(i, 0),
                    "course_name", model.getValueAt(i, 1),
                    "instructor", model.getValueAt(i, 2),
                    "credits", model.getValueAt(i, 3),
                    "semester", model.getValueAt(i, 4),
                    "year", model.getValueAt(i, 5),
                    "grade", model.getValueAt(i, 6)
            ));
        }
        return list;
    }

    /** Creates an empty placeholder chart */
    private JFreeChart createEmptyChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("No Data", 100);
        return ChartFactory.createPieChart("Grade Distribution", dataset, true, true, false);
    }
}
