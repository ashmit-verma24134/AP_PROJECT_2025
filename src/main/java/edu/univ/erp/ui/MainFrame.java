package edu.univ.erp.ui;

import edu.univ.erp.ui.admin.AdminPanel;
import edu.univ.erp.ui.Instructor.InstructorPanel; // adjust package if your instructor package differs
import edu.univ.erp.ui.student.StudentPanel;
import edu.univ.erp.ui.LoginPanel;
import edu.univ.erp.ui.SignUpPanel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel cards = new JPanel(cardLayout);

    // keep references so we can call methods
    private LoginPanel loginPanel;
    private AdminPanel adminPanel;
    private SignUpPanel signupPanel;
    private InstructorPanel instructorPanel;
    private StudentPanel studentPanel;

    public MainFrame() {
        super("IIITD Portal - Uni ERP");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setSize(1100, 760);
        setLocationRelativeTo(null);

        // create panels and assign to fields
        loginPanel = new LoginPanel(this);
        adminPanel = new AdminPanel(this);
        signupPanel = new SignUpPanel(this);
        instructorPanel = new InstructorPanel(this);
        studentPanel = new StudentPanel(this);

        // add cards
        cards.add(loginPanel, "login");
        cards.add(adminPanel, "admin");
        cards.add(signupPanel, "signup");
        cards.add(instructorPanel, "instructor");
        cards.add(studentPanel, "student");

        add(cards);
        showCard("login");
    }

    /**
     * Called from LoginPanel (after successful login) to set current student id and switch to student view.
     */
    public void setCurrentStudentId(String studentId) {
        if (studentPanel != null) {
            studentPanel.setStudentId(studentId);
            showCard("student");
        }
    }

    public void showStudentDashboard(String studentId) {
    setCurrentStudentId(studentId);
    showCard("student");
}


    public void showCard(String key) {
        cardLayout.show(cards, key);
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
