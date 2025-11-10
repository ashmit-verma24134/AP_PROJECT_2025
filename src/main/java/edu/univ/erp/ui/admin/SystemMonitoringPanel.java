package edu.univ.erp.ui.admin;

import edu.univ.erp.ui.Theme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SystemMonitoringPanel extends JPanel {
    public SystemMonitoringPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel title = new JLabel("⚙️ System Monitoring");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(new EmptyBorder(30, 50, 40, 50));

        content.add(createStatus("Database Status", "Healthy", Theme.SUCCESS));
        content.add(Box.createVerticalStrut(10));
        content.add(createStatus("Server Status", "Online", Theme.SUCCESS));
        content.add(Box.createVerticalStrut(10));
        content.add(createStatus("Backup Process", "Running", Theme.WARNING));
        content.add(Box.createVerticalStrut(10));
        content.add(createStatus("API Services", "Operational", Theme.SUCCESS));
        content.add(Box.createVerticalStrut(10));
        content.add(createStatus("Mail Service", "Delayed", Theme.DANGER));

        add(content, BorderLayout.CENTER);
    }

    private JPanel createStatus(String label, String status, Color color) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Theme.SURFACE);
        row.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel l = new JLabel(label);
        l.setFont(Theme.BODY_BOLD);

        JLabel s = new JLabel(status);
        s.setOpaque(true);
        s.setBackground(Theme.withAlpha(color.getRGB(), 0.2f));
        s.setForeground(color.darker());
        s.setBorder(new EmptyBorder(4, 10, 4, 10));

        row.add(l, BorderLayout.WEST);
        row.add(s, BorderLayout.EAST);
        return row;
    }
}
