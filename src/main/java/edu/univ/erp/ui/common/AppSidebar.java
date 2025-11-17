package edu.univ.erp.ui.common;

import edu.univ.erp.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AppSidebar extends JPanel {
    public AppSidebar() {
        setBackground(Theme.SIDEBAR_BG);
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 18, 8, 18);

        // Title area
        JLabel title = new JLabel("<html><span style='color:#FFFFFF;font-weight:bold;'>Admin Portal</span></html>");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        add(title, gbc);

        // Buttons container
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new GridLayout(0, 1, 0, 12));
        buttons.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));

        // Example buttons; external panels will add ActionListeners
        buttons.add(createSidebarButton("Dashboard"));
        buttons.add(createSidebarButton("Add Student"));
        buttons.add(createSidebarButton("Students"));
        buttons.add(createSidebarButton("Instructors"));
        buttons.add(createSidebarButton("Courses"));
        buttons.add(createSidebarButton("Departments"));
        buttons.add(createSidebarButton("Monitoring"));

        gbc.gridy = 1;
        add(buttons, gbc);

        // push everything up
        gbc.weighty = 1;
        gbc.gridy = 2;
        add(Box.createVerticalGlue(), gbc);
    }

    private JButton createSidebarButton(String text) {
        JButton b = new JButton(text);
        // Use theme tokens: transparent text over active color
        b.setBackground(Theme.SIDEBAR_ACTIVE);
        b.setForeground(Theme.SIDEBAR_ITEM);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        b.setFont(Theme.BODY_BOLD);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_ITEM_HEIGHT));
        return b;
    }

    // Expose helper to let callers add action listeners by button text
    public void addButtonListener(String buttonText, ActionListener listener) {
        // The buttons panel is at index 1 as constructed above
        Component panelComp = getComponentCount() > 1 ? getComponent(1) : null;
        if (!(panelComp instanceof JPanel)) return;
        for (Component c : ((JPanel) panelComp).getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                if (b.getText().equals(buttonText)) {
                    b.addActionListener(listener);
                    return;
                }
            }
        }
    }
}
