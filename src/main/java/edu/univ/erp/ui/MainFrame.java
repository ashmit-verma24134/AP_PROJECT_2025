package edu.univ.erp.ui;

import edu.univ.erp.ui.admin.AdminPanel;
import edu.univ.erp.ui.Instructor.InstructorPanel;
import javax.swing.*;
import java.awt.*;

/**
 * MainFrame — merged / integrated version
 * - Keeps references to all panels so LoginPanel and others can set context.
 * - Includes both variants of showStudentDashboard (with and without username).
 * - Provides refreshStudentUIs(), setAdminUser(), and getters for panels.
 */
public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    // keep references so we can call methods
    private final LoginPanel loginPanel;
    private final AdminPanel adminPanel;
    private final SignUpPanel signupPanel;
    private final InstructorPanel instructorPanel;
    private final StudentPanel studentPanel;

    public MainFrame() {
        super("IIITD Portal - Uni ERP");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setSize(1100, 760);
        setLocationRelativeTo(null);

        // create panels and assign to fields (pass this frame so panels can callback)
        loginPanel = new LoginPanel(this);
        adminPanel = new AdminPanel(this);
        signupPanel = new SignUpPanel(this);
        instructorPanel = new InstructorPanel(this);
        studentPanel = new StudentPanel(this);

        // add cards
        cards.add(loginPanel, "login");
        cards.add(adminPanel, "admin");
        cards.add(signuppanel(), "signup"); // helper to avoid possible naming collisions
        cards.add(instructorPanel, "instructor");
        cards.add(studentPanel, "student");

        add(cards);
        showCard("login");
    }

    // small helper in case you prefer to keep creation inline; keeps constructor tidy
    private JPanel signuppanel() {
        // SignUpPanel already created as signupPanel field — return it
        return signupPanel;
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

    /**
     * Backward-compatible: show student dashboard without username.
     */
    public void showStudentDashboard(String studentId) {
        setCurrentStudentId(studentId);
        showCard("student");
    }

    /**
     * Preferred: show student dashboard and set the username that authenticated.
     * Ensures StudentPanel receives the login username for any personalized UI/permissions.
     */
    public void showStudentDashboard(String studentId, String loginUsername) {
        if (studentPanel != null) {
            studentPanel.setStudentId(studentId);
            // attempt to set username if method exists on StudentPanel
            try {
                studentPanel.setStudentUsername(loginUsername);
            } catch (NoSuchMethodError | AbstractMethodError ignored) {
                // older StudentPanel might not have setStudentUsername; ignore gracefully
            }
            showCard("student");
        }
    }

    // expose instructor panel so login flow can set context
    public InstructorPanel getInstructorPanel() {
        return instructorPanel;
    }

    // expose student panel so admin or other code can trigger UI refreshes
    public StudentPanel getStudentPanel() {
        return studentPanel;
    }

    public AdminPanel getAdminPanel() {
        return adminPanel;
    }

    /**
     * Convenience helper admin code can call after toggling maintenance in DB
     * to force running StudentPanel instances to refresh immediately.
     */
    public void refreshStudentUIs() {
        if (studentPanel != null) {
            try {
                studentPanel.forceRefresh();
            } catch (NoSuchMethodError | AbstractMethodError ignored) {
                // If StudentPanel doesn't implement forceRefresh(), ignore gracefully.
            }
        }
    }

    public void setAdminUser(String username) {
        if (adminPanel != null) {
            try {
                adminPanel.setAdminUsername(username);
            } catch (NoSuchMethodError | AbstractMethodError ignored) {
                // If older AdminPanel doesn't have setAdminUsername, fallback could be added.
            }
            showCard("admin");
        }
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
