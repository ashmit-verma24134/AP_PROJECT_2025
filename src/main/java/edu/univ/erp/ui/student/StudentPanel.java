package edu.univ.erp.ui.student;

import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.data.SettingsDaoImpl;
import edu.univ.erp.util.DBConnection;
import edu.univ.erp.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

/**
 * StudentPanel — now shows CatalogPanel (course catalog) instead of CoursesCard.
 * Keeps the maintenance banner and a polling timer to refresh maintenance flag.
 */
public class StudentPanel extends JPanel {
    private final MainFrame mainFrame;
private final CatalogPanel catalogPanel;
    private final JLabel maintenanceBanner;  // banner label at top
    private String studentId;

    // polling timer (optional)
    private final javax.swing.Timer pollTimer;

    public StudentPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.catalogPanel = new CatalogPanel();

        setLayout(new BorderLayout());

        // ---------- HEADER ----------
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel title = new JLabel("Student Portal");
        title.setFont(title.getFont().deriveFont(16f));
        header.add(title, BorderLayout.WEST);

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.showCard("login"));
        header.add(btnLogout, BorderLayout.EAST);

        // ---------- MAINTENANCE BANNER ----------
        maintenanceBanner = new JLabel(" Maintenance Mode Active — View Only");
        maintenanceBanner.setOpaque(true);
        maintenanceBanner.setBackground(new Color(255, 230, 100)); // yellow
        maintenanceBanner.setForeground(Color.DARK_GRAY);
        maintenanceBanner.setHorizontalAlignment(SwingConstants.CENTER);
        maintenanceBanner.setVisible(false); // hidden by default

        // ---------- LAYOUT ----------
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.add(header, BorderLayout.NORTH);
        topWrapper.add(maintenanceBanner, BorderLayout.SOUTH);

        add(topWrapper, BorderLayout.NORTH);
        add(catalogPanel, BorderLayout.CENTER);

        // ---------- start polling timer ----------
        int intervalMs = 10_000; // check every 10 seconds
        pollTimer = new javax.swing.Timer(intervalMs, e -> refreshMaintenance());
        pollTimer.setRepeats(true);
        pollTimer.start();
    }

    /**
     * Called by MainFrame (after successful login) to set the student id.
     * Passes id into catalog and triggers data load.
     */
public void setStudentId(String studentId) {
    this.studentId = studentId;
    refreshMaintenance();
    catalogPanel.setStudentId(studentId);
    catalogPanel.reloadMockData();
}


    public String getStudentId() {
        return studentId;
    }

    /**
     * Refresh maintenance flag from DB (runs in background).
     * When maintenance ON we show banner and disable catalog actions.
     */
    public void refreshMaintenance() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DBConnection.getErpConnection()) {
                    SettingsDao settingsDao = new SettingsDaoImpl(conn);
                    return settingsDao.isMaintenanceOn();
                } catch (Exception ex) {
                    System.err.println("[StudentPanel] refreshMaintenance error: " + ex.getMessage());
                    return false; // default OFF on error
                }
            }

            @Override
            protected void done() {
                try {
                    boolean maintenance = get();
                    maintenanceBanner.setVisible(maintenance);
                    catalogPanel.setActionsEnabled(!maintenance);
                } catch (Exception ex) {
                    // ignore - we've logged in background
                }
            }
        }.execute();
    }

    /** Stop polling when panel is discarded. */
    public void stopPolling() {
        if (pollTimer != null && pollTimer.isRunning()) pollTimer.stop();
    }
}
