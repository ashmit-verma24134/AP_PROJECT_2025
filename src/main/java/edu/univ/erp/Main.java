package edu.univ.erp;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import edu.univ.erp.ui.MainFrame;
import edu.univ.erp.ui.Theme;
import com.formdev.flatlaf.FlatLightLaf;

import java.awt.*;

/**
 * App entry: init FlatLaf + UI defaults and show MainFrame.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("[Main] launching ui.MainFrame");

        SwingUtilities.invokeLater(() -> {
            try {
                // initialize FlatLaf light theme
                FlatLightLaf.setup();

                // Global font + component defaults (assumes Theme constants exist)
                UIManager.put("defaultFont", Theme.BODY_FONT);
                UIManager.put("Button.font", Theme.BODY_FONT);
                UIManager.put("Label.font", Theme.BODY_FONT);
                UIManager.put("TextField.font", Theme.BODY_FONT);
                UIManager.put("PasswordField.font", Theme.BODY_FONT);
                UIManager.put("ToggleButton.font", Theme.BODY_FONT);
                UIManager.put("TabbedPane.font", Theme.BODY_FONT);

                // ✅ Updated Accent / brand colors to use new Theme constants
                UIManager.put("Component.focusColor", Theme.PRIMARY);
                UIManager.put("TextComponent.selectionBackground", Theme.PRIMARY);
                UIManager.put("Button.background", Theme.PRIMARY);
                UIManager.put("Button.foreground", Color.WHITE);

                // Rounded components and spacing (FlatLaf keys)
                UIManager.put("Button.arc", 12);
                UIManager.put("Component.arc", 10);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("Table.rowHeight", 44);
                UIManager.put("Component.focusWidth", 0);

                // ✅ Neutral background (SOFT_BG → BACKGROUND)
                UIManager.put("Panel.background", Theme.BACKGROUND);
                UIManager.put("Viewport.background", Theme.BACKGROUND);
                UIManager.put("TabbedPane.tabAreaBackground", Theme.BACKGROUND);

            } catch (Exception ex) {
                System.err.println("[Main] Failed to setup FlatLaf: " + ex.getMessage());
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignore) {}
            }

            // create and show main window (assumes MainFrame has a default constructor)
            MainFrame mainFrame = new MainFrame();
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setVisible(true);
        });
    }
}
