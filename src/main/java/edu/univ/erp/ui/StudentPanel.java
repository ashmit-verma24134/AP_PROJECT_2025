package edu.univ.erp.ui;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.service.RegistrationEventBus;
import edu.univ.erp.ui.student.*;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Integrated StudentPanel
 *
 * Combines two variants:
 *  - stacked header + MaintenanceBanner (banner under header)
 *  - transparent overlay blocker that consumes input during maintenance
 *
 * Behavior:
 *  - Polls DB settings.key='maintenance_on' every 10s (immediate first check)
 *  - setMaintenanceState(boolean) toggles banner/blocker and updates UI
 *  - Registers MyCoursesPanel to RegistrationEventBus using register(...)
 *  - Provides setStudentId / setStudentUsername / setAuthUsername / change password UI
 */
public class StudentPanel extends JPanel {

    private final MainFrame mainFrame;

    // Banner shown under the header
    private final MaintenanceBanner maintenanceBanner = new MaintenanceBanner();

    private final JPanel navPanel = new JPanel(null);
    private final JPanel navButtonsContainer = new JPanel();

    // cards + wrapper for overlay
    private final JPanel cards = new JPanel(new CardLayout());
    private final JPanel cardsWrapper = new JPanel(); // OverlayLayout wrapper

    // Transparent blocker placed on top of cardsWrapper to consume input when maintenance is ON
    private final JComponent maintenanceBlocker = new JComponent() {
        {
            setOpaque(false);
            setVisible(false);
            setFocusable(true);

            // Consume mouse events
            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
            addMouseWheelListener(e -> {});
            // Consume keyboard events when focused
            addKeyListener(new KeyAdapter() {});

            // ensure it can receive focus
            setFocusTraversalKeysEnabled(false);

            // fill available space in overlay layout
            setAlignmentX(0.0f);
            setAlignmentY(0.0f);
        }

        @Override
        public boolean contains(int x, int y) {
            // When visible, consume all mouse interactions
            return isVisible();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isVisible()) {
                String msg = "Site is in Maintenance Mode — view-only";
                Font f = g.getFont().deriveFont(Font.BOLD, 12f);
                g.setFont(f);
                FontMetrics fm = g.getFontMetrics(f);
                int w = fm.stringWidth(msg);
                int x = (getWidth() - w) / 2;
                int y = Math.max(16, fm.getHeight());
                g.setColor(new Color(120, 30, 20, 200));
                g.drawString(msg, Math.max(0, x), y);
            }
        }
    };

    // panels
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final CatalogPanel catalogPanel = new CatalogPanel();
    private final TimetablePanel timetablePanel = new TimetablePanel();
    private final TranscriptPanel transcriptPanel = new TranscriptPanel();
    private final SemesterGradesPanel gradesPanel = new SemesterGradesPanel();
    private final MyCoursesPanel myCoursesPanel = new MyCoursesPanel();

    private JLabel welcomeLabel;
    private String studentUsername = "Student";
    private String studentId;

    private javax.swing.Timer pollTimer;

    // nav buttons kept as fields so we can enable/disable easily if needed
    private JButton btnDashboard, btnCatalog, btnTimetable, btnTranscript, btnMyCourses, btnGrades;

    // Change-password button is now a field so we can toggle it during maintenance
    private JButton changePassBtn;

    // Auth username for change-password
    private String studentAuthUsername;

    public StudentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ---- TOP STACK: header then banner (banner visible under header) ----
        maintenanceBanner.setVisible(false); // hidden initially

        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setOpaque(false);

        // ------------ HEADER ------------
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setOpaque(false);

        JLabel title = new JLabel("Student Portal");
        title.setFont(Theme.TITLE_FONT);

        welcomeLabel = new JLabel("Welcome, " + studentUsername);
        welcomeLabel.setFont(Theme.BODY_FONT);

        leftHeader.add(title);
        leftHeader.add(welcomeLabel);
        header.add(leftHeader, BorderLayout.WEST);

        // logout + change password buttons on right
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightButtons.setOpaque(false);

        // assign changePassBtn to field so maintenance can toggle it
        changePassBtn = new JButton("Change Password");
        changePassBtn.setPreferredSize(new Dimension(150, 32));
        changePassBtn.addActionListener(e -> showChangePasswordDialog());

        JButton logout = new JButton("Logout");
        logout.setPreferredSize(new Dimension(100, 32));
        logout.addActionListener(e -> {
            stopPolling();
            mainFrame.showCard("login");
        });

        rightButtons.add(changePassBtn);
        rightButtons.add(logout);
        header.add(rightButtons, BorderLayout.EAST);

        topStack.add(header);
        topStack.add(maintenanceBanner);

        add(topStack, BorderLayout.NORTH);

        // ------------ SIDEBAR ------------
        navPanel.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        navPanel.setBackground(Theme.SIDEBAR_BG);

        navButtonsContainer.setLayout(new BoxLayout(navButtonsContainer, BoxLayout.Y_AXIS));
        navButtonsContainer.setOpaque(false);
        navButtonsContainer.setBounds(0, 16, Theme.SIDEBAR_WIDTH, 600);

        // create nav buttons and keep references
        btnDashboard = makeNavButton("Dashboard");
        btnCatalog = makeNavButton("Course Catalog");
        btnTimetable = makeNavButton("My Timetable");
        btnTranscript = makeNavButton("Transcript");
        btnMyCourses = makeNavButton("My Courses");
        btnGrades = makeNavButton("My Grades");

        navButtonsContainer.add(Box.createVerticalStrut(12));
        navButtonsContainer.add(btnDashboard);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnCatalog);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnTimetable);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnTranscript);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnMyCourses);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnGrades);
        navButtonsContainer.add(Box.createVerticalGlue());

        navPanel.add(navButtonsContainer);
        add(navPanel, BorderLayout.WEST);

        // ------------ CARD CONTENT (wrapped for overlay) ------------
        cards.add(wrapInPadding(dashboardPanel), "dashboard");
        cards.add(wrapInPadding(catalogPanel), "catalog");
        cards.add(wrapInPadding(timetablePanel), "timetable");
        cards.add(wrapInPadding(transcriptPanel), "transcript");
        cards.add(wrapInPadding(myCoursesPanel), "mycourses");
        cards.add(wrapInPadding(gradesPanel), "grades");

        // Prepare overlay wrapper using OverlayLayout
        cardsWrapper.setLayout(new OverlayLayout(cardsWrapper));
        cardsWrapper.setOpaque(false);

        // Ensure cards & blocker alignment
        cards.setAlignmentX(0.0f);
        cards.setAlignmentY(0.0f);
        maintenanceBlocker.setAlignmentX(0.0f);
        maintenanceBlocker.setAlignmentY(0.0f);

        // add in order: cards first, blocker last so blocker is on top (OverlayLayout paints last added on top)
        cardsWrapper.add(cards);
        cardsWrapper.add(maintenanceBlocker);

        add(cardsWrapper, BorderLayout.CENTER);

        // Register MyCoursesPanel to the event bus (safe call)
        try {
            RegistrationEventBus.get().register(() -> myCoursesPanel.onRegistrationChanged());
            System.out.println("StudentPanel: registered myCoursesPanel to RegistrationEventBus (register lambda)");
        } catch (Throwable t) {
            // fallback: try older API if available
            try {
                RegistrationEventBus.get().addListener(myCoursesPanel);
                System.out.println("StudentPanel: registered myCoursesPanel to RegistrationEventBus (addListener)");
            } catch (Throwable tt) {
                System.err.println("StudentPanel: failed to register myCoursesPanel to event bus: " + tt.getMessage());
                tt.printStackTrace();
            }
        }

        // ------------ NAV ACTIONS ------------
        btnDashboard.addActionListener(e -> { setNavActive(btnDashboard); showCard("dashboard"); });
        btnCatalog.addActionListener(e -> { setNavActive(btnCatalog); showCard("catalog"); });
        btnTimetable.addActionListener(e -> { setNavActive(btnTimetable); showCard("timetable"); });
        btnTranscript.addActionListener(e -> { setNavActive(btnTranscript); showCard("transcript"); });
        btnMyCourses.addActionListener(e -> {
            setNavActive(btnMyCourses);
            System.out.println("StudentPanel: My Courses button clicked -> reloading myCoursesPanel");
            try { myCoursesPanel.reloadFromDb(null); } catch (Throwable ignore) {}
            showCard("mycourses");
        });
        btnGrades.addActionListener(e -> { setNavActive(btnGrades); showCard("grades"); });

        // ---------- REGISTRATION LISTENER FOR CATALOG ----------
        catalogPanel.setRegistrationListener(() -> {
            try { myCoursesPanel.onRegistrationChanged(); } catch (Throwable ignore) {}
            try { timetablePanel.reloadForStudent(); } catch (Throwable ignore) {}
            try { transcriptPanel.reloadForStudent(); } catch (Throwable ignore) {}
            try { dashboardPanel.onRegistrationChanged(); } catch (Throwable ignore) {}
        });

        SwingUtilities.invokeLater(() -> {
            setNavActive(btnDashboard);
            showCard("dashboard");
        });

        // ------------ MAINTENANCE POLLING ------------
        pollTimer = new javax.swing.Timer(10_000, e -> refreshMaintenance());
        pollTimer.setInitialDelay(0);
        pollTimer.start();

        // immediate sync at construction so UI shows correct state right away
        refreshMaintenance();
    }

    private JButton makeNavButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_ITEM_HEIGHT));
        b.setFont(Theme.BODY_FONT);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setForeground(Color.WHITE);
        b.setBackground(Theme.SIDEBAR_BG);
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        return b;
    }

    private void setNavActive(AbstractButton active) {
        for (Component c : navButtonsContainer.getComponents()) {
            if (c instanceof AbstractButton) {
                c.setBackground(Theme.SIDEBAR_BG);
                ((AbstractButton)c).setForeground(Color.WHITE);
            }
        }
        active.setBackground(Theme.SIDEBAR_ACTIVE);
        active.setForeground(Color.WHITE);
    }

    private void showCard(String name) {
        // ensure maintenance state is fresh when switching views
        refreshMaintenance();

        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);

        // extra safety: reload my courses when visible
        if ("mycourses".equals(name)) {
            System.out.println("StudentPanel: showCard -> reloading myCoursesPanel (safety)");
            try { myCoursesPanel.reloadFromDb(null); } catch (Throwable ignore) {}
        }
    }

    private JComponent wrapInPadding(JComponent c) {
        RoundedPanel p = new RoundedPanel(Theme.BORDER_RADIUS);
        p.setLayout(new BorderLayout());
        p.setBackground(Theme.SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(
                Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING
        ));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    // ================= LOGIN =================
    public void setStudentId(String studentId) {
        this.studentId = studentId;

        long numericId = -1L;
        try {
            numericId = Long.parseLong(studentId);
        } catch (NumberFormatException nfe) {
            final String txt = "Welcome, " + (studentId == null ? "Student" : studentId);
            SwingUtilities.invokeLater(() -> welcomeLabel.setText(txt));
            dashboardPanel.setStudentId(studentId);
            catalogPanel.setStudentId(studentId);
            timetablePanel.setStudentId(studentId);
            transcriptPanel.setStudentId(studentId);
            gradesPanel.setStudentId(studentId);
            myCoursesPanel.setStudentId(studentId);
            catalogPanel.reloadFromDb(null);
            timetablePanel.reloadForStudent();
            transcriptPanel.reloadForStudent();
            return;
        }

        try (Connection conn = DBConnection.getErpConnection()) {
            String q = "SELECT s.full_name, s.roll_no, u.username "
                    + "FROM students s LEFT JOIN auth_db.users u ON s.user_id = u.user_id "
                    + "WHERE s.student_id = ? LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setLong(1, numericId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String uname = rs.getString("username");
                        String full = rs.getString("full_name");
                        String roll = rs.getString("roll_no");

                        if (uname != null && !uname.isEmpty()) studentUsername = uname;
                        else if (full != null && !full.isEmpty()) studentUsername = full;
                        else if (roll != null && !roll.isEmpty()) studentUsername = roll;

                        final String txt = "Welcome, " + studentUsername;
                        SwingUtilities.invokeLater(() -> welcomeLabel.setText(txt));
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // notify other panels
        dashboardPanel.setStudentId(studentId);
        catalogPanel.setStudentId(studentId);
        timetablePanel.setStudentId(studentId);
        transcriptPanel.setStudentId(studentId);
        gradesPanel.setStudentId(studentId);
        myCoursesPanel.setStudentId(studentId);

        catalogPanel.reloadFromDb(null);
        timetablePanel.reloadForStudent();
        transcriptPanel.reloadForStudent();

        // safety: ensure my courses starts with fresh data
        try { myCoursesPanel.reloadFromDb(null); } catch (Throwable ignore) {}
    }

    public void setStudentUsername(String username) {
        this.studentUsername = username;
        updateWelcomeLabel();
    }

    private void updateWelcomeLabel() {
        if (welcomeLabel != null && studentUsername != null) {
            welcomeLabel.setText("Welcome, " + studentUsername);
        }
    }

    public void setAuthUsername(String username) {
        this.studentAuthUsername = username;
    }

    private void showChangePasswordDialog() {

        if (studentAuthUsername == null) {
            JOptionPane.showMessageDialog(this,
                    "Auth user not set. Contact admin.");
            return;
        }

        JPasswordField oldPass = new JPasswordField();
        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();

        Object[] form = {
                "Current Password:", oldPass,
                "New Password:", newPass,
                "Confirm New Password:", confirmPass
        };

        int ok = JOptionPane.showConfirmDialog(
                this, form, "Change Password", JOptionPane.OK_CANCEL_OPTION);

        if (ok != JOptionPane.OK_OPTION) return;

        String oldP = new String(oldPass.getPassword());
        String newP = new String(newPass.getPassword());
        String confP = new String(confirmPass.getPassword());

        if (!newP.equals(confP)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.");
            return;
        }

        boolean success = AuthService.changePassword(studentAuthUsername, oldP, newP);

        if (success)
            JOptionPane.showMessageDialog(this, "Password changed successfully!");
        else
            JOptionPane.showMessageDialog(this, "Old password incorrect.");
    }

    // ================= MAINTENANCE =================
    // Robust refreshMaintenance with direct DB read
    public void refreshMaintenance() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                boolean maintenance = false;
                try (Connection conn = DBConnection.getErpConnection()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT `value` FROM settings WHERE `key` = 'maintenance_on' LIMIT 1")) {
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String v = rs.getString(1);
                                if (v != null) {
                                    v = v.trim().toLowerCase();
                                    maintenance = v.equals("1") || v.equals("true") || v.equals("on") || v.equals("yes");
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("refreshMaintenance: DB read failed: " + ex.getMessage());
                    ex.printStackTrace();
                    maintenance = false; // safe default
                }
                System.out.println("refreshMaintenance: DB value -> maintenance=" + maintenance + " (thread=" + Thread.currentThread().getName() + ")");
                return maintenance;
            }

            @Override
            protected void done() {
                boolean maintenance;
                try {
                    maintenance = get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    maintenance = false;
                }
                System.out.println("refreshMaintenance: calling setMaintenanceState(" + maintenance + ")");
                setMaintenanceState(maintenance);
            }
        }.execute();
    }

    // Hardened setMaintenanceState
    private void setMaintenanceState(boolean maintenance) {
        System.out.println("setMaintenanceState called: maintenance=" + maintenance +
                " | bannerVisible(before)=" + maintenanceBanner.isVisible() +
                " | blockerVisible(before)=" + maintenanceBlocker.isVisible() +
                " | thread=" + Thread.currentThread().getName());

        SwingUtilities.invokeLater(() -> {
            try {
                // 1) ensure banner visibility updated
                maintenanceBanner.setVisible(maintenance);

                // 2) ensure blocker visibility updated
                maintenanceBlocker.setVisible(maintenance);
                if (maintenance) {
                    maintenanceBlocker.requestFocusInWindow();
                } else {
                    if (maintenanceBlocker.isFocusOwner()) {
                        this.requestFocusInWindow();
                    }
                }

                // NOTE: We intentionally DO NOT change nav visuals here.
                // The blocker only covers the content cards (cardsWrapper),
                // so the sidebar navigation remains usable and visually unchanged.

                // 4) ensure panels that have actions know maintenance state
                try { myCoursesPanel.setMaintenanceMode(maintenance); } catch (Throwable ignore) {}
                try { myCoursesPanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                try { catalogPanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                try { timetablePanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                try { transcriptPanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                try { dashboardPanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                try { gradesPanel.setEnabled(!maintenance); } catch (Throwable ignore) {}

                // 4b) ensure header actions respect maintenance (change password becomes view-only)
                try {
                    if (changePassBtn != null) {
                        changePassBtn.setEnabled(!maintenance);
                        changePassBtn.setToolTipText(maintenance ? "Unavailable during maintenance" : null);
                    }
                } catch (Throwable ignore) {}

                // 5) force validate/repaint on affected containers so UI immediately reflects change
                Component parentOfBanner = maintenanceBanner.getParent();
                if (parentOfBanner != null) {
                    parentOfBanner.revalidate();
                    parentOfBanner.repaint();
                }
                if (cardsWrapper != null) {
                    cardsWrapper.revalidate();
                    cardsWrapper.repaint();
                }
                this.revalidate();
                this.repaint();

                System.out.println("setMaintenanceState applied: bannerVisible(after)=" + maintenanceBanner.isVisible() +
                        " | blockerVisible(after)=" + maintenanceBlocker.isVisible());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    // Public helper to force an immediate refresh (call from admin UI after toggling DB)
    public void forceRefresh() {
        System.out.println("forceRefresh: manual trigger");
        refreshMaintenance();
    }

    /**
     * Change the nav appearance while keeping behavior intact.
     * (left here for backwards compatibility; not called by default)
     */
    private void dimNavVisual(boolean maintenance) {
        Color dimBg = new Color(230, 230, 230);
        Color dimFg = new Color(110, 110, 110);
        for (Component c : navButtonsContainer.getComponents()) {
            if (c instanceof AbstractButton) {
                if (maintenance) {
                    c.setBackground(dimBg);
                    ((AbstractButton) c).setForeground(dimFg);
                } else {
                    c.setBackground(Theme.SIDEBAR_BG);
                    ((AbstractButton) c).setForeground(Color.WHITE);
                }
            }
        }
    }

    /**
     * Recursively disable interactive components so UI becomes view-only.
     * Kept as a fallback but not used for visual preservation.
     */
    @SuppressWarnings("unused")
    private void makeReadOnly(Component comp) {
        if (comp == null) return;

        if (comp instanceof AbstractButton) {
            ((AbstractButton) comp).setEnabled(false);
        } else if (comp instanceof JTextComponent) {
            ((JTextComponent) comp).setEditable(false);
            ((JTextComponent) comp).setEnabled(false);
        } else if (comp instanceof JTable) {
            ((JTable) comp).setEnabled(false);
        } else if (comp instanceof JComboBox) {
            ((JComboBox<?>) comp).setEnabled(false);
        } else if (comp instanceof JSpinner) {
            ((JSpinner) comp).setEnabled(false);
        } else if (comp instanceof JSlider) {
            ((JSlider) comp).setEnabled(false);
        } else if (comp instanceof JScrollPane) {
            Component view = ((JScrollPane) comp).getViewport().getView();
            if (view != null) makeReadOnly(view);
        } else {
            comp.setEnabled(false);
        }

        if (comp instanceof Container) {
            for (Component c : ((Container) comp).getComponents()) {
                makeReadOnly(c);
            }
        }
    }

    /**
     * Re-enable components that were disabled by makeReadOnly.
     */
    @SuppressWarnings("unused")
    private void makeReadWrite(Component comp) {
        if (comp == null) return;

        comp.setEnabled(true);

        if (comp instanceof JTextComponent) {
            ((JTextComponent) comp).setEditable(true);
        }

        if (comp instanceof Container) {
            for (Component c : ((Container) comp).getComponents()) {
                makeReadWrite(c);
            }
        }
    }

    public void stopPolling() {
        if (pollTimer != null && pollTimer.isRunning()) pollTimer.stop();
    }
}