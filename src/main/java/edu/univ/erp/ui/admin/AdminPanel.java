package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.AnimatedSidebarButton;
import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.Theme;
import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.data.SettingsDaoImpl;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminPanel — wired to use edu.univ.erp.ui.Theme tokens.
 * - Sidebar (left) + header (top) + CardLayout content area
 * - Pages are registered in registerDefaultPages(); use addPage(...) to extend.
 */
public class AdminPanel extends JPanel {

    private final JPanel cards = new JPanel(new CardLayout());
    private final Map<String, JPanel> pages = new LinkedHashMap<>();
    private final Map<String, AnimatedSidebarButton> navButtons = new LinkedHashMap<>();
    private final MainFrame mainFrame;

    // maintenance banner shown under header
    private final JLabel maintenanceBanner = new JLabel("", SwingConstants.CENTER);

    // UI refs / auth state
    private String adminUsername = "Admin";
    private JLabel welcomeLabel;
    private long currentAdminUserId = -1;

    public AdminPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        initHeader();
        initSidebarAndContent();
        registerDefaultPages();

        // show first page by default
        if (!pages.isEmpty()) {
            String first = pages.keySet().iterator().next();
            showCard(first);
        }

        // initial banner refresh
        refreshMaintenanceBanner();
    }

    // -------------------- Header --------------------
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
        logout.setToolTipText("Logout and return to login (Alt+L)");
        logout.addActionListener(e -> mainFrame.showCard("login"));
        logout.setMnemonic(KeyEvent.VK_L);

        header.add(titlePanel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        right.setOpaque(false);
        right.add(changePassword);
        right.add(logout);
        header.add(right, BorderLayout.EAST);

        // maintenance banner area
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

    // -------------------- Sidebar + Cards --------------------
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

        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

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
        btn.setFocusable(true);
        btn.setToolTipText(labelText);
        btn.setFont(Theme.BODY_FONT);
        btn.addActionListener(e -> {
            if (!pages.containsKey(cardName)) {
                JOptionPane.showMessageDialog(this, "Page not available: " + cardName, "Missing Page", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            showCard(cardName);
            updateNavSelection(cardName);
        });
        return btn;
    }

    private void updateNavSelection(String activeCard) {
        for (Map.Entry<String, AnimatedSidebarButton> e : navButtons.entrySet()) {
            AnimatedSidebarButton b = e.getValue();
            if (e.getKey().equals(activeCard)) {
                b.setSelected(true);
            } else {
                b.setSelected(false);
            }
        }
    }

    // -------------------- Page registration --------------------
    public void addPage(String name, JPanel panel, String navLabel) {
        pages.put(name, panel);
        cards.add(panel, name);
        if (navLabel != null && navButtons.containsKey(navLabel)) {
            AnimatedSidebarButton b = navButtons.get(navLabel);
            b.addActionListener(e -> showCard(name));
        }
    }

    public void addPage(String name, JPanel panel) {
        addPage(name, panel, null);
    }

    private void registerDefaultPages() {
        addPage("Users", new AdminUsersPanel(), "Users");
        addPage("Sections", new AdminCourseSectionPanel(), "Sections");
        addPage("Courses", new AdminCourseSectionPanel(), null);
        addPage("Settings", new AdminSettingsPanel(() -> refreshMaintenanceBanner()), "Settings");
    }

    // -------------------- Navigation API --------------------
    public void showCard(String name) {
        if (!pages.containsKey(name)) {
            System.err.println("Attempted to show unknown card: " + name);
            return;
        }
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
        updateNavSelection(name);
    }

    // -------------------- Maintenance banner --------------------
    public void refreshMaintenanceBanner() {
        boolean on = false;
        try (Connection conn = DBConnection.getErpConnection()) {
            SettingsDao sd = new SettingsDaoImpl(conn);
            on = sd.isMaintenanceOn();
        } catch (SQLException ex) {
            System.err.println("[AdminPanel] Failed to read maintenance flag: " + ex.getMessage());
            on = false;
        }

        if (on) {
            maintenanceBanner.setText("Site is in Maintenance Mode. Some features may be disabled.");
            maintenanceBanner.setVisible(true);
        } else {
            maintenanceBanner.setVisible(false);
        }

        maintenanceBanner.revalidate();
        maintenanceBanner.repaint();
    }

    /**
     * Set the admin username to display
     */
    public void setAdminUsername(String username) {
        this.adminUsername = username == null ? "Admin" : username;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + this.adminUsername);
        }
    }

    /**
     * Set the admin user id (auth DB user_id) and attempt to resolve username for display.
     */
    public void setAdminUserId(long uid) {
        this.currentAdminUserId = uid;
        try {
            String uname = getAuthUsername(uid);
            if (uname != null && !uname.isBlank()) {
                setAdminUsername(uname);
            }
        } catch (Exception ex) {
            // ignore - leave existing username in place
            ex.printStackTrace();
        }
    }

    // -------------------- DB helpers --------------------
    private String getAuthUsername(long userId) {
        try (Connection conn = DBConnection.getAuthConnection()) {
            String q = "SELECT username FROM users WHERE user_id = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("username");
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private long getErpUserIdForAdmin(String adminEmail) {
        try (Connection conn = DBConnection.getErpConnection()) {
            String q = "SELECT id FROM users WHERE email = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setString(1, adminEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    // -------------------- Change password --------------------
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
            JOptionPane.showMessageDialog(this, "New passwords don't match!");
            return;
        }

        try {
            boolean success = AuthService.changePassword(
                    adminUsername,
                    oldP,
                    newP
            );

            if (success)
                JOptionPane.showMessageDialog(this, "Password changed successfully!");
            else
                JOptionPane.showMessageDialog(this, "Old password incorrect.");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to change password: " + ex.getMessage());
        }
    }
}
