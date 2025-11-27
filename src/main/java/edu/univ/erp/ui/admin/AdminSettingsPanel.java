package edu.univ.erp.ui.admin;

import edu.univ.erp.service.SettingsService;
import edu.univ.erp.service.SettingsServiceImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * AdminSettingsPanel (service-based)
 * All DB logic moved to SettingsService.
 * UI behavior 100% unchanged.
 */
public class AdminSettingsPanel extends JPanel {

    private final JCheckBox maintenanceCheck = new JCheckBox(
            "Maintenance Mode (show banner to users)"
    );
    private final JButton backupBtn = new JButton("Backup DB");
    private final JButton restoreBtn = new JButton("Restore DB");

    private final Runnable refreshBannerCallback;

    private final SettingsService settingsService;


    /** NEW: Use service instead of DAO/DB code */
    //private final SettingsService settingsService = new SettingsServiceImpl();

   public AdminSettingsPanel(SettingsService settingsService, Runnable refreshBannerCallback) {
        this.settingsService = settingsService;      // injected
        this.refreshBannerCallback = refreshBannerCallback;
        init();
        loadSettings();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 245, 250));

        JLabel titleLabel = new JLabel("System Administration Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        titleLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(titleLabel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);

        // System status
        JPanel maintenancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 10));
        maintenancePanel.setOpaque(false);
        maintenancePanel.setBorder(BorderFactory.createTitledBorder("System Status"));

        maintenanceCheck.setFont(maintenanceCheck.getFont().deriveFont(Font.BOLD, 14f));
        maintenanceCheck.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        maintenancePanel.add(maintenanceCheck);
        mainPanel.add(maintenancePanel);

        // DB controls
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 15));
        dbPanel.setOpaque(false);
        dbPanel.setBorder(BorderFactory.createTitledBorder("Database Control"));

        backupBtn.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        restoreBtn.setIcon(UIManager.getIcon("FileView.hardDriveIcon"));

        setButtonStyle(backupBtn, new Color(33, 150, 243), Color.WHITE);
        setButtonStyle(restoreBtn, new Color(67, 160, 71), Color.WHITE);

        dbPanel.add(backupBtn);
        dbPanel.add(restoreBtn);
        mainPanel.add(dbPanel);

        add(mainPanel, BorderLayout.CENTER);

        // listeners
        maintenanceCheck.addActionListener(this::onToggleMaintenance);
        backupBtn.addActionListener(e -> onBackup());
        restoreBtn.addActionListener(e -> onRestore());
    }

    private void setButtonStyle(JButton btn, Color bg, Color fg) {
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 14f));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
    }

    /**
     * Load settings using SettingsService
     */
    private void loadSettings() {
        boolean on = settingsService.isMaintenanceOn();
        maintenanceCheck.setSelected(on);
    }

    /**
     * Toggle handler using SettingsService
     */
    private void onToggleMaintenance(ActionEvent e) {
        boolean on = maintenanceCheck.isSelected();

        boolean ok = settingsService.setMaintenance(on);
        if (!ok) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to update maintenance flag.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            loadSettings(); // revert
            return;
        }

        if (refreshBannerCallback != null)
            refreshBannerCallback.run();

        String msg = on
                ? "Maintenance Mode is now ON."
                : "Maintenance Mode is now OFF.\nSystem is back to normal.";

        JOptionPane.showMessageDialog(this, msg);
    }

    // ---------- Backup / Restore logic unchanged ----------

    private void onBackup() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose location to save backup");
        fc.setSelectedFile(new File("erp_db_backup.sql"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL Files (*.sql)", "sql"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();

        String dbName = "erp_db";
        String credPath = new File("creds.cnf").getAbsolutePath();
        String cmd = String.format(
                "mysqldump --defaults-extra-file=\"%s\" %s -r \"%s\"",
                credPath, dbName, out.getAbsolutePath()
        );

        runCommandAsync(cmd, "Backup completed: " + out.getAbsolutePath(), "mysqldump");
    }

    private void onRestore() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose SQL backup file");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL Files (*.sql)", "sql"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File in = fc.getSelectedFile();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Restoring will overwrite data. Proceed?",
                "Confirm restore",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        String dbName = "erp_db";
        String credPath = new File("creds.cnf").getAbsolutePath();
        String cmd = String.format(
                "mysql --defaults-extra-file=\"%s\" %s < \"%s\"",
                credPath, dbName, in.getAbsolutePath()
        );

        runCommandAsync(cmd, "Restore executed successfully", "mysql");
    }

    private void runCommandAsync(String command, String successMessage, String requiredExe) {
        new Thread(() -> {
            try {
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

                if (requiredExe != null && !checkExecutable(requiredExe, isWindows)) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(
                                    this,
                                    requiredExe + " not found on PATH.",
                                    "Error", JOptionPane.ERROR_MESSAGE));
                    return;
                }

                String[] shell = isWindows
                        ? new String[]{"cmd.exe", "/c", command}
                        : new String[]{"/bin/sh", "-c", command};

                ProcessBuilder pb = new ProcessBuilder(shell);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                String output = new java.util.Scanner(p.getInputStream())
                        .useDelimiter("\\A").next();
                int rc = p.waitFor();

                SwingUtilities.invokeLater(() -> {
                    if (rc == 0)
                        JOptionPane.showMessageDialog(this, successMessage);
                    else
                        JOptionPane.showMessageDialog(this,
                                "Command failed.\n" + output,
                                "Error", JOptionPane.ERROR_MESSAGE);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private boolean checkExecutable(String exe, boolean win) {
        try {
            Process p = win
                    ? new ProcessBuilder("cmd.exe", "/c", "where " + exe).start()
                    : new ProcessBuilder("/bin/sh", "-c", "which " + exe).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
