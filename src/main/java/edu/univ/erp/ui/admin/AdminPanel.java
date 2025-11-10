package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class AdminPanel extends JPanel {
    private final JPanel cards = new JPanel(new CardLayout());

    public AdminPanel(MainFrame main) {
        setLayout(new BorderLayout());

        // ===== Top Header (Temporary Navigation Buttons) =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton dashboardBtn = new JButton("Dashboard");
        JButton addStudentBtn = new JButton("Add Student");
        JButton instructorsBtn = new JButton("Instructors");
        JButton studentsBtn = new JButton("Students");
        JButton deptBtn = new JButton("Departments");
        JButton coursesBtn = new JButton("Courses");
        JButton monitoringBtn = new JButton("Monitoring");

        // Assign actions
        dashboardBtn.addActionListener(e -> showCard("Dashboard"));
        addStudentBtn.addActionListener(e -> showCard("AddStudent"));
        instructorsBtn.addActionListener(e -> showCard("Instructors"));
        studentsBtn.addActionListener(e -> showCard("Students"));
        deptBtn.addActionListener(e -> showCard("Departments"));
        coursesBtn.addActionListener(e -> showCard("Courses"));
        monitoringBtn.addActionListener(e -> showCard("Monitoring"));

        // Add to top bar
        top.add(dashboardBtn);
        top.add(addStudentBtn);
        top.add(instructorsBtn);
        top.add(studentsBtn);
        top.add(deptBtn);
        top.add(coursesBtn);
        top.add(monitoringBtn);

        add(top, BorderLayout.NORTH);

        // ===== Create Pages =====
        JPanel dashboard = new AdminDashboardPanel();
        JPanel addStudent = new AddStudentPanel();
        JPanel instructors = new InstructorManagementPanel();
        JPanel students = new StudentOverviewPanel();
        JPanel departments = new DepartmentStatsPanel();
        JPanel courses = new CourseDistributionPanel();
        JPanel monitoring = new SystemMonitoringPanel();

        // ===== Register Panels in Card Layout =====
        cards.add(dashboard, "Dashboard");
        cards.add(addStudent, "AddStudent");
        cards.add(instructors, "Instructors");
        cards.add(students, "Students");
        cards.add(departments, "Departments");
        cards.add(courses, "Courses");
        cards.add(monitoring, "Monitoring");

        add(cards, BorderLayout.CENTER);

        // ===== Logout Section =====
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> main.showCard("login"));
        south.add(logout);
        add(south, BorderLayout.SOUTH);

        // ===== Show Dashboard by Default =====
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, "Dashboard");
    }

    private void showCard(String name) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
    }
}
