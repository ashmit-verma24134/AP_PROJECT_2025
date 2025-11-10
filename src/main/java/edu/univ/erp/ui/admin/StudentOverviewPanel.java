package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StudentOverviewPanel extends JPanel {

    public StudentOverviewPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("🎓 Student Overview");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Summary Cards
        JPanel summary = new JPanel(new GridLayout(1, 4, 20, 0));
        summary.setBackground(Theme.BACKGROUND);
        summary.setBorder(new EmptyBorder(20, 20, 10, 20));

        summary.add(createCard("Total Students", "1842", "+124 this semester"));
        summary.add(createCard("Enrolled Courses", "4589", ""));
        summary.add(createCard("Graduating Batch", "2025", ""));
        summary.add(createCard("On Leave", "37", ""));

        add(summary, BorderLayout.NORTH);

        // Table
        String[] cols = {"Student ID", "Name", "Program", "Semester", "CGPA"};
        Object[][] data = {
                {"2024001", "John Doe", "B.Tech CSE", 5, 9.1},
                {"2024002", "Priya Verma", "B.Tech ECE", 5, 8.7},
                {"2024003", "Ravi Patel", "B.Tech CSE", 3, 8.4}
        };

        JTable table = new JTable(data, cols);
        table.setFont(Theme.BODY_FONT);
        table.setRowHeight(28);
        table.getTableHeader().setFont(Theme.BODY_BOLD);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(10, 20, 20, 20));

        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createCard(String title, String value, String subtext) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel t = new JLabel(title);
        t.setFont(Theme.BODY_BOLD);
        t.setForeground(Theme.NEUTRAL_MED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        v.setForeground(Theme.NEUTRAL_DARK);
        JLabel s = new JLabel(subtext);
        s.setFont(Theme.BODY_FONT);
        s.setForeground(Theme.SUCCESS);

        JPanel box = new JPanel(new GridLayout(3, 1));
        box.setBackground(Theme.SURFACE);
        box.add(t);
        box.add(v);
        box.add(s);
        card.add(box, BorderLayout.CENTER);
        return card;
    }
}
