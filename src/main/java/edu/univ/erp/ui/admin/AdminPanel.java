package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.AnimatedSidebarButton;
import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.service.UserService;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.service.CourseService;
import edu.univ.erp.service.SectionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminPanel — SERVICE-BASED VERSION
 * -------------------------------------------------------------
 *  No direct DB calls
 *  Uses injected services
 * -------------------------------------------------------------
 */
public class AdminPanel extends JPanel {

    // ------------ Injected Services ------------
    private final AdminService adminService;
    private final CourseService courseService;
    private final SectionService sectionService;
    private final SettingsService settingsService;
    private final UserService userService;

    // ------------ UI ------------
    private final JPanel cards = new JPanel(new CardLayout());
    private final Map<String, JPanel> pages = new LinkedHashMap<>();
    private final Map<String, AnimatedSidebarButton> navButtons = new LinkedHashMap<>();

    private final MainFrame mainFrame;

    private final JLabel maintenanceBanner = new JLabel("", SwingConstants.CENTER);

    private String adminUsername = "Admin";
    private JLabel welcomeLabel;
    private long currentAdminUserId = -1;

    // ------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------
    public AdminPanel(MainFrame mainFrame,
                      AdminService adminService,
                      SettingsService settingsService,
                      CourseService courseService,
                      SectionService sectionService,
                      UserService userService) {
        this.mainFrame = mainFrame;
        this.adminService = adminService;
        this.settingsService = settingsService;
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.userService = userService;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        initTabs();
    }




    // ------------------------------------------------------------
    // Compose header/sidebar/content and pages
    // ------------------------------------------------------------
    private void initTabs() {
        initHeader();
        initSidebarAndContent();
        registerDefaultPages();

        // add cards panel to center (already added in initSidebarAndContent())
        // ensure at least one page shown
        if (!pages.isEmpty()) {
            String first = pages.keySet().iterator().next();
            showCard(first);
            updateNavSelection(first);
        }

        // ensure maintenance UI is synced at startup
        refreshMaintenanceBanner();
    }

    // ------------------------------------------------------------
    // Header
    // ------------------------------------------------------------
    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(8, Theme.PADDING_X, 8, Theme.PADDING_X));
        header.setPreferredSize(new Dimension(0, 56));

        JLabel title = new JLabel("IIITD Portal—Admin ERP");
        title.setFont(Theme.HEADER_FONT);
        title.setForeground(Color.WHITE);

        welcomeLabel = new JLabel("Welcome, " + adminUsername);
        welcomeLabel.setFont(Theme.BODY_FONT);
        welcomeLabel.setForeground(Color.WHITE);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(welcomeLabel);

        JButton changePassword = new JButton("Change Password");
        changePassword.setBackground(Theme.PRIMARY_DARK);
        changePassword.setForeground(Color.WHITE);
        changePassword.setFocusPainted(false);
        changePassword.setFont(Theme.BODY_BOLD);
        changePassword.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        changePassword.addActionListener(e -> showChangePasswordDialog());

        JButton logout = new JButton("Logout");
        logout.setBackground(Theme.PRIMARY_DARK);
        logout.setForeground(Color.WHITE);
        logout.setFocusPainted(false);
        logout.setFont(Theme.BODY_BOLD);
        logout.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        logout.setToolTipText("Logout (Alt+L)");
        logout.addActionListener(e -> mainFrame.showCard("login"));
        logout.setMnemonic(KeyEvent.VK_L);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        right.setOpaque(false);
        right.add(changePassword);
        right.add(logout);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        maintenanceBanner.setOpaque(true);
        maintenanceBanner.setVisible(false);
        maintenanceBanner.setFont(maintenanceBanner.getFont().deriveFont(Font.BOLD, 13f));
        maintenanceBanner.setBackground(new Color(255, 240, 240));
        maintenanceBanner.setForeground(new Color(140, 0, 0));
        maintenanceBanner.setBorder(new EmptyBorder(6, 8, 6, 8));

        JPanel headerContainer = new JPanel(new BorderLayout());
        headerContainer.add(header, BorderLayout.NORTH);
        headerContainer.add(maintenanceBanner, BorderLayout.SOUTH);

        add(headerContainer, BorderLayout.NORTH);
    }

    // ------------------------------------------------------------
    // Sidebar + Cards
    // ------------------------------------------------------------
    private void initSidebarAndContent() {
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

        Map<String, String> navItems = new LinkedHashMap<>();
        navItems.put("Users", "Users");
        navItems.put("Sections", "Sections");
        navItems.put("Settings", "Settings");

        ButtonGroup navGroup = new ButtonGroup();

        for (Map.Entry<String, String> entry : navItems.entrySet()) {
            AnimatedSidebarButton btn = createNavButton(entry.getKey(), entry.getValue());
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_ITEM_HEIGHT));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(8));
            navGroup.add(btn);
            navButtons.put(entry.getValue(), btn);
        }

        sidebar.add(Box.createVerticalGlue());
        add(sidebar, BorderLayout.WEST);

        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(Theme.PADDING_Y, Theme.PADDING_X, Theme.PADDING_Y, Theme.PADDING_X));
        add(cards, BorderLayout.CENTER);
    }

    private AnimatedSidebarButton createNavButton(String labelText, String cardName) {
        AnimatedSidebarButton btn = new AnimatedSidebarButton(labelText);
        btn.setToolTipText(labelText);
        btn.setFont(Theme.BODY_FONT);
        btn.addActionListener(e -> {
            if (!pages.containsKey(cardName)) {
                JOptionPane.showMessageDialog(this, "Page not available: " + cardName);
                return;
            }
            showCard(cardName);
            updateNavSelection(cardName);
        });
        return btn;
    }

    private void updateNavSelection(String activeCard) {
        navButtons.forEach((name, btn) -> btn.setSelected(name.equals(activeCard)));
    }

    // ------------------------------------------------------------
    // Page Registration
    // ------------------------------------------------------------
    private void registerDefaultPages() {
        // AdminUsersPanel requires UserService
        addPage("Users", new AdminUsersPanel(userService), "Users");

        // AdminCourseSectionPanel requires CourseService, SectionService, AdminService
        addPage("Sections", new AdminCourseSectionPanel(courseService, sectionService, adminService), "Sections");

        // AdminSettingsPanel - assumes there's a constructor (SettingsService, Runnable)
        // If your AdminSettingsPanel only has (SettingsService) constructor, replace the call with:
        // addPage("Settings", new AdminSettingsPanel(settingsService), "Settings");
        addPage("Settings", new AdminSettingsPanel(settingsService, this::refreshMaintenanceBanner), "Settings");
    }

    public void addPage(String name, JPanel panel, String navKey) {
        pages.put(name, panel);
        cards.add(panel, name);

        if (navKey != null && navButtons.containsKey(navKey)) {
            // make the sidebar button switch to this page when clicked
            navButtons.get(navKey).addActionListener(e -> showCard(name));
        }
    }

    // ------------------------------------------------------------
    // Navigation API
    // ------------------------------------------------------------
    public void showCard(String name) {
        if (!pages.containsKey(name)) return;
        ((CardLayout) cards.getLayout()).show(cards, name);
        updateNavSelection(name);
    }

    // ------------------------------------------------------------
    // Maintenance Banner (service-based)
    // ------------------------------------------------------------
    public void refreshMaintenanceBanner() {
        boolean maintenance = settingsService.isMaintenanceOn();

        if (maintenance) {
            maintenanceBanner.setText("Site is in Maintenance Mode. Some features may be disabled.");
            maintenanceBanner.setVisible(true);
        } else {
            maintenanceBanner.setVisible(false);
        }

        maintenanceBanner.revalidate();
        maintenanceBanner.repaint();
    }

    // ------------------------------------------------------------
    // Admin User Context
    // ------------------------------------------------------------
    public void setAdminUsername(String username) {
        this.adminUsername = username == null ? "Admin" : username;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + this.adminUsername);
        }
    }

    public void setAdminUserId(long uid) {
        this.currentAdminUserId = uid;

        try {
            String uname = adminService.getAdminUsername(uid);
            if (uname != null && !uname.isBlank()) {
                setAdminUsername(uname);
            }
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------
    // Change Password
    // ------------------------------------------------------------
    private void showChangePasswordDialog() {
        JPasswordField oldPass = new JPasswordField();
        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();

        Object[] form = {
                "Current Password:", oldPass,
                "New Password:", newPass,
                "Confirm New Password:", confirmPass
        };

        int ok = JOptionPane.showConfirmDialog(this, form, "Change Password", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String oldP = new String(oldPass.getPassword());
        String newP = new String(newPass.getPassword());
        String confP = new String(confirmPass.getPassword());

        if (!newP.equals(confP)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match!");
            return;
        }

        boolean success = AuthService.changePassword(adminUsername, oldP, newP);

        JOptionPane.showMessageDialog(this,
                success ? "Password changed successfully!" : "Old password incorrect.");
    }
}