package edu.univ.erp;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.Theme;

// FlatLaf (you already have dependency in pom.xml: com.formdev:flatlaf)
public class Main {
    public static void main(String[] args) {
        System.out.println("[Main] launching ui.MainFrame");

        SwingUtilities.invokeLater(() -> {
            try {
                // Use FlatLaf light theme for a modern base look
                com.formdev.flatlaf.FlatLightLaf.setup();

                // Apply some brand UI defaults (colors, fonts)
                UIManager.put("Button.font", Theme.BODY_FONT);
                UIManager.put("Label.font", Theme.BODY_FONT);
                UIManager.put("TextField.font", Theme.BODY_FONT);
                UIManager.put("PasswordField.font", Theme.BODY_FONT);
                UIManager.put("ToggleButton.font", Theme.BODY_FONT);

                // FlatLaf-specific color keys (improve selection/focus colors)
                UIManager.put("Component.focusColor", Theme.PRIMARY_TEAL);
                UIManager.put("TextComponent.selectionBackground", Theme.PRIMARY_TEAL);
                UIManager.put("Button.background", Theme.PRIMARY_TEAL);
                UIManager.put("Button.foreground", java.awt.Color.WHITE);

                // Small tweaks for consistent neutral background
                UIManager.put("Panel.background", Theme.SOFT_BG);
                UIManager.put("Viewport.background", Theme.SOFT_BG);

                // Optional: system look fallback removed (we use FlatLaf).
            } catch (Exception ex) {
                System.err.println("Failed to setup FlatLaf theme: " + ex.getMessage());
                try {
                    // fallback to system LAF
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignore) {}
            }

            // create + show the main frame
            MainFrame mainFrame = new MainFrame();
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setVisible(true);
        });
    }
}
