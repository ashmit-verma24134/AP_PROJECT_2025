package edu.univ.erp.ui.student;

import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class GradesPanel extends JPanel {
    private String studentId;
    private DefaultTableModel model;
    private JLabel lblSemesterGPA, lblCumulativeGPA;
    private JTextField txtSearch;

    public GradesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BACKGROUND);

        // Header
        JLabel header = new JLabel("📊 My Grades");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setForeground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        add(header, BorderLayout.NORTH);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        searchPanel.setBackground(Theme.BACKGROUND);
        txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("Search");
        btnSearch.addActionListener(e -> reloadFromDb(txtSearch.getText().trim()));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        add(searchPanel, BorderLayout.SOUTH);

        // Table setup
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
        add(tablePanel, BorderLayout.CENTER);

        // GPA summary section
        JPanel gpaPanel = new JPanel(new GridLayout(1, 2, 20, 10));
        gpaPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        gpaPanel.setBackground(Theme.BACKGROUND);

        lblSemesterGPA = new JLabel("Semester GPA: -");
        lblCumulativeGPA = new JLabel("Cumulative GPA: -");

        for (JLabel lbl : new JLabel[]{lblSemesterGPA, lblCumulativeGPA}) {
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lbl.setForeground(Theme.PRIMARY);
        }

        gpaPanel.add(lblSemesterGPA);
        gpaPanel.add(lblCumulativeGPA);
        add(gpaPanel, BorderLayout.SOUTH);
    }

    /** Called by StudentPanel after login */
    public void setStudentId(String id) {
        this.studentId = id;
        reloadFromDb(null);
    }

    private void reloadFromDb(String searchQuery) {
        model.setRowCount(0);
        lblSemesterGPA.setText("Semester GPA: -");
        lblCumulativeGPA.setText("Cumulative GPA: -");

        if (studentId == null || studentId.isEmpty()) return;

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                // Replace this with real DAO calls — mock data for now
                Thread.sleep(300);

                return Map.of(
                    "grades", List.of(
                        Map.of("course_code", "CS201", "course_name", "Data Structures", "instructor", "Dr. Sharma",
                               "credits", 4, "semester", "Fall", "year", 2025, "grade", "A"),
                        Map.of("course_code", "EE203", "course_name", "Digital Circuits", "instructor", "Prof. Rao",
                               "credits", 3, "semester", "Fall", "year", 2025, "grade", "B+"),
                        Map.of("course_code", "MA102", "course_name", "Calculus II", "instructor", "Dr. Verma",
                               "credits", 4, "semester", "Spring", "year", 2025, "grade", "A-")
                    ),
                    "semesterGPA", 8.5,
                    "cumulativeGPA", 8.7
                );
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    List<Map<String, Object>> grades = (List<Map<String, Object>>) data.get("grades");

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
}

