package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import edu.univ.erp.service.SectionService;
import edu.univ.erp.service.SectionServiceImpl;
import edu.univ.erp.data.SectionDaoImpl;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Dialog now uses SectionService instead of JDBC directly.
 * Functionality is 100% identical to your original version.
 */
public class SectionDeadlineDialog extends JDialog {

    private final long sectionId;
    private final JTextField txtDeadline;
    private final JButton btnSave;
    private final JButton btnClear;
    private final JButton btnCancel;
    private final JLabel statusLabel;

    private final SectionService sectionService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SectionDeadlineDialog(Window owner, long sectionId) {
        super(owner, "Set Drop Deadline", ModalityType.APPLICATION_MODAL);
        this.sectionId = sectionId;

        // create service with DAO using ERP connection
        SectionService tmp = null;
try {
    tmp = new SectionServiceImpl(
            new SectionDaoImpl(DBConnection.getErpConnection())
    );
} catch (SQLException ex) {
    JOptionPane.showMessageDialog(this,
            "Failed to initialize section service: " + ex.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE);
}
this.sectionService = tmp;


        setLayout(new BorderLayout(10, 10));
        setSize(450, 250);
        setLocationRelativeTo(owner);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Set Drop Deadline for Section");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BACKGROUND);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Section ID:"), gbc);

        gbc.gridx = 1;
        JLabel lblSectionId = new JLabel(String.valueOf(sectionId));
        lblSectionId.setFont(Theme.BODY_BOLD);
        form.add(lblSectionId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Drop Deadline:"), gbc);

        gbc.gridx = 1;
        txtDeadline = new JTextField(20);
        txtDeadline.setToolTipText("Format: YYYY-MM-DD");
        form.add(txtDeadline, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        JLabel help = new JLabel(
                "<html><small>Format: YYYY-MM-DD<br/>Leave empty to allow drops anytime</small></html>");
        help.setForeground(Theme.NEUTRAL_MED);
        form.add(help, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Theme.DANGER);
        form.add(statusLabel, gbc);

        add(form, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setBackground(Theme.BACKGROUND);

        btnSave = new JButton("Save");
        btnSave.setBackground(Theme.PRIMARY);
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> saveDeadline());

        btnClear = new JButton("Clear Deadline");
        btnClear.setBackground(Theme.WARNING);
        btnClear.setForeground(Color.WHITE);
        btnClear.addActionListener(e -> clearDeadline());

        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        buttons.add(btnSave);
        buttons.add(btnClear);
        buttons.add(btnCancel);

        add(buttons, BorderLayout.SOUTH);

        // load existing deadline via service
        loadCurrentDeadline();

        setVisible(true);
    }

    private void loadCurrentDeadline() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                LocalDate d = sectionService.getDropDeadline(sectionId);
                return d == null ? "" : d.format(DATE_FORMAT);
            }

            @Override
            protected void done() {
                try {
                    txtDeadline.setText(get());
                    statusLabel.setText("Current deadline loaded");
                    statusLabel.setForeground(Theme.SUCCESS);
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load: " + ex.getMessage());
                    statusLabel.setForeground(Theme.DANGER);
                }
            }
        }.execute();
    }

    private void saveDeadline() {
        String dateStr = txtDeadline.getText().trim();

        if (dateStr.isEmpty()) {
            statusLabel.setText("Enter a date or click 'Clear Deadline'");
            return;
        }

        LocalDate deadline;
        try {
            deadline = LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (Exception ex) {
            statusLabel.setText("Invalid date format. Use YYYY-MM-DD");
            return;
        }

        btnSave.setEnabled(false);
        statusLabel.setText("Saving...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return sectionService.updateDropDeadline(sectionId, deadline);
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                try {
                    if (get()) {
                        statusLabel.setText("Deadline saved successfully!");
                        statusLabel.setForeground(Theme.SUCCESS);
                        Timer t = new Timer(1500, e -> dispose());
                        t.setRepeats(false);
                        t.start();
                    } else {
                        statusLabel.setText("Failed to save deadline");
                        statusLabel.setForeground(Theme.DANGER);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void clearDeadline() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Clear drop deadline? Students will be able to drop anytime.",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        btnClear.setEnabled(false);
        statusLabel.setText("Clearing...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return sectionService.clearDropDeadline(sectionId);
            }

            @Override
            protected void done() {
                btnClear.setEnabled(true);
                try {
                    if (get()) {
                        txtDeadline.setText("");
                        statusLabel.setText("Deadline cleared!");
                        statusLabel.setForeground(Theme.SUCCESS);
                        Timer t = new Timer(1500, e -> dispose());
                        t.setRepeats(false);
                        t.start();
                    } else {
                        statusLabel.setText("Failed to clear deadline");
                        statusLabel.setForeground(Theme.DANGER);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
