package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;

public class AnimatedSidebarButton extends JButton {
    public AnimatedSidebarButton(String text) {
        super(text);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setBackground(Theme.SIDEBAR_ACTIVE);
        setFont(Theme.BODY_FONT);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }
}
