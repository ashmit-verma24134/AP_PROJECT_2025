package edu.univ.erp.ui.admin;

import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.data.SettingsDaoImpl;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Professionalized AdminSettingsPanel, all logic identical, improved UI and uses icons.
 */
public class AdminSettingsPanel extends JPanel {
    private final JCheckBox maintenanceCheck = new JCheckBox("Maintenance Mode (show banner to users)");
    private final JButton backupBtn = new JButton("Backup DB");
    private final JButton restoreBtn = new JButton("Restore DB");

    // Parent callback to refresh the banner (can be null)
    private final Runnable refreshBannerCallback;

    public AdminSettingsPanel(Runnable refreshBannerCallback) {
        this.refreshBannerCallback = refreshBannerCallback;
        init();
        loadSettings();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 245, 250));

        // Header
        JLabel titleLabel = new JLabel("System Administration Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        titleLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(titleLabel, BorderLayout.NORTH);

        // Center panel (card style)
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);

        // Maintenance section
        JPanel maintenancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 10));
        maintenancePanel.setOpaque(false);
        maintenancePanel.setBorder(BorderFactory.createTitledBorder("System Status"));

        maintenanceCheck.setFont(maintenanceCheck.getFont().deriveFont(Font.BOLD, 14f));
        maintenanceCheck.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        maintenancePanel.add(maintenanceCheck);

        mainPanel.add(maintenancePanel);

        // Backup/restore buttons with icons
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 15));
        dbPanel.setOpaque(false);
        dbPanel.setBorder(BorderFactory.createTitledBorder("Database Control"));

        // Use standard icons; substitute with your own if available
        backupBtn.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        restoreBtn.setIcon(UIManager.getIcon("FileView.hardDriveIcon"));

        setButtonStyle(backupBtn, new Color(33, 150, 243), Color.WHITE);
        setButtonStyle(restoreBtn, new Color(67, 160, 71), Color.WHITE);

        dbPanel.add(backupBtn);
        dbPanel.add(restoreBtn);

        mainPanel.add(dbPanel);

        add(mainPanel, BorderLayout.CENTER);

        // listeners (logic unchanged)
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
     * Load the maintenance flag from ERP DB and set checkbox.
     * Uses DBConnection.getErpConnection().
     */
    private void loadSettings() {
        try (Connection conn = DBConnection.getErpConnection()) {
            SettingsDao dao = new SettingsDaoImpl(conn);
            boolean on = dao.isMaintenanceOn();
            maintenanceCheck.setSelected(on);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load settings: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            maintenanceCheck.setSelected(false);
        }
    }

    /**
     * Called when admin toggles the checkbox.
     * Writes the new value to ERP DB and triggers banner refresh.
     */
    private void onToggleMaintenance(ActionEvent e) {
        boolean on = maintenanceCheck.isSelected();
        try (Connection conn = DBConnection.getErpConnection()) {
            SettingsDao dao = new SettingsDaoImpl(conn);
            boolean ok = dao.setMaintenance(on);
            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        "No row updated. If the settings row doesn't exist, create it first.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                // revert to DB value
                loadSettings();
                return;
            }
            // Refresh banner in parent
            if (refreshBannerCallback != null) refreshBannerCallback.run();

            String msg = on
                    ? "Maintenance Mode is now ON.\n"
                    : "Maintenance Mode is now OFF.\nSystem is back to normal.";
            JOptionPane.showMessageDialog(this, msg);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to update setting: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            // revert checkbox to DB state
            loadSettings();
        }
    }

    // --- Backup / Restore logic unchanged ---

    private void onBackup() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose location to save backup (sql file)");
        fc.setSelectedFile(new File("erp_db_backup.sql"));

        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "SQL Files (*.sql)", "sql"));
        fc.setAcceptAllFileFilterUsed(true);

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();

        String dbName = "erp_db";
        String credPath = new File("creds.cnf").getAbsolutePath();
        String cmd = String.format("mysqldump --defaults-extra-file=\"%s\" %s -r \"%s\"",
                credPath, dbName, out.getAbsolutePath());
        runCommandAsync(cmd, "Backup completed: " + out.getAbsolutePath(), "mysqldump");
    }

    private void onRestore() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose SQL backup file to restore");

        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "SQL Files (*.sql)", "sql"));
        fc.setAcceptAllFileFilterUsed(true);

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File in = fc.getSelectedFile();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Restoring will overwrite DB data. Are you sure?",
                "Confirm restore", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String dbName = "erp_db";
        String credPath = new File("creds.cnf").getAbsolutePath();
        String cmd = String.format("mysql --defaults-extra-file=\"%s\" %s < \"%s\"",
                credPath, dbName, in.getAbsolutePath());

        runCommandAsync(cmd, "Restore command executed successfully", "mysql");
    }

    /**
     * Cross-platform runner that checks for required executable and runs via shell.
     * requiredExe can be "mysqldump" or "mysql" (or null to skip check).
     */
    private void runCommandAsync(String command, String successMessage, String requiredExe) {
        new Thread(() -> {
            try {
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

                // check for required executable on PATH first to give nicer error
                if (requiredExe != null && !requiredExe.isBlank()) {
                    boolean found = isWindows ? checkCommandOnWindows(requiredExe) : checkCommandOnUnix(requiredExe);
                    if (!found) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                                requiredExe + " was not found on PATH. Make sure it is installed and available.",
                                "Executable not found", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                }

                String[] shell = isWindows
                        ? new String[]{"cmd.exe", "/c", command}
                        : new String[]{"/bin/sh", "-c", command};

                ProcessBuilder pb = new ProcessBuilder(shell);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                java.io.InputStream is = p.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String out = s.hasNext() ? s.next() : "";
                int rc = p.waitFor();

                final int exitCode = rc;
                final String output = out;

                SwingUtilities.invokeLater(() -> {
                    if (exitCode == 0) {
                        JOptionPane.showMessageDialog(this, successMessage + "\n" + (output.isBlank() ? "" : output));
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Command failed (exit " + exitCode + ").\nOutput:\n" + output,
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Failed to run command: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private boolean checkCommandOnUnix(String cmd) {
        try {
            Process p = new ProcessBuilder("/bin/sh", "-c", "which " + cmd).start();
            int rc = p.waitFor();
            return rc == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkCommandOnWindows(String exe) {
        try {
            Process p = new ProcessBuilder("cmd.exe", "/c", "where " + exe).start();
            int rc = p.waitFor();
            return rc == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
