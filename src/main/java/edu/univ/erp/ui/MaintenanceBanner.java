package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;

public class MaintenanceBanner extends JPanel {

    private final JLabel label;

    public MaintenanceBanner() {
        setLayout(new BorderLayout());
        setBackground(new Color(200, 40, 40));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        label = new JLabel("⚠ System under maintenance — Changes are disabled");
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));

        add(label, BorderLayout.CENTER);

        setVisible(false); // default hidden
    }

    public void showBanner(boolean visible) {
        setVisible(visible);
    }
}
