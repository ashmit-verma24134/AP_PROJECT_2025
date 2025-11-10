package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class InstructorManagementPanel extends JPanel {

    public InstructorManagementPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("👨‍🏫 Instructor Management");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // Table
        String[] cols = {"Instructor ID", "Name", "Department", "Courses Assigned", "Status"};
        Object[][] data = {
                {"I101", "Dr. Rajesh Kumar", "CSE", 3, "Active"},
                {"I102", "Prof. Anjali Sharma", "ECE", 2, "Active"},
                {"I103", "Dr. Vikram Singh", "Math", 1, "On Leave"},
        };

        JTable table = new JTable(data, cols);
        table.setFont(Theme.BODY_FONT);
        table.setRowHeight(28);
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(10, 20, 20, 20));

        add(scrollPane, BorderLayout.CENTER);

        // Bottom Buttons
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.setBackground(Theme.BACKGROUND);

        JButton addBtn = new JButton("Add Instructor");
        JButton editBtn = new JButton("Edit Details");
        JButton removeBtn = new JButton("Remove");

        for (JButton b : new JButton[]{addBtn, editBtn, removeBtn}) {
            b.setFont(Theme.BODY_BOLD);
            b.setBackground(Theme.PRIMARY);
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
        }

        controls.add(addBtn);
        controls.add(editBtn);
        controls.add(removeBtn);
        add(controls, BorderLayout.SOUTH);
    }
}
