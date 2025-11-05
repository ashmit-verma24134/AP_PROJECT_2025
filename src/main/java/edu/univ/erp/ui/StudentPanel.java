package edu.univ.erp.ui;

import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.data.SettingsDaoImpl;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.ui.student.CatalogPanel;
import edu.univ.erp.ui.student.DashboardPanel;
import edu.univ.erp.ui.student.TimetablePanel;
import edu.univ.erp.ui.student.TranscriptPanel;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

/**
 * StudentPanel (no animations, no slider): left nav + CardLayout right.
 * Minimal, clean sidebar - active button gets a flat background highlight.
 */
public class StudentPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JLabel maintenanceBanner = new JLabel();
    private final StudentService studentService = new StudentService();

    private final JPanel navPanel = new JPanel(null);
    private final JPanel navButtonsContainer = new JPanel();
    private final JPanel cards = new JPanel(new CardLayout());

    // keep your existing panels (imported from edu.univ.erp.ui.student)
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final CatalogPanel catalogPanel = new CatalogPanel();
    private final TimetablePanel timetablePanel = new TimetablePanel();
    private final TranscriptPanel transcriptPanel = new TranscriptPanel();

    private String studentId;
    private javax.swing.Timer pollTimer;

    public StudentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // Top header
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

        // Left nav
        navPanel.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        navPanel.setBackground(Theme.SIDEBAR_BG);

        navButtonsContainer.setLayout(new BoxLayout(navButtonsContainer, BoxLayout.Y_AXIS));
        navButtonsContainer.setOpaque(false);
        navButtonsContainer.setBounds(0, 16, Theme.SIDEBAR_WIDTH, 600);

        // nav buttons - visually consistent
        JButton btnDashboard = makeNavButton("Dashboard");
        JButton btnCatalog = makeNavButton("Course Catalog");
        JButton btnTimetable = makeNavButton("My Timetable");
        JButton btnTranscript = makeNavButton("Transcript");

        navButtonsContainer.add(Box.createVerticalStrut(12));
        navButtonsContainer.add(btnDashboard);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnCatalog);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnTimetable);
        navButtonsContainer.add(Box.createVerticalStrut(8));
        navButtonsContainer.add(btnTranscript);
        navButtonsContainer.add(Box.createVerticalGlue());

        navPanel.add(navButtonsContainer);
        add(navPanel, BorderLayout.WEST);

        // Right cards (CardLayout)
        cards.setBackground(Theme.BACKGROUND);
        // wrap each panel in a RoundedPanel for nicer card surface & padding
        cards.add(wrapInPadding(dashboardPanel), "dashboard");
        cards.add(wrapInPadding(catalogPanel), "catalog");
        cards.add(wrapInPadding(timetablePanel), "timetable");
        cards.add(wrapInPadding(transcriptPanel), "transcript");

        add(cards, BorderLayout.CENTER);

        // listeners: instant movement + show card
        btnDashboard.addActionListener(e -> { setNavActive(btnDashboard); showCard("dashboard"); });
        btnCatalog.addActionListener(e -> { setNavActive(btnCatalog); showCard("catalog"); });
        btnTimetable.addActionListener(e -> { setNavActive(btnTimetable); showCard("timetable"); });
        btnTranscript.addActionListener(e -> { setNavActive(btnTranscript); showCard("transcript"); });

        // mark dashboard active after UI is realized (avoids early-layout issues)
        SwingUtilities.invokeLater(() -> {
            setNavActive(btnDashboard);
            showCard("dashboard");
        });

        // poll maintenance flag
        pollTimer = new javax.swing.Timer(10_000, e -> refreshMaintenance());
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

    /**
     * Simple, flat active style (no slider).
     */
    private void setNavActive(AbstractButton active) {
        // reset all buttons to default sidebar style
        for (Component c : navButtonsContainer.getComponents()) {
            if (c instanceof AbstractButton) {
                c.setBackground(Theme.SIDEBAR_BG);
                ((AbstractButton) c).setForeground(Color.WHITE);
            }
        }

        // highlight the active button with flat background
        active.setBackground(Theme.SIDEBAR_ACTIVE);
        active.setForeground(Color.WHITE);
    }

    private void showCard(String name) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, name);
    }

    /**
     * Wrap a component in a rounded white surface with consistent padding.
     * Ensures every card has the same look/spacing.
     */
    private JComponent wrapInPadding(JComponent c) {
        RoundedPanel p = new RoundedPanel(Theme.BORDER_RADIUS);
        p.setLayout(new BorderLayout());
        p.setBackground(Theme.SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(
                Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
        // propagate
        dashboardPanel.loadData(studentId);
        catalogPanel.setStudentId(studentId);
        timetablePanel.setStudentId(studentId);
        transcriptPanel.setStudentId(studentId);

        catalogPanel.reloadFromDb(null);
        timetablePanel.reloadForStudent();
        transcriptPanel.reloadForStudent();
    }

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
                } catch (Exception ignore) {}
            }
        }.execute();
    }

    public void stopPolling() {
        if (pollTimer != null && pollTimer.isRunning()) pollTimer.stop();
    }
}
