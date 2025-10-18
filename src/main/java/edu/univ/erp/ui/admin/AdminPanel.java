package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class AdminPanel extends JPanel {
    public AdminPanel(MainFrame main) {
        setLayout(new BorderLayout());

        // Title label
        add(new JLabel("Admin Dashboard", SwingConstants.CENTER), BorderLayout.CENTER);

        // Bottom panel (logout button)
        JPanel south = new JPanel();
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> main.showCard("login"));
        south.add(logout);

        add(south, BorderLayout.SOUTH);
    }
}
