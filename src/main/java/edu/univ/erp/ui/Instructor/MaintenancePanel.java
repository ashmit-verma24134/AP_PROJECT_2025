package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MaintenancePanel extends JPanel {

    public MaintenancePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ===== Main container =====
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(Theme.BACKGROUND);
        container.setBorder(new EmptyBorder(60, 0, 60, 0));

        // ===== Logo =====
        JLabel logo = new JLabel(new ImageIcon("src/main/resources/iiitd_logo.png")); // replace with your logo path
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== Icon (maintenance graphic placeholder) =====
        JLabel icon = new JLabel("🚧");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 70));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setBorder(new EmptyBorder(20, 0, 10, 0));

        // ===== Title =====
        JLabel title = new JLabel("System Under Maintenance");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Theme.NEUTRAL_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(new EmptyBorder(10, 0, 10, 0));

        // ===== Description =====
        JLabel desc = new JLabel("<html>We're currently performing scheduled maintenance<br>to improve your experience.</html>");
        desc.setFont(Theme.BODY_FONT);
        desc.setForeground(Theme.NEUTRAL_MED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        desc.setHorizontalAlignment(SwingConstants.CENTER);
        desc.setBorder(new EmptyBorder(5, 0, 20, 0));

        // ===== Info Box =====
        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setBackground(Theme.SURFACE);
        infoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                new EmptyBorder(20, 30, 20, 30)
        ));
        infoBox.setMaximumSize(new Dimension(480, 200));
        infoBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoBox.add(createInfoRow("⏰ Expected Downtime",
                "We expect to be back online within 2–3 hours. We apologize for any inconvenience."));

        infoBox.add(Box.createVerticalStrut(10));

        infoBox.add(createInfoRow("📩 Need Assistance?",
                "For urgent matters, please contact IT support at <a href='mailto:support@iiitd.ac.in'>support@iiitd.ac.in</a>."));

        // ===== Footer =====
        String lastUpdated = new SimpleDateFormat("MMM dd, yyyy, hh:mm a").format(new Date());
        JLabel footer = new JLabel(
                "<html><center>Thank you for your patience and understanding.<br>"
                        + "<small>Last updated: " + lastUpdated + "</small></center></html>",
                SwingConstants.CENTER
        );
        footer.setFont(Theme.BODY_FONT);
        footer.setForeground(Theme.NEUTRAL_MED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.setBorder(new EmptyBorder(30, 0, 10, 0));

        // ===== Add all to container =====
        container.add(logo);
        container.add(icon);
        container.add(title);
        container.add(desc);
        container.add(infoBox);
        container.add(footer);

        // Center wrapper
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(Theme.BACKGROUND);
        centerWrapper.add(container);

        add(centerWrapper, BorderLayout.CENTER);
    }

    private JPanel createInfoRow(String heading, String body) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(Theme.SURFACE);

        JLabel h = new JLabel(heading);
        h.setFont(Theme.BODY_BOLD);
        h.setForeground(Theme.NEUTRAL_DARK);

        JLabel b = new JLabel("<html><p style='width:400px;'>" + body + "</p></html>");
        b.setFont(Theme.BODY_FONT);
        b.setForeground(Theme.NEUTRAL_MED);

        row.add(h);
        row.add(Box.createVerticalStrut(4));
        row.add(b);
        return row;
    }
}
