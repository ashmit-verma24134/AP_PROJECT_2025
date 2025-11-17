package edu.univ.erp.ui.common;

import edu.univ.erp.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class MainShell extends JPanel {
    private final JPanel cards;
    private final CardLayout cardLayout;
    private final Map<String, Component> pages = new HashMap<>();
    private final AppSidebar sidebar;
    private final JButton logoutButton;

    public MainShell() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setPreferredSize(new Dimension(0, 56)); // fixed height similar to previous
        header.setBorder(BorderFactory.createEmptyBorder(8, Theme.PADDING_X, 8, Theme.PADDING_X));

        JLabel appTitle = new JLabel("IIITD Portal - Admin ERP");
        appTitle.setForeground(Color.WHITE);
        appTitle.setFont(Theme.HEADER_FONT);
        header.add(appTitle, BorderLayout.WEST);

        // Logout button area right
        logoutButton = new JButton("Logout");
        logoutButton.setBackground(Theme.SURFACE);
        logoutButton.setForeground(Theme.PRIMARY_DARK);
        logoutButton.setFocusPainted(false);
        logoutButton.setFont(Theme.BODY_BOLD);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        right.add(logoutButton);
        header.add(right, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Sidebar + content
        sidebar = new AppSidebar();
        add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setBackground(Theme.BACKGROUND);
        cards.setBorder(BorderFactory.createEmptyBorder(Theme.PADDING_Y, Theme.PADDING_X, Theme.PADDING_Y, Theme.PADDING_X));
        add(cards, BorderLayout.CENTER);
    }

    public void addPage(String name, Component comp) {
        pages.put(name, comp);
        cards.add(comp, name);
    }

    public void showPage(String name) {
        cardLayout.show(cards, name);
    }

    // Helpers so callers can wire sidebar and logout
    public AppSidebar getSidebar() {
        return sidebar;
    }

    public void setLogoutAction(ActionListener al) {
        logoutButton.addActionListener(al);
    }
}
