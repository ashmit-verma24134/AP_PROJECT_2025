package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class InstructorPanel extends JPanel {
    public InstructorPanel(MainFrame main) {
        setLayout(new BorderLayout());
        add(new JLabel("Instructor Dashboard", SwingConstants.CENTER), BorderLayout.CENTER);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> main.showCard("login"));
        add(logout, BorderLayout.SOUTH);
    }
}
