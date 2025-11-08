package edu.univ.erp.ui.student;

import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GradesPanel extends JPanel {
    private String studentId;
    private DefaultTableModel model;
    private JLabel lblSemesterGPA, lblCumulativeGPA;
    private JComboBox<String> semesterSelector;
    private JPanel chartContainer;
    private ChartPanel chartPanel;

    public GradesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BACKGROUND);

        // ---------- Header ----------
        JLabel header = new JLabel("📊 My Grades");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setForeground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        add(header, BorderLayout.NORTH);

        // ---------- Top Info Section ----------
        JPanel topPanel = new JPanel(new GridLayout(1, 3, 20, 10));
        topPanel.setBackground(Theme.BACKGROUND);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        lblSemesterGPA = new JLabel("Semester GPA: -");
        lblCumulativeGPA = new JLabel("Cumulative GPA: -");

        for (JLabel lbl : new JLabel[]{lblSemesterGPA, lblCumulativeGPA}) {
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lbl.setForeground(Theme.PRIMARY);
        }

        semesterSelector = new JComboBox<>();
        semesterSelector.addItem("Overall");
        semesterSelector.addActionListener(e -> updateChartForSelectedSemester());

        JLabel lblSelect = new JLabel("Select Semester:");
        lblSelect.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel semPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        semPanel.setBackground(Theme.BACKGROUND);
        semPanel.add(lblSelect);
        semPanel.add(semesterSelector);

        topPanel.add(lblSemesterGPA);
        topPanel.add(lblCumulativeGPA);
        topPanel.add(semPanel);

        add(topPanel, BorderLayout.NORTH);

        // ---------- Table ----------
        String[] cols = {"Course Code", "Course Name", "Instructor", "Credits", "Semester", "Year", "Grade"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.getTableHeader().setBackground(Theme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setGridColor(new Color(230, 230, 230));

        RoundedPanel tablePanel = new RoundedPanel(20);
        tablePanel.setBackground(Theme.CARD_BG);
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---------- Chart ----------
        chartContainer = new RoundedPanel(20);
        chartContainer.setBackground(Theme.CARD_BG);
        chartContainer.setLayout(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel chartTitle = new JLabel("Grade Distribution");
        chartTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chartTitle.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        chartContainer.add(chartTitle, BorderLayout.NORTH);
        chartPanel = new ChartPanel(createEmptyChart());
        chartContainer.add(chartPanel, BorderLayout.CENTER);

        // Split layout: top = table, bottom = chart
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, chartContainer);
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerSize(5);
        add(splitPane, BorderLayout.CENTER);
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

                    lblSemesterGPA.setText("Semester GPA: " + data.get("semesterGPA"));
                    lblCumulativeGPA.setText("Cumulative GPA: " + data.get("cumulativeGPA"));

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
