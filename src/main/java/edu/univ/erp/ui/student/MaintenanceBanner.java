package edu.univ.erp.ui.student;

import javax.swing.*;
import java.awt.*;

public class MaintenanceBanner extends JPanel {

    private final JLabel lbl = new JLabel(
            "Site is in Maintenance Mode. Some features may be disabled."
    );

    public MaintenanceBanner() {

        setLayout(new BorderLayout());

        // Background EXACT like your screenshot (#FDEEEE)
        setBackground(new Color(253, 238, 238));  

        // Label styling
        lbl.setForeground(new Color(150, 0, 0));  // dark red text
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // top/bottom spacing

        add(lbl, BorderLayout.CENTER);

        setVisible(false); // hidden by default
    }

    public void setMessage(String msg) { lbl.setText(msg); }
}
