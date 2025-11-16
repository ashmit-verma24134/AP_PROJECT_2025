package edu.univ.erp.ui.student;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Collapsible semester section with smooth slide animation.
 */
public class SemesterPanel extends JPanel {

    private static final int ANIM_MS = 200;
    private static final int ANIM_FPS = 30;

    private final JPanel header;
    private final JLabel lblTitle;
    private final JLabel lblChevron;

    private final JPanel bodyWrapper;
    private boolean expanded = true;

    private final Timer animator;
    private int startHeight = 0;
    private int targetHeight = 0;
    private long animStartTime = 0;

    public SemesterPanel(String title, JPanel body) {
        super(new BorderLayout());
        setBackground(Color.WHITE);

        // ---------- HEADER ----------
        header = new JPanel(new BorderLayout());
        header.setBackground(new Color(40, 55, 95));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        lblTitle = new JLabel(title);
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        lblChevron = new JLabel("▼"); // down arrow
        lblChevron.setForeground(Color.WHITE);
        lblChevron.setFont(new Font("Segoe UI", Font.BOLD, 14));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblChevron, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ---------- BODY WRAPPER (animates height) ----------
        bodyWrapper = new JPanel(new BorderLayout());
        bodyWrapper.add(body, BorderLayout.CENTER);
        bodyWrapper.setOpaque(true);
        add(bodyWrapper, BorderLayout.CENTER);

        // ---------- ANIMATOR ----------
        animator = new Timer(1000 / ANIM_FPS, e -> animateStep());
        animator.setRepeats(true);

        // ---------- CLICK TOGGLE ----------
        MouseAdapter clickToggle = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle();
            }
        };
        header.addMouseListener(clickToggle);
        lblTitle.addMouseListener(clickToggle);
        lblChevron.addMouseListener(clickToggle);
    }

    // ---------- ANIMATION STEP ----------
    private void animateStep() {
        float t = (System.currentTimeMillis() - animStartTime) / (float) ANIM_MS;
        if (t > 1f) t = 1f;

        int newH = startHeight + (int) ((targetHeight - startHeight) * t);
        bodyWrapper.setPreferredSize(new Dimension(bodyWrapper.getWidth(), Math.max(0, newH)));
        revalidate();

        if (t >= 1f) {
            animator.stop();

            if (!expanded) {
                bodyWrapper.setVisible(false);
            } else {
                bodyWrapper.setPreferredSize(null);
            }

            lblChevron.setText(expanded ? "▲" : "▼");
        }
    }

    // ---------- TOGGLE OPEN/CLOSE ----------
    private void toggle() {
        if (animator.isRunning())
            animator.stop();

        expanded = !expanded;

        if (expanded) {
            // Expand
            bodyWrapper.setVisible(true);
            bodyWrapper.setPreferredSize(null);
            bodyWrapper.revalidate();
            targetHeight = bodyWrapper.getPreferredSize().height;
            startHeight = 0;
        } else {
            // Collapse
            startHeight = bodyWrapper.getHeight();
            targetHeight = 0;
        }

        animStartTime = System.currentTimeMillis();
        animator.start();
    }
}
