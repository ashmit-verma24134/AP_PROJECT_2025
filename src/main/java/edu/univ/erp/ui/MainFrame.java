package edu.univ.erp.ui;

import edu.univ.erp.ui.admin.AdminPanel;
import edu.univ.erp.ui.Instructor.InstructorPanel;
import edu.univ.erp.ui.StudentPanel;

import edu.univ.erp.service.*;      // all services
import edu.univ.erp.data.*;        // DAOs

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    // Panels
    private final LoginPanel loginPanel;
    private final AdminPanel adminPanel;
    private final SignUpPanel signupPanel;
    private final InstructorPanel instructorPanel;
    private final StudentPanel studentPanel;

    // Services
    private final SettingsService settingsService;
    private final AdminService adminService;
    private final CourseService courseService;
    private final SectionService sectionService;
    private final UserService userService;
    private final InstructorService instructorService;
    private final EnrollmentService enrollmentService;
    private final GradeService gradeService;

    public MainFrame() {
        super("IIITD Portal - Uni ERP");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 760);
        setLocationRelativeTo(null);

     

        // ----------------------------------------------------
        //  CREATE SERVICES USING DAO IMPLEMENTATIONS
        // ----------------------------------------------------

        this.settingsService = new SettingsServiceImpl();

        this.adminService = new AdminServiceImpl(new AdminDaoImpl());
        this.courseService = new CourseServiceImpl(new CourseDaoImpl());
        this.sectionService = new SectionServiceImpl(new SectionDaoImpl());
        this.userService = new UserServiceImpl(new UserDaoImpl());
        this.instructorService = new InstructorServiceImpl(new InstructorDaoImpl());
        this.enrollmentService = new EnrollmentServiceImpl(new EnrollmentDaoImpl());
        this.gradeService = new GradeServiceImpl(new GradeDaoImpl());

        // ----------------------------------------------------
        //  CREATE UI PANELS WITH THEIR DEPENDENCIES
        // ----------------------------------------------------

        loginPanel = new LoginPanel(this);

        adminPanel = new AdminPanel(
                this,
                adminService,
                settingsService,
                courseService,
                sectionService,
                userService
        );

        signupPanel = new SignUpPanel(this);

        instructorPanel = new InstructorPanel(
                this,
                instructorService,
                courseService,
                sectionService,
                enrollmentService,
                gradeService,
                settingsService
        );

studentPanel = new StudentPanel(this, settingsService, null);
        // ----------------------------------------------------
        //  ADD PANELS TO CARDS
        // ----------------------------------------------------
        cards.add(loginPanel, "login");
        cards.add(adminPanel, "admin");
        cards.add(signupPanel, "signup");
        cards.add(instructorPanel, "instructor");
        cards.add(studentPanel, "student");

        add(cards);
        showCard("login");
    }

    // ----------------------------------------------------
    //   NAVIGATION HELPERS
    // ----------------------------------------------------

    public void showCard(String key) {
        cardLayout.show(cards, key);
    }

    // Called by LoginPanel
    public void setCurrentStudentId(String studentId) {
        studentPanel.setStudentId(studentId);
        showCard("student");
    }

    public void showStudentDashboard(String studentId) {
        setCurrentStudentId(studentId);
    }

    public void showStudentDashboard(String studentId, String loginUsername) {
        studentPanel.setStudentId(studentId);
        try {
            studentPanel.setStudentUsername(loginUsername);
        } catch (Throwable ignored) {}
        showCard("student");
    }

    // Expose panels
    public InstructorPanel getInstructorPanel() {
        return instructorPanel;
    }

    public StudentPanel getStudentPanel() {
        return studentPanel;
    }

    public AdminPanel getAdminPanel() {
        return adminPanel;
    }

    public void refreshStudentUIs() {
        try {
            studentPanel.forceRefresh();
        } catch (Throwable ignored) {}
    }

    public void setAdminUser(String username) {
        try {
            adminPanel.setAdminUsername(username);
        } catch (Throwable ignored) {}
        showCard("admin");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}
