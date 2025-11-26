package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Simple styled banner that displays under the header.
 * No close button — visible only when maintenance flag is ON.
 */
public class MaintenanceBanner extends JPanel {
    private final JLabel label = new JLabel();

    public MaintenanceBanner() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        // Strong, attention-grabbing background but not too harsh
        setBackground(new Color(255, 245, 240)); // very light warm tint
        label.setText("Site is in Maintenance Mode — contents are view-only.");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setForeground(new Color(120, 30, 20)); // dark warm text
        label.setHorizontalAlignment(SwingConstants.CENTER);

        add(label, BorderLayout.CENTER);
        setPreferredSize(new Dimension(10, 36)); // small fixed height so it's noticeable
    }
}
