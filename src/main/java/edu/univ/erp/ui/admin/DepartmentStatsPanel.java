package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DepartmentStatsPanel extends JPanel {
    public DepartmentStatsPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel title = new JLabel("🏛️ Department Statistics");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(new EmptyBorder(20, 40, 40, 40));

        content.add(createDeptRow("Computer Science & Engineering", 456, 12, 38));
        content.add(createDeptRow("Electronics & Communication", 398, 10, 32));
        content.add(createDeptRow("Mathematics", 245, 8, 24));
        content.add(createDeptRow("Computational Biology", 178, 6, 18));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createDeptRow(String name, int students, int instructors, int courses) {
        JPanel row = new JPanel(new GridLayout(2, 1));
        row.setBackground(Theme.SURFACE);
        row.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel dept = new JLabel("<html><b>" + name + "</b></html>");
        dept.setFont(Theme.BODY_BOLD);
        JLabel stats = new JLabel(students + " students • " + instructors + " instructors • " + courses + " courses");
        stats.setFont(Theme.BODY_FONT);
        stats.setForeground(Theme.NEUTRAL_MED);

        row.add(dept);
        row.add(stats);
        return row;
    }
}
