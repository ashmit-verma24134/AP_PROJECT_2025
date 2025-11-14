package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class InstructorTimetablePanel extends JPanel {

    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private static final String[] TIME_SLOTS = {"8:00 - 9:00", "9:00 - 10:00", "10:00 - 11:00", "11:00 - 12:00",
            "12:00 - 1:00", "1:00 - 2:00", "2:00 - 3:00", "3:00 - 4:00", "4:00 - 5:00"};

    public InstructorTimetablePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // ==== HEADER ====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("📅 Weekly Teaching Timetable");
        title.setForeground(Color.WHITE);
        title.setFont(Theme.HEADER_FONT);
        header.add(title, BorderLayout.WEST);

        JLabel subtitle = new JLabel("Monsoon 2025");
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(Theme.BODY_BOLD);
        header.add(subtitle, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ==== MAIN GRID ====
        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setBackground(Theme.BACKGROUND);
        gridPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(2, 2, 2, 2);

        // Top-left corner (blank)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gridPanel.add(createHeaderCell(""), gbc);

        // Time slot headers (top row)
        for (int t = 0; t < TIME_SLOTS.length; t++) {
            gbc.gridx = t + 1;
            gbc.gridy = 0;
            gridPanel.add(createHeaderCell(TIME_SLOTS[t]), gbc);
        }

        // Populate dummy timetable data (can connect to DB later)
        Map<String, String> schedule = new HashMap<>();
        schedule.put("Monday-9:00 - 10:00", "CS101\nR201");
        schedule.put("Monday-2:00 - 3:00", "MA202\nR107");
        schedule.put("Tuesday-11:00 - 12:00", "CS101\nR201");
        schedule.put("Wednesday-10:00 - 11:00", "HS301\nR105");
        schedule.put("Thursday-8:00 - 9:00", "CS301\nLab 2");
        schedule.put("Friday-3:00 - 4:00", "CS101\nR201");

        // Day rows
        for (int d = 0; d < DAYS.length; d++) {
            gbc.gridx = 0;
            gbc.gridy = d + 1;
            gridPanel.add(createDayHeader(DAYS[d]), gbc);

            // Time columns
            for (int t = 0; t < TIME_SLOTS.length; t++) {
                gbc.gridx = t + 1;
                gbc.gridy = d + 1;

                String key = DAYS[d] + "-" + TIME_SLOTS[t];
                String val = schedule.getOrDefault(key, "");
                gridPanel.add(createSlotCell(val), gbc);
            }
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ==== HEADER CELL ====
    private JPanel createHeaderCell(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.PRIMARY_DARK);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BACKGROUND));

        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(Theme.BODY_BOLD);
        panel.add(label, BorderLayout.CENTER);

        panel.setPreferredSize(new Dimension(120, 40));
        return panel;
    }

    // ==== DAY HEADER CELL ====
    private JPanel createDayHeader(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BACKGROUND));

        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.BODY_BOLD);
        label.setForeground(Theme.NEUTRAL_DARK);
        panel.add(label, BorderLayout.CENTER);

        panel.setPreferredSize(new Dimension(120, 60));
        return panel;
    }

    // ==== TIME SLOT CELL ====
    private JPanel createSlotCell(String courseInfo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER));
        panel.setBackground(Theme.SURFACE);

        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(Theme.BODY_FONT);
        label.setForeground(Theme.NEUTRAL_DARK);
        label.setOpaque(false);

        if (!courseInfo.isEmpty()) {
            String[] parts = courseInfo.split("\n");
            String course = parts[0];
            String room = parts.length > 1 ? parts[1] : "";

            label.setText("<html><center><b>" + course + "</b><br/><small>" + room + "</small></center></html>");
            panel.setBackground(Theme.SUCCESS); // highlight scheduled classes
            label.setForeground(Color.WHITE);

            // Hover tooltip
            panel.setToolTipText("Course: " + course + (room.isEmpty() ? "" : " | Room: " + room));

            // Hover effect
            panel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    panel.setBackground(Theme.PRIMARY_DARK);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    panel.setBackground(Theme.SUCCESS);
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(panel,
                            "Open Course Page for: " + course,
                            "Navigate", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }

        panel.add(label, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(120, 60));
        return panel;
    }
}
