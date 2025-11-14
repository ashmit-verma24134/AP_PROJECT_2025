package edu.univ.erp.ui;

import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.data.SettingsDaoImpl;
import edu.univ.erp.ui.student.*;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

/**
 * StudentPanel — Central authenticated student UI.
 * Sidebar navigation + CardLayout content area.
 */
public class StudentPanel extends JPanel {

    private final MainFrame mainFrame;

    private final JLabel maintenanceBanner = new JLabel();

    // Sidebar navigation containers
    private final JPanel navPanel = new JPanel(null);
    private final JPanel navButtonsContainer = new JPanel();

    // CardLayout container
    private final JPanel cards = new JPanel(new CardLayout());

    // PANELS
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final CatalogPanel catalogPanel = new CatalogPanel();
    private final TimetablePanel timetablePanel = new TimetablePanel();
    private final TranscriptPanel transcriptPanel = new TranscriptPanel();

    private final FinancePanel financePanel = new FinancePanel();
    private final GradesPanel gradesPanel = new GradesPanel();
    private final MyCoursesPanel myCoursesPanel = new MyCoursesPanel();

    private String studentId;
    private javax.swing.Timer pollTimer;

    public StudentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ---------- HEADER ----------
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Student Portal");
        title.setFont(Theme.TITLE_FONT);
        header.add(title, BorderLayout.WEST);

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> mainFrame.showCard("login"));
        header.add(logout, BorderLayout.EAST);

        maintenanceBanner.setOpaque(true);
        maintenanceBanner.setBackground(Theme.WARNING);
        maintenanceBanner.setHorizontalAlignment(SwingConstants.CENTER);
        maintenanceBanner.setVisible(false);

        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.setOpaque(false);
        topWrap.add(header, BorderLayout.NORTH);
        topWrap.add(maintenanceBanner, BorderLayout.SOUTH);
        add(topWrap, BorderLayout.NORTH);

        // ---------- SIDEBAR ----------
        navPanel.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        navPanel.setBackground(Theme.SIDEBAR_BG);

        navButtonsContainer.setLayout(new BoxLayout(navButtonsContainer, BoxLayout.Y_AXIS));
        navButtonsContainer.setOpaque(false);
        navButtonsContainer.setBounds(0, 16, Theme.SIDEBAR_WIDTH, 600);

        JButton btnDashboard   = makeNavButton("Dashboard");
        JButton btnCatalog     = makeNavButton("Course Catalog");
        JButton btnTimetable   = makeNavButton("My Timetable");
        JButton btnTranscript  = makeNavButton("Transcript");
        JButton btnMyCourses   = makeNavButton("My Courses");
        JButton btnGrades      = makeNavButton("My Grades");
        JButton btnFinance     = makeNavButton("My Finances");

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
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnFinance);
        navButtonsContainer.add(Box.createVerticalGlue());

        navPanel.add(navButtonsContainer);
        add(navPanel, BorderLayout.WEST);

        // ---------- CARDLAYOUT AREA ----------
        cards.setBackground(Theme.BACKGROUND);
        cards.add(wrapInPadding(dashboardPanel), "dashboard");
        cards.add(wrapInPadding(catalogPanel), "catalog");
        cards.add(wrapInPadding(timetablePanel), "timetable");
        cards.add(wrapInPadding(transcriptPanel), "transcript");
        cards.add(wrapInPadding(myCoursesPanel), "mycourses");
        cards.add(wrapInPadding(gradesPanel), "grades");
        cards.add(wrapInPadding(financePanel), "finance");

        add(cards, BorderLayout.CENTER);

        // ---------- SIDEBAR BUTTON ACTIONS ----------
        btnDashboard.addActionListener(e -> { setNavActive(btnDashboard); showCard("dashboard"); });
        btnCatalog.addActionListener(e -> { setNavActive(btnCatalog); showCard("catalog"); });
        btnTimetable.addActionListener(e -> { setNavActive(btnTimetable); showCard("timetable"); });
        btnTranscript.addActionListener(e -> { setNavActive(btnTranscript); showCard("transcript"); });
        btnMyCourses.addActionListener(e -> { setNavActive(btnMyCourses); showCard("mycourses"); });
        btnGrades.addActionListener(e -> { setNavActive(btnGrades); showCard("grades"); });
        btnFinance.addActionListener(e -> { setNavActive(btnFinance); showCard("finance"); });

        // ---------- REGISTRATION LISTENER WIRING ----------
        catalogPanel.setRegistrationListener(() -> {

            myCoursesPanel.onRegistrationChanged();
            timetablePanel.reloadForStudent();
            transcriptPanel.reloadForStudent();
            dashboardPanel.onRegistrationChanged();
        });

        // Dashboard starts active
        SwingUtilities.invokeLater(() -> {
            setNavActive(btnDashboard);
            showCard("dashboard");
        });

        // ---------- MAINTENANCE POLLING ----------
        pollTimer = new javax.swing.Timer(10_000, e -> refreshMaintenance());
        pollTimer.start();
    }

    // ===== Helper for Sidebar Button =====
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
                ((AbstractButton) c).setForeground(Color.WHITE);
            }
        }
        active.setBackground(Theme.SIDEBAR_ACTIVE);
        active.setForeground(Color.WHITE);
    }

    private void showCard(String name) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
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

    // ---------- LOGIN PROPAGATION ----------
    public void setStudentId(String studentId) {
        this.studentId = studentId;

        dashboardPanel.setStudentId(studentId);
        catalogPanel.setStudentId(studentId);
        timetablePanel.setStudentId(studentId);
        transcriptPanel.setStudentId(studentId);
        financePanel.setStudentId(studentId);
        gradesPanel.setStudentId(studentId);
        myCoursesPanel.setStudentId(studentId);

        catalogPanel.reloadFromDb(null);
        timetablePanel.reloadForStudent();
        transcriptPanel.reloadForStudent();
    }

    // ---------- Maintenance Banner ----------
    public void refreshMaintenance() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    SettingsDao s = new SettingsDaoImpl(conn);
                    return s.isMaintenanceOn();
                } catch (Exception ex) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean maintenance = get();
                    maintenanceBanner.setVisible(maintenance);

                    catalogPanel.setActionsEnabled(!maintenance);
                    timetablePanel.setActionsEnabled(!maintenance);
                    transcriptPanel.setActionsEnabled(!maintenance);
                    dashboardPanel.setActionsEnabled(!maintenance);
                    gradesPanel.setEnabled(!maintenance);
                    financePanel.setEnabled(!maintenance);
                    myCoursesPanel.setEnabled(!maintenance);
                } catch (Exception ignore) {}
            }
        }.execute();
    }

    public void stopPolling() {
        if (pollTimer != null && pollTimer.isRunning()) pollTimer.stop();
    }
}
