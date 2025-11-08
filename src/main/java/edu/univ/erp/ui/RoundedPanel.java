package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Simple rounded panel for nicer UI.
 */
public class RoundedPanel extends JPanel {
    private final int arc;

    /** Constructor with just radius */
    public RoundedPanel(int arc) {
        super();
        this.arc = arc;
        setOpaque(false);
    }

    /** ✅ New constructor with radius + background color */
    public RoundedPanel(int arc, Color bgColor) {
        super();
        this.arc = arc;
        setBackground(bgColor);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

