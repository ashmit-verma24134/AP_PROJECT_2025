package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SlidePanel: holds named cards and slides between them.
 *
 * Usage (compatible with your StudentPanel):
 *   slide.addCardNamed("dashboard", dashboardComponent);
 *   slide.showCard("dashboard");
 *
 * Implementation notes:
 * - Stores components in a map for O(1) lookup.
 * - If sized width==0 (not laid out yet) it will immediately show the target card.
 * - Animation runs on EDT via javax.swing.Timer.
 */
public class SlidePanel extends JPanel {
    private final Map<String, JComponent> cards = new LinkedHashMap<>();
    private String visibleKey = null;

    public SlidePanel() {
        super(null); // manual layout for animation
        setOpaque(false);
    }

    /**
     * Add a card and associate it with a name.
     * The component will be managed by this panel.
     */
    public void addCardNamed(String name, JComponent comp) {
        if (name == null || comp == null) throw new IllegalArgumentException("name && comp required");
        comp.putClientProperty("cardName", name);
        comp.setName(name);
        comp.setVisible(false);
        add(comp);
        cards.put(name, comp);

        // If this is the first card added, show it immediately
        if (visibleKey == null) {
            visibleKey = name;
            comp.setVisible(true);
        }
        revalidate();
        repaint();
    }

    /**
     * Show the named card with a slide animation.
     * If the target is already visible, does nothing.
     */
   public void showCard(String name) {
    if (name == null) return;
    if (name.equals(visibleKey)) return;

    final JComponent to = cards.get(name);
    final JComponent from = visibleKey == null ? null : cards.get(visibleKey);

    if (to == null) return;

    final int w = getWidth();
    final int h = getHeight();

    if (w <= 0 || from == null) {
        if (from != null) from.setVisible(false);
        to.setBounds(0, 0, w <= 0 ? 1 : w, h);
        to.setVisible(true);
        visibleKey = name;
        revalidate();
        repaint();
        return;
    }

    to.setBounds(w, 0, w, h);
    to.setVisible(true);

    final int durationMs = 280;
    final int fps = 60;
    final int totalFrames = Math.max(1, durationMs * fps / 1000);
    final int dx = Math.max(1, w / totalFrames);
    final String targetName = name; // ✅ make effectively final for lambda use

    final Timer timer = new Timer(1000 / fps, null);
    timer.addActionListener(new AbstractAction() {
        int frame = 0;
        @Override
        public void actionPerformed(ActionEvent e) {
            frame++;
            from.setLocation(from.getX() - dx, 0);
            to.setLocation(to.getX() - dx, 0);
            repaint();
            if (frame >= totalFrames) {
                timer.stop();
                from.setVisible(false);
                from.setBounds(0, 0, w, h);
                to.setBounds(0, 0, w, h);
                visibleKey = targetName; // ✅ use final copy
                revalidate();
                repaint();
            }
        }
    });
    timer.setInitialDelay(0);
    timer.start();
}

    @Override
    public void doLayout() {
        // ensure all cards occupy full size when laying out
        int w = getWidth();
        int h = getHeight();
        for (Component c : getComponents()) {
            // keep current x for animating components, but ensure width/height match
            c.setSize(Math.max(1, w), Math.max(1, h));
            if (!c.isVisible()) {
                // keep offscreen to the right to avoid flicker
                c.setLocation(w, 0);
            } else {
                // ensure visible card is at (0,0)
                c.setLocation(0, 0);
            }
        }
    }
}
