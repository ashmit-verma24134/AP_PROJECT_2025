package edu.univ.erp.ui;

import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.data.SettingsDaoImpl;
import edu.univ.erp.service.RegistrationEventBus;
import edu.univ.erp.ui.student.*;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * StudentPanel — Central authenticated student UI.
 *
 * Fixed:
 * - use RegistrationEventBus.register(...) (not addListener)
 * - immediate maintenance check on startup
 * - ensure banner show/hide forces layout update
 * - call myCoursesPanel.setActionsEnabled(...) so Drop buttons grey out during maintenance
 */
public class StudentPanel extends JPanel {

    private final MainFrame mainFrame;

    // New banner component (you provided)
    private final MaintenanceBanner maintenanceBanner = new MaintenanceBanner();

    private final JPanel navPanel = new JPanel(null);
    private final JPanel navButtonsContainer = new JPanel();

    private final JPanel cards = new JPanel(new CardLayout());

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

    public StudentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

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

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            stopPolling();
            mainFrame.showCard("login");
        });
        header.add(logout, BorderLayout.EAST);

        maintenanceBanner.setVisible(false);

        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.setOpaque(false);
        topWrap.add(header, BorderLayout.NORTH);
        topWrap.add(maintenanceBanner, BorderLayout.SOUTH);

        add(topWrap, BorderLayout.NORTH);

        // ------------ SIDEBAR ------------
        navPanel.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        navPanel.setBackground(Theme.SIDEBAR_BG);

        navButtonsContainer.setLayout(new BoxLayout(navButtonsContainer, BoxLayout.Y_AXIS));
        navButtonsContainer.setOpaque(false);
        navButtonsContainer.setBounds(0, 16, Theme.SIDEBAR_WIDTH, 600);

        JButton btnDashboard = makeNavButton("Dashboard");
        JButton btnCatalog = makeNavButton("Course Catalog");
        JButton btnTimetable = makeNavButton("My Timetable");
        JButton btnTranscript = makeNavButton("Transcript");
        JButton btnMyCourses = makeNavButton("My Courses");
        JButton btnGrades = makeNavButton("My Grades");

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

        // ------------ CARD CONTENT ------------
        cards.add(wrapInPadding(dashboardPanel), "dashboard");
        cards.add(wrapInPadding(catalogPanel), "catalog");
        cards.add(wrapInPadding(timetablePanel), "timetable");
        cards.add(wrapInPadding(transcriptPanel), "transcript");
        cards.add(wrapInPadding(myCoursesPanel), "mycourses");
        cards.add(wrapInPadding(gradesPanel), "grades");

        add(cards, BorderLayout.CENTER);

        // Register MyCoursesPanel to the event bus (use 'register' method from your DAO)
        try {
// register using a lambda that delegates to myCoursesPanel
RegistrationEventBus.get().register(() -> myCoursesPanel.onRegistrationChanged());
            System.out.println("StudentPanel: registered myCoursesPanel to RegistrationEventBus");
        } catch (Throwable t) {
            // keep going even if event-bus registration fails
            t.printStackTrace();
        }

        // ------------ NAV ACTIONS ------------
        btnDashboard.addActionListener(e -> { setNavActive(btnDashboard); showCard("dashboard"); });
        btnCatalog.addActionListener(e -> { setNavActive(btnCatalog); showCard("catalog"); });
        btnTimetable.addActionListener(e -> { setNavActive(btnTimetable); showCard("timetable"); });
        btnTranscript.addActionListener(e -> { setNavActive(btnTranscript); showCard("transcript"); });
        btnMyCourses.addActionListener(e -> {
            setNavActive(btnMyCourses);
            // ensure freshest data when opening
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
        // initial delay 0 so we check DB immediately at startup
        pollTimer = new javax.swing.Timer(10_000, e -> refreshMaintenance());
        pollTimer.setInitialDelay(0);
        pollTimer.start();
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

        // If studentId is not numeric, skip DB lookup to avoid SQL exceptions
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

    // ================= MAINTENANCE =================
    public void refreshMaintenance() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    SettingsDao s = new SettingsDaoImpl(conn);
                    return s.isMaintenanceOn();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean maintenance = get();
                    System.out.println("[DEBUG] refreshMaintenance() -> maintenance=" + maintenance);

                    // UI changes on EDT
                    SwingUtilities.invokeLater(() -> {
                        try {
                            // show/hide banner
                            maintenanceBanner.setVisible(maintenance);

                            // re-layout header/banner
                            Container top = maintenanceBanner.getParent();
                            if (top instanceof JComponent) {
                                ((JComponent) top).revalidate();
                                ((JComponent) top).repaint();
                            } else {
                                StudentPanel.this.revalidate();
                                StudentPanel.this.repaint();
                            }

                            // update other panels' actions (disable action buttons during maintenance)
                            try { catalogPanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                            try { timetablePanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                            try { transcriptPanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                            try { dashboardPanel.setActionsEnabled(!maintenance); } catch (Throwable ignore) {}
                            try { gradesPanel.setEnabled(!maintenance); } catch (Throwable ignore) {}

                            // THIS ensures drop/register buttons in MyCoursesPanel become greyed out
                            try { myCoursesPanel.setActionsEnabled(!maintenance); } catch (Throwable t) {
                                // If MyCoursesPanel doesn't have setActionsEnabled, print a hint
                                System.err.println("myCoursesPanel.setActionsEnabled(...) failed: " + t.getMessage());
                            }

                            // repaint cards
                            cards.revalidate();
                            cards.repaint();

                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    public void stopPolling() {
        if (pollTimer != null && pollTimer.isRunning()) pollTimer.stop();
    }
}
