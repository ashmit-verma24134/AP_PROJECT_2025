package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.RoundedPanel;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.ui.AnimatedSidebarButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminPanel – unified design (matching Student and Instructor portals)
 * Uses sidebar + top bar layout with CardLayout for dynamic page switching.
 */
public class AdminPanel extends JPanel {

    private final JPanel cards = new JPanel(new CardLayout());
    private final Map<String, JPanel> pages = new LinkedHashMap<>();

    public AdminPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ===== Top Header Bar =====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(8, 16, 8, 16));

        JLabel title = new JLabel("🧭 IIITD Portal – Admin ERP");
        title.setFont(Theme.HEADER_FONT);
        title.setForeground(Color.WHITE);

        JButton logout = new JButton("Logout");
        logout.setBackground(Theme.PRIMARY_DARK);
        logout.setForeground(Color.WHITE);
        logout.setFocusPainted(false);
        logout.setFont(Theme.BODY_BOLD);
        logout.addActionListener(e -> mainFrame.showCard("login"));

        header.add(title, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ===== Sidebar Navigation =====
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        sidebar.setBorder(new EmptyBorder(24, 12, 24, 12));

        JLabel portalLabel = new JLabel("Admin Portal");
        portalLabel.setFont(Theme.TITLE_FONT);
        portalLabel.setForeground(Color.WHITE);
        portalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        portalLabel.setBorder(new EmptyBorder(0, 0, 16, 0));
        sidebar.add(portalLabel);

        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton addUserBtn = new JButton("Add User");
        addUserBtn.addActionListener(e -> showCard("addUser"));
        sidebar.add(addUserBtn);

        cards.add(new AddUserPanel(), "addUser");


        // ===== Define Navigation Buttons =====
        Map<String, String> navItems = Map.of(
                "Dashboard", "Dashboard",
                "Add Student", "AddStudent",
                "Instructors", "Instructors",
                "Students", "Students",
                "Departments", "Departments",
                "Courses", "Courses",
                "Monitoring", "Monitoring"
        );

        ButtonGroup navGroup = new ButtonGroup();

        for (Map.Entry<String, String> entry : navItems.entrySet()) {
            AnimatedSidebarButton btn = new AnimatedSidebarButton(entry.getKey());
            btn.addActionListener(e -> showCard(entry.getValue()));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(6));
            navGroup.add(btn);
        }

        add(sidebar, BorderLayout.WEST);

        // ===== Main Content (CardLayout) =====
        cards.setOpaque(false);

        pages.put("Dashboard", new AdminDashboardPanel());
        pages.put("AddStudent", new AddStudentPanel());
        pages.put("Instructors", new InstructorManagementPanel());
        pages.put("Students", new StudentOverviewPanel());
        pages.put("Departments", new DepartmentStatsPanel());
        pages.put("Courses", new CourseDistributionPanel());
        pages.put("Monitoring", new SystemMonitoringPanel());

        for (Map.Entry<String, JPanel> entry : pages.entrySet()) {
            cards.add(entry.getValue(), entry.getKey());
        }

        add(cards, BorderLayout.CENTER);

        // ===== Show Dashboard by Default =====
        showCard("Dashboard");
    }

    private void showCard(String name) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
    }
}
