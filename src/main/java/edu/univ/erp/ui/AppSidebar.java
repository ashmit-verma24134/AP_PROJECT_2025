package ui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AppSidebar extends JPanel {
    public AppSidebar() {
        setBackground(Theme.SIDEBAR_BG);
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 18, 8, 18);

        // Title area
        JLabel title = new JLabel("<html><span style='color:#FFFFFF;font-weight:bold;'>Admin Portal</span></html>");
        title.setFont(Theme.H2);
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        add(title, gbc);

        // Buttons container
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new GridLayout(0,1,0,12));
        buttons.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));

        // Example buttons; external panels will add ActionListeners
        buttons.add(createSidebarButton("Add Student"));
        buttons.add(createSidebarButton("Monitoring"));
        buttons.add(createSidebarButton("Dashboard"));
        buttons.add(createSidebarButton("Students"));
        buttons.add(createSidebarButton("Departments"));
        buttons.add(createSidebarButton("Instructors"));
        buttons.add(createSidebarButton("Courses"));

        gbc.gridy = 1;
        add(buttons, gbc);

        // push everything up
        gbc.weighty = 1;
        gbc.gridy = 2;
        add(Box.createVerticalGlue(), gbc);
    }

    private JButton createSidebarButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Theme.SIDEBAR_BUTTON);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        b.setFont(Theme.BODY.deriveFont(Font.BOLD));
        return b;
    }

    // Expose helper to let callers add action listeners by button text
    public void addButtonListener(String buttonText, ActionListener listener) {
        for (Component c : ((JPanel)getComponent(1)).getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton)c;
                if (b.getText().equals(buttonText)) {
                    b.addActionListener(listener);
                    return;
                }
            }
        }
    }
}
