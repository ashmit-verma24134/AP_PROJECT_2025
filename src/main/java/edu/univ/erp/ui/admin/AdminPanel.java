package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class AdminPanel extends JPanel {
    private final JPanel cards = new JPanel(new CardLayout());

    public AdminPanel(MainFrame main) {
        setLayout(new BorderLayout());

        // Sidebar or top header (optional)
        JButton addStudentBtn = new JButton("Add Student");
        addStudentBtn.addActionListener(e -> showCard("AddStudent"));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(addStudentBtn);
        add(top, BorderLayout.NORTH);

        // Add panels
        JPanel dashboard = new JPanel(new BorderLayout());
        dashboard.add(new JLabel("Admin Dashboard", SwingConstants.CENTER), BorderLayout.CENTER);

        cards.add(dashboard, "Dashboard");
        cards.add(new AddStudentPanel(), "AddStudent");

        add(cards, BorderLayout.CENTER);

        // Bottom panel (logout)
        JPanel south = new JPanel();
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> main.showCard("login"));
        south.add(logout);
        add(south, BorderLayout.SOUTH);
    }

    private void showCard(String name) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
    }
}
