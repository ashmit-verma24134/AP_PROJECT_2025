package edu.univ.erp.ui.student;

import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.data.SettingsDaoImpl;
import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

/**
 * StudentPanel: left nav + right CardLayout for Catalog / Timetable / Transcript.
 */
public class StudentPanel extends JPanel {
    private final MainFrame mainFrame;

    private final JLabel maintenanceBanner;
    private String studentId;

    // nav & cards
    private final JPanel navPanel;
    private final JPanel cards;
    private final CardLayout cardLayout;

    // child panels
    private final CatalogPanel catalogPanel;
    private final TimetablePanel timetablePanel;
    private final TranscriptPanel transcriptPanel;

    // polling timer
    private final javax.swing.Timer pollTimer;

    public StudentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());

        // TOP header
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel title = new JLabel("Student Portal");
        title.setFont(title.getFont().deriveFont(16f));
        header.add(title, BorderLayout.WEST);
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.showCard("login"));
        header.add(btnLogout, BorderLayout.EAST);

        maintenanceBanner = new JLabel("Maintenance Mode Active — View Only");
        maintenanceBanner.setOpaque(true);
        maintenanceBanner.setBackground(new Color(255, 230, 100));
        maintenanceBanner.setHorizontalAlignment(SwingConstants.CENTER);
        maintenanceBanner.setVisible(false);

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.add(header, BorderLayout.NORTH);
        topWrapper.add(maintenanceBanner, BorderLayout.SOUTH);
        add(topWrapper, BorderLayout.NORTH);

        // left nav
        navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        navPanel.setPreferredSize(new Dimension(200, 0));

        // nav buttons
        JButton btnCatalog = makeNavButton("Course Catalog");
        JButton btnTimetable = makeNavButton("My Timetable");
        JButton btnTranscript = makeNavButton("Transcript / Download CSV");

        navPanel.add(btnCatalog);
        navPanel.add(Box.createVerticalStrut(6));
        navPanel.add(btnTimetable);
        navPanel.add(Box.createVerticalStrut(6));
        navPanel.add(btnTranscript);
        navPanel.add(Box.createVerticalGlue());

        // right cards
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        catalogPanel = new CatalogPanel();
        timetablePanel = new TimetablePanel();
        transcriptPanel = new TranscriptPanel();

        // register listener so other panels refresh when register happens
catalogPanel.setRegistrationListener(() -> {
    // these run on the EDT (done() already runs on EDT), but to be safe:
    SwingUtilities.invokeLater(() -> {
        timetablePanel.reloadForStudent();
        transcriptPanel.reloadForStudent();
    });
});

        cards.add(catalogPanel, "catalog");
        cards.add(timetablePanel, "timetable");
        cards.add(transcriptPanel, "transcript");

        // split layout: nav left, cards right
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navPanel, cards);
        split.setDividerLocation(220);
        split.setResizeWeight(0);
        add(split, BorderLayout.CENTER);

        // nav actions
        btnCatalog.addActionListener(e -> showCard("catalog"));
        btnTimetable.addActionListener(e -> showCard("timetable"));
        btnTranscript.addActionListener(e -> showCard("transcript"));

        // poll maintenance flag every 10s
        pollTimer = new javax.swing.Timer(10_000, e -> refreshMaintenance());
        pollTimer.setRepeats(true);
        pollTimer.start();

        // initial
        showCard("catalog");
    }

    private JButton makeNavButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return b;
    }

    public void showCard(String key) {
        cardLayout.show(cards, key);
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
        // propagate into child panels and trigger loads
        catalogPanel.setStudentId(studentId);
        catalogPanel.reloadFromDb(null);

        timetablePanel.setStudentId(studentId);
        timetablePanel.reloadForStudent();

        transcriptPanel.setStudentId(studentId);
        transcriptPanel.reloadForStudent();

        refreshMaintenance();
    }

    public String getStudentId() { return studentId; }

    public void refreshMaintenance() {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    SettingsDao s = new SettingsDaoImpl(conn);
                    return s.isMaintenanceOn();
                } catch (Exception ex) {
                    System.err.println("[StudentPanel] refreshMaintenance: " + ex.getMessage());
                    return false;
                }
            }
            @Override protected void done() {
                try {
                    boolean maintenance = get();
                    maintenanceBanner.setVisible(maintenance);

                    // disable actions on all child panels
                    catalogPanel.setActionsEnabled(!maintenance);
                    timetablePanel.setActionsEnabled(!maintenance);
                    transcriptPanel.setActionsEnabled(!maintenance);
                } catch (Exception ignore) {}
            }
        }.execute();
    }

    public void stopPolling() { if (pollTimer != null && pollTimer.isRunning()) pollTimer.stop(); }
}
