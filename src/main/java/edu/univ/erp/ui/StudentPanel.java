package edu.univ.erp.ui;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.service.RegistrationEventBus;
import edu.univ.erp.ui.student.*;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.service.StudentService;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * StudentPanel (refactored) - no direct DB access.
 *
 * Construction now requires SettingsService and StudentService instances
 * (injected by MainFrame or whichever bootstrap creates UI).
 *
 * This version instantiates subpanels via reflection and invokes optional methods
 * only if present. That makes it tolerant to varying constructor signatures on subpanels
 * after refactor (no-arg, (MainFrame), (MainFrame, SettingsService), (MainFrame, SettingsService, StudentService)).
 */
public class StudentPanel extends JPanel {

    private final MainFrame mainFrame;
    private final SettingsService settingsService;
    private final StudentService studentService;

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

            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
            addMouseWheelListener(e -> {});
            addKeyListener(new KeyAdapter() {});
            setFocusTraversalKeysEnabled(false);
            setAlignmentX(0.0f);
            setAlignmentY(0.0f);
        }

        @Override
        public boolean contains(int x, int y) {
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

    // panels (lazily created)
    private JComponent dashboardPanel;
    private JComponent catalogPanel;
    private JComponent timetablePanel;
    private JComponent transcriptPanel;
    private JComponent gradesPanel;
    private JComponent myCoursesPanel;

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

    /**
     * New constructor: Services must be injected (no DB usage here).
     */
    public StudentPanel(MainFrame mainFrame, SettingsService settingsService, StudentService studentService) {
        this.mainFrame = mainFrame;
        this.settingsService = settingsService;
        this.studentService = studentService;

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
        // We add placeholder panels now; real panels are created lazily and replaced when needed.
dashboardPanel = createAndAddPanel(uiClass("DashboardPanel"), "dashboard");
catalogPanel  = createAndAddPanel(uiClass("CatalogPanel"), "catalog");
timetablePanel = createAndAddPanel(uiClass("TimetablePanel"), "timetable");
transcriptPanel = createAndAddPanel(uiClass("TranscriptPanel"), "transcript");
myCoursesPanel = createAndAddPanel(uiClass("MyCoursesPanel"), "mycourses");
gradesPanel    = createAndAddPanel(uiClass("SemesterGradesPanel"), "grades");


        // Prepare overlay wrapper using OverlayLayout
        cardsWrapper.setLayout(new OverlayLayout(cardsWrapper));
        cardsWrapper.setOpaque(false);

        cards.setAlignmentX(0.0f);
        cards.setAlignmentY(0.0f);
        maintenanceBlocker.setAlignmentX(0.0f);
        maintenanceBlocker.setAlignmentY(0.0f);

        cardsWrapper.add(cards);
        cardsWrapper.add(maintenanceBlocker);

        add(cardsWrapper, BorderLayout.CENTER);

        // Register MyCoursesPanel to the event bus (safe call)
        try {
            RegistrationEventBus.get().register(() -> invokeIfExists(myCoursesPanel, "onRegistrationChanged"));
            System.out.println("StudentPanel: registered myCoursesPanel to RegistrationEventBus (lambda)");
        } catch (Throwable t) {
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
            invokeIfExists(myCoursesPanel, "reloadFromDb", (Class<?>[]) null); // attempt reload
            showCard("mycourses");
        });
        btnGrades.addActionListener(e -> { setNavActive(btnGrades); showCard("grades"); });

        // ---------- REGISTRATION LISTENER FOR CATALOG ----------
        // attempt to set registration listener on catalog panel if that API exists
        try {
            Method setReg = catalogPanel.getClass().getMethod("setRegistrationListener", Runnable.class);
            setReg.invoke(catalogPanel, (Runnable) () -> {
                try { invokeIfExists(myCoursesPanel, "onRegistrationChanged"); } catch (Throwable ignore) {}
                try { invokeIfExists(timetablePanel, "reloadForStudent"); } catch (Throwable ignore) {}
                try { invokeIfExists(transcriptPanel, "reloadForStudent"); } catch (Throwable ignore) {}
                try { invokeIfExists(dashboardPanel, "onRegistrationChanged"); } catch (Throwable ignore) {}
            });
        } catch (Throwable ignore) {
            // If method not present, no-op
        }

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

    // Helper to resolve a panel class name to a Class object in edu.univ.erp.ui.student package
    // Returns the Class object if found, otherwise null.
    private static Class<?> uiClass(String simpleName) {
        String base = "edu.univ.erp.ui.student.";
        try {
            return Class.forName(base + simpleName);
        } catch (ClassNotFoundException e) {
            // Not found: maybe class name differs. Return null and the code will place a placeholder.
            return null;
        }
    }

    // create and add panel to cards with given card name. If the real class isn't present or instantiation fails,
    // it creates a placeholder panel that shows an error label.
    private JComponent createAndAddPanel(Class<?> panelClass, String cardName) {
        JComponent panel;
        if (panelClass != null) {
            panel = instantiatePanel(panelClass);
        } else {
            panel = placeholderPanel("Missing: " + cardName + " (class not found)");
        }
        cards.add(wrapInPadding(panel), cardName);
        return panel;
    }

    // instantiatePanel tries several constructor signatures. Falls back to a placeholder on failure.
    @SuppressWarnings("unchecked")
    private JComponent instantiatePanel(Class<?> cls) {
        try {
            // 1) (MainFrame, SettingsService, StudentService)
            try {
                Constructor<?> c = cls.getConstructor(MainFrame.class, SettingsService.class, StudentService.class);
                Object o = c.newInstance(mainFrame, settingsService, studentService);
                if (o instanceof JComponent) return (JComponent) o;
            } catch (Throwable ignored) {}

            // 2) (MainFrame, SettingsService)
            try {
                Constructor<?> c = cls.getConstructor(MainFrame.class, SettingsService.class);
                Object o = c.newInstance(mainFrame, settingsService);
                if (o instanceof JComponent) return (JComponent) o;
            } catch (Throwable ignored) {}

            // 3) (MainFrame)
            try {
                Constructor<?> c = cls.getConstructor(MainFrame.class);
                Object o = c.newInstance(mainFrame);
                if (o instanceof JComponent) return (JComponent) o;
            } catch (Throwable ignored) {}

            // 4) no-arg
            try {
                Constructor<?> c = cls.getConstructor();
                Object o = c.newInstance();
                if (o instanceof JComponent) return (JComponent) o;
            } catch (Throwable ignored) {}

            // Nothing worked
            return placeholderPanel("Unable to instantiate " + cls.getSimpleName());
        } catch (Throwable t) {
            t.printStackTrace();
            return placeholderPanel("Error instantiating " + cls.getSimpleName());
        }
    }

    private JComponent placeholderPanel(String msg) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel("<html><body style='padding:10px;color:#600;'>" + msg + "</body></html>");
        p.add(l, BorderLayout.NORTH);
        p.setBackground(Theme.SURFACE);
        return p;
    }

    // Safely invoke a no-arg method if it exists on the target
    private void invokeIfExists(Object target, String methodName, Class<?>... paramTypes) {
        if (target == null) return;
        try {
            Method m;
            if (paramTypes == null || paramTypes.length == 0) {
                m = target.getClass().getMethod(methodName);
                m.setAccessible(true);
                m.invoke(target);
            } else {
                m = target.getClass().getMethod(methodName, paramTypes);
                m.setAccessible(true);
                // no args invocation here (we only support arg-less calls with paramTypes==null above)
                m.invoke(target);
            }
        } catch (NoSuchMethodException nsme) {
            // ignore - method not present
        } catch (Throwable t) {
            System.err.println("invokeIfExists: failed to invoke " + methodName + " on " + target.getClass().getSimpleName() + ": " + t.getMessage());
            //t.printStackTrace();
        }
    }

    // variant that calls method with single Object param (useful for reloadFromDb(null) style calls)
    private void invokeIfExistsWithArg(Object target, String methodName, Object arg) {
        if (target == null) return;
        try {
            Method m = target.getClass().getMethod(methodName, arg == null ? Object.class : arg.getClass());
            m.setAccessible(true);
            m.invoke(target, arg);
        } catch (NoSuchMethodException nsme) {
            // try method that accepts (String) if arg was null (common pattern)
            try {
                Method m2 = target.getClass().getMethod(methodName, String.class);
                m2.setAccessible(true);
                m2.invoke(target, (Object) null);
            } catch (Throwable ignore) {}
        } catch (Throwable t) {
            System.err.println("invokeIfExistsWithArg: failed to invoke " + methodName + " on " + target.getClass().getSimpleName() + ": " + t.getMessage());
            //t.printStackTrace();
        }
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
        refreshMaintenance();

        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);

        if ("mycourses".equals(name)) {
            System.out.println("StudentPanel: showCard -> reloading myCoursesPanel (safety)");
            // try several common reload method names
            invokeIfExists(myCoursesPanel, "reloadFromDb");
            invokeIfExists(myCoursesPanel, "reload");
            invokeIfExistsWithArg(myCoursesPanel, "reloadFromDb", (Object) null);
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
            // try to set id on child panels if APIs exist
            invokeIfExistsWithArg(dashboardPanel, "setStudentId", studentId);
            invokeIfExistsWithArg(catalogPanel, "setStudentId", studentId);
            invokeIfExistsWithArg(timetablePanel, "setStudentId", studentId);
            invokeIfExistsWithArg(transcriptPanel, "setStudentId", studentId);
            invokeIfExistsWithArg(gradesPanel, "setStudentId", studentId);
            invokeIfExistsWithArg(myCoursesPanel, "setStudentId", studentId);

            invokeIfExists(catalogPanel, "reloadFromDb");
            invokeIfExists(timetablePanel, "reloadForStudent");
            invokeIfExists(transcriptPanel, "reloadForStudent");
            return;
        }

        

        // Use studentService (no direct DB)
        try {
            Map<String, Object> overview = null;
            try {
                if (studentService != null) overview = studentService.getStudentOverview(studentId);
            } catch (Exception e) {
                System.err.println("setStudentId: studentService.getStudentOverview failed: " + e.getMessage());
            }

            if (overview != null) {
                String uname = null, full = null, roll = null;
                if (overview.get("username") != null) uname = String.valueOf(overview.get("username"));
                if (overview.get("full_name") != null) full = String.valueOf(overview.get("full_name"));
                if (overview.get("roll_no") != null) roll = String.valueOf(overview.get("roll_no"));

                if (uname != null && !uname.isEmpty()) studentUsername = uname;
                else if (full != null && !full.isEmpty()) studentUsername = full;
                else if (roll != null && !roll.isEmpty()) studentUsername = roll;
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        final String txt = "Welcome, " + studentUsername;
        SwingUtilities.invokeLater(() -> welcomeLabel.setText(txt));

        // notify other panels (if they implement setStudentId)
        invokeIfExistsWithArg(dashboardPanel, "setStudentId", studentId);
        invokeIfExistsWithArg(catalogPanel, "setStudentId", studentId);
        invokeIfExistsWithArg(timetablePanel, "setStudentId", studentId);
        invokeIfExistsWithArg(transcriptPanel, "setStudentId", studentId);
        invokeIfExistsWithArg(gradesPanel, "setStudentId", studentId);
        invokeIfExistsWithArg(myCoursesPanel, "setStudentId", studentId);

        invokeIfExists(catalogPanel, "reloadFromDb");
        invokeIfExists(timetablePanel, "reloadForStudent");
        invokeIfExists(transcriptPanel, "reloadForStudent");

        try { invokeIfExists(myCoursesPanel, "reloadFromDb"); } catch (Throwable ignore) {}
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
    public void refreshMaintenance() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                boolean maintenance = false;
                try {
                    maintenance = settingsService != null && settingsService.isMaintenanceOn();
                } catch (Exception ex) {
                    System.err.println("refreshMaintenance: settingsService read failed: " + ex.getMessage());
                    ex.printStackTrace();
                    maintenance = false; // safe default
                }
                System.out.println("refreshMaintenance: service value -> maintenance=" + maintenance + " (thread=" + Thread.currentThread().getName() + ")");
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

    private void setMaintenanceState(boolean maintenance) {
        System.out.println("setMaintenanceState called: maintenance=" + maintenance +
                " | bannerVisible(before)=" + maintenanceBanner.isVisible() +
                " | blockerVisible(before)=" + maintenanceBlocker.isVisible() +
                " | thread=" + Thread.currentThread().getName());

        SwingUtilities.invokeLater(() -> {
            try {
                maintenanceBanner.setVisible(maintenance);

                maintenanceBlocker.setVisible(maintenance);
                if (maintenance) {
                    maintenanceBlocker.requestFocusInWindow();
                } else {
                    if (maintenanceBlocker.isFocusOwner()) {
                        this.requestFocusInWindow();
                    }
                }

                invokeIfExistsWithArg(myCoursesPanel, "setMaintenanceMode", maintenance);
                invokeIfExists(myCoursesPanel, "setActionsEnabled");
                invokeIfExistsWithArg(catalogPanel, "setActionsEnabled", !maintenance);
                invokeIfExistsWithArg(timetablePanel, "setActionsEnabled", !maintenance);
                invokeIfExistsWithArg(transcriptPanel, "setActionsEnabled", !maintenance);
                invokeIfExistsWithArg(dashboardPanel, "setActionsEnabled", !maintenance);

                try { if (gradesPanel != null) gradesPanel.setEnabled(!maintenance); } catch (Throwable ignore) {}

                try {
                    if (changePassBtn != null) {
                        changePassBtn.setEnabled(!maintenance);
                        changePassBtn.setToolTipText(maintenance ? "Unavailable during maintenance" : null);
                    }
                } catch (Throwable ignore) {}

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

    public void forceRefresh() {
        System.out.println("forceRefresh: manual trigger");
        refreshMaintenance();
    }

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
