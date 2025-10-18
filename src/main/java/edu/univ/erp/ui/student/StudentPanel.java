package edu.univ.erp.ui.student;

import edu.univ.erp.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class StudentPanel extends JPanel {
    public StudentPanel(MainFrame main) {
        setLayout(new BorderLayout());
        add(new JLabel("Student Dashboard", SwingConstants.CENTER), BorderLayout.CENTER);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> main.showCard("login"));
        add(logout, BorderLayout.SOUTH);
    }
}
