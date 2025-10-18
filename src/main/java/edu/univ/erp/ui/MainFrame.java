package edu.univ.erp.ui;

import edu.univ.erp.ui.admin.AdminPanel;
import edu.univ.erp.ui.Instructor.InstructorPanel;
import edu.univ.erp.ui.student.StudentPanel;
import edu.univ.erp.ui.LoginPanel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel cards = new JPanel(cardLayout);

    public MainFrame() {
        super("IIITD Portal - Uni ERP");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // correct setSize call (no named args)
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // create panels (LoginPanel should be implemented)
        LoginPanel login = new LoginPanel(this);
        AdminPanel admin = new AdminPanel(this);
        InstructorPanel instr = new InstructorPanel(this);
        StudentPanel student = new StudentPanel(this);

        // add cards — use plain String as the card name
        cards.add(login, "login");
        cards.add(admin, "admin");
        cards.add(instr, "instructor");
        cards.add(student, "student");

        add(cards);
        showCard("login");
    }

    public void showCard(String key) {
        cardLayout.show(cards, key);
        // optionally call an onShow() method if panels implement one
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}
