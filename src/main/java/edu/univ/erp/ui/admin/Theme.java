package edu.univ.erp.ui.admin;

import java.awt.*;

/**
 * Central theme / design tokens for the application.
 * - Color tokens are semantic (use CardBackground, Primary, Success etc.)
 * - Size tokens (radii, spacing) help keep UI consistent
 * - withAlpha(...) is a small helper to produce translucent colors
 *
 * Tweak values here to change the entire app look quickly.
 */
public final class Theme {
    // Primary brand (IIITD inspired)
    public static final Color PRIMARY      = new Color(0x2FB6AD); // #2FB6AD - main brand teal
    public static final Color PRIMARY_DARK = new Color(0x0B817D); // deeper teal for accents / hover
    public static final Color PRIMARY_LIGHT= new Color(0xE8F8F7); // subtle background tint

    // Neutrals
    public static final Color NEUTRAL_DARK  = new Color(0x3B3B3B); // primary text
    public static final Color NEUTRAL_MED   = new Color(0x78828C); // secondary text
    public static final Color NEUTRAL_LIGHT = new Color(0xDCE3E8); // light border / divider
    public static final Color BACKGROUND    = new Color(0xF5F7FA); // app background (soft)
    public static final Color SURFACE       = Color.WHITE;         // card / surface background

    // Status / semantic
    public static final Color SUCCESS = new Color(0x2ECC71);
    public static final Color WARNING = new Color(0xF1C40F);
    public static final Color DANGER  = new Color(0xE74C3C);
    public static final Color INFO    = new Color(0x3498DB);

    // Sidebar & nav
    public static final Color SIDEBAR_BG      = new Color(0x2F2F2F);
    public static final Color SIDEBAR_ACTIVE  = new Color(0x208A80);
    public static final Color SIDEBAR_ITEM    = new Color(0xFFFFFF, true);

    // Card / surface accents
    public static final Color CARD_BG         = SURFACE;
    public static final Color CARD_BORDER     = NEUTRAL_LIGHT;
    public static final Color SHADOW_COLOR    = withAlpha(0x000000, 0.08f); // subtle elevation shadow

    // Muted / disabled
    public static final Color MUTED           = new Color(0xA6ADB3);
    public static final Color DISABLED_BG     = new Color(0xF0F2F4);

    // Small utility colors used for overlays / subtle lines
    public static final Color DIVIDER = new Color(0xE6EEF0);

    // Type scale
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font TITLE_FONT  = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BODY_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font MONO_FONT   = new Font("Consolas", Font.PLAIN, 12);

    // Layout tokens (use instead of magic numbers)
    public static final int BORDER_RADIUS = 12;
    public static final int BUTTON_RADIUS = 10;
    public static final int PADDING_X = 16;
    public static final int PADDING_Y = 12;
    public static final int CARD_PADDING = 14;
    public static final int SIDEBAR_WIDTH = 220;
    public static final int NAV_ITEM_HEIGHT = 44;

    // Animation / timing tokens (ms)
    public static final int ANIM_FAST  = 120;
    public static final int ANIM_MID   = 220;
    public static final int ANIM_SLOW  = 350;

    private Theme() {}

    /**
     * Create a Color from a hex int (0xRRGGBB) with an alpha multiplier (0.0 - 1.0).
     * Example: withAlpha(0x000000, 0.08f) -> translucent black
     */
    public static Color withAlpha(int rgbHex, float alpha) {
        int r = (rgbHex >> 16) & 0xFF;
        int g = (rgbHex >> 8) & 0xFF;
        int b = (rgbHex) & 0xFF;
        return new Color(r, g, b, Math.max(0, Math.min(255, (int) (alpha * 255))));
    }

    /**
     * Convenience: returns a slightly darker shade for hover/active visuals.
     * Not perfect color science but handy for small UI tweaks.
     */
    public static Color darken(Color c, float factor) {
        factor = Math.max(0f, Math.min(1f, factor));
        int r = Math.max(0, (int) (c.getRed() * (1f - factor)));
        int g = Math.max(0, (int) (c.getGreen() * (1f - factor)));
        int b = Math.max(0, (int) (c.getBlue() * (1f - factor)));
        return new Color(r, g, b, c.getAlpha());
    }

    /**
     * Convenience: returns a slightly lighter shade for hover/pressed visuals.
     */
    public static Color lighten(Color c, float factor) {
        factor = Math.max(0f, Math.min(1f, factor));
        int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * factor));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * factor));
        int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * factor));
        return new Color(r, g, b, c.getAlpha());
    }
}
