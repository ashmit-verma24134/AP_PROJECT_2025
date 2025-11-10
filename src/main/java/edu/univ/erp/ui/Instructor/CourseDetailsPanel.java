package edu.univ.erp.ui.instructor;

import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CourseDetailsPanel extends JPanel {

    private JTable gradeTable, attendanceTable;
    private DefaultTableModel gradeModel, attendanceModel;

    public CourseDetailsPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // === HEADER SECTION ===
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel courseTitle = new JLabel("📘 CS101 - Introduction to Programming (Section A)");
        courseTitle.setFont(Theme.HEADER_FONT);
        courseTitle.setForeground(Color.WHITE);

        JLabel semesterLabel = new JLabel("Monsoon 2025");
        semesterLabel.setFont(Theme.BODY_BOLD);
        semesterLabel.setForeground(Color.WHITE);

        header.add(courseTitle, BorderLayout.WEST);
        header.add(semesterLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // === TAB PANEL ===
        JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
        tabPane.setFont(Theme.BODY_FONT);
        tabPane.setBackground(Theme.SURFACE);

        tabPane.addTab("🧮 Gradebook", createGradebookPanel());
        tabPane.addTab("📅 Attendance", createAttendancePanel());
        tabPane.addTab("📤 Materials", createMaterialsPanel());
        tabPane.addTab("🗣️ Feedback", createFeedbackPanel());

        JScrollPane scrollPane = new JScrollPane(tabPane);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ===================================
    // 🧮 GRADEBOOK TAB
    // ===================================
    private JPanel createGradebookPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        gradeModel = new DefaultTableModel(
                new Object[]{"Student ID", "Name", "Assignment 1", "Assignment 2", "Midterm", "Final", "Grade"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col >= 2; // allow editing for grade columns only
            }
        };

        gradeTable = new JTable(gradeModel);
        gradeTable.setRowHeight(36);
        gradeTable.setFont(Theme.BODY_FONT);
        gradeTable.getTableHeader().setFont(Theme.BODY_BOLD);
        gradeTable.getTableHeader().setBackground(Theme.PRIMARY_DARK);
        gradeTable.getTableHeader().setForeground(Color.WHITE);
        gradeTable.setGridColor(Theme.DIVIDER);
        gradeTable.setShowGrid(true);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        gradeTable.setDefaultRenderer(Object.class, center);

        // Dummy data
        gradeModel.addRow(new Object[]{"S001", "Rahul Kumar", "85", "90", "88", "92", "A"});
        gradeModel.addRow(new Object[]{"S002", "Aisha Verma", "78", "82", "80", "86", "B+"});
        gradeModel.addRow(new Object[]{"S003", "Arjun Mehta", "92", "95", "89", "94", "A+"});

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnPanel.setBackground(Theme.BACKGROUND);
        JButton saveBtn = styledButton("💾 Save");
        JButton exportBtn = styledButton("📤 Export CSV");
        JButton lockBtn = styledButton("🔒 Lock Grades");

        btnPanel.add(saveBtn);
        btnPanel.add(exportBtn);
        btnPanel.add(lockBtn);

        panel.add(new JScrollPane(gradeTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ===================================
    // 📅 ATTENDANCE TAB
    // ===================================
    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        attendanceModel = new DefaultTableModel(
                new Object[]{"Student ID", "Name", "Total Classes", "Attended", "Attendance %"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3; // editable only for "Attended"
            }
        };

        attendanceTable = new JTable(attendanceModel);
        attendanceTable.setRowHeight(36);
        attendanceTable.setFont(Theme.BODY_FONT);
        attendanceTable.getTableHeader().setFont(Theme.BODY_BOLD);
        attendanceTable.getTableHeader().setBackground(Theme.PRIMARY_DARK);
        attendanceTable.getTableHeader().setForeground(Color.WHITE);
        attendanceTable.setGridColor(Theme.DIVIDER);
        attendanceTable.setShowGrid(true);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        attendanceTable.setDefaultRenderer(Object.class, center);

        // Dummy data
        attendanceModel.addRow(new Object[]{"S001", "Rahul Kumar", 30, 28, "93%"});
        attendanceModel.addRow(new Object[]{"S002", "Aisha Verma", 30, 27, "90%"});
        attendanceModel.addRow(new Object[]{"S003", "Arjun Mehta", 30, 25, "83%"});

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnPanel.setBackground(Theme.BACKGROUND);
        JButton saveBtn = styledButton("💾 Save");
        JButton exportBtn = styledButton("📤 Export CSV");

        btnPanel.add(saveBtn);
        btnPanel.add(exportBtn);

        panel.add(new JScrollPane(attendanceTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ===================================
    // 📤 MATERIALS TAB
    // ===================================
    private JPanel createMaterialsPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel info = new JLabel("Upload lecture notes, assignments, and resources for students.");
        info.setFont(Theme.BODY_FONT);

        JButton uploadBtn = styledButton("📎 Upload File");
        JButton viewBtn = styledButton("📂 View Uploaded");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        buttonPanel.setBackground(Theme.BACKGROUND);
        buttonPanel.add(uploadBtn);
        buttonPanel.add(viewBtn);

        JTextArea fileList = new JTextArea();
        fileList.setEditable(false);
        fileList.setText("• Lecture 1 - Intro.pdf\n• Assignment 1.pdf\n• Sample Codes.zip");
        fileList.setFont(Theme.MONO_FONT);
        fileList.setBackground(Theme.SURFACE);
        fileList.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(info, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(new JScrollPane(fileList), BorderLayout.SOUTH);
        return panel;
    }

    // ===================================
    // 🗣️ FEEDBACK TAB
    // ===================================
    private JPanel createFeedbackPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel header = new JLabel("Student Feedback Summary");
        header.setFont(Theme.TITLE_FONT);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea feedbackArea = new JTextArea(
                """
                ⭐ Average Rating: 4.6 / 5
                
                • "The lectures are very clear and easy to follow."
                • "Would appreciate more examples in assignments."
                • "Weekly quizzes help in revision."
                """
        );
        feedbackArea.setEditable(false);
        feedbackArea.setFont(Theme.BODY_FONT);
        feedbackArea.setBackground(Theme.SURFACE);
        feedbackArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        panel.add(header);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JScrollPane(feedbackArea));
        return panel;
    }

    // ===================================
    // STYLED BUTTON HELPER
    // ===================================
    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.BODY_BOLD);
        btn.setBackground(Theme.PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(Theme.PRIMARY_DARK);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(Theme.PRIMARY);
            }
        });
        return btn;
    }
}
