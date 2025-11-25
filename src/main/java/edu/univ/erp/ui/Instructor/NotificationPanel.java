package edu.univ.erp.ui.Instructor;

import edu.univ.erp.ui.Theme;
import edu.univ.erp.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationPanel for Instructor Dashboard
 * Shows recent notifications like assignment submissions, student queries, etc.
 */
public class NotificationPanel extends JPanel {
    
    private final JPanel notificationsContainer;
    private String term = null;

    private final JButton btnRefresh;
    private long instructorId = 0L;
    
    public NotificationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Theme.BACKGROUND);
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BACKGROUND);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        JLabel title = new JLabel(" Recent Notifications");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Theme.NEUTRAL_DARK);
        header.add(title, BorderLayout.WEST);
        
        btnRefresh = new JButton("🔄 Refresh");
        btnRefresh.setFont(Theme.BODY_FONT);
        btnRefresh.setBackground(Theme.PRIMARY);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btnRefresh.addActionListener(e -> loadNotifications());
        header.add(btnRefresh, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);
        
        // Notifications container
        notificationsContainer = new JPanel();
        notificationsContainer.setLayout(new BoxLayout(notificationsContainer, BoxLayout.Y_AXIS));
        notificationsContainer.setBackground(Theme.SURFACE);
        notificationsContainer.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        JScrollPane scrollPane = new JScrollPane(notificationsContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        
        // Initial placeholder
        addPlaceholder("Loading notifications...");
    }
    public void setInstructorContext(long instructorId, String term) {
    this.instructorId = instructorId;
    this.term = term;
    // call your existing loader — replace loadNotifications() with the real method name if different
    try {
        loadNotifications(); // implement this or replace with your existing refresh method
    } catch (NoSuchMethodError | RuntimeException ignored) {
        // If you don't have a loader yet, this block is safe; just ensures compile + runtime safety.
        System.out.println("NotificationPanel: context set for instructorId=" + instructorId + ", term=" + term);
    }
}

    
    public void setInstructorId(long instructorId) {
        this.instructorId = instructorId;
        loadNotifications();
    }
    
    
    private void addPlaceholder(String message) {
        notificationsContainer.removeAll();
        JLabel placeholder = new JLabel(message, SwingConstants.CENTER);
        placeholder.setFont(Theme.BODY_FONT);
        placeholder.setForeground(Theme.NEUTRAL_MED);
        placeholder.setBorder(new EmptyBorder(20, 20, 20, 20));
        notificationsContainer.add(placeholder);
        notificationsContainer.revalidate();
        notificationsContainer.repaint();
    }
    
    
    private void loadNotifications() {
        if (instructorId <= 0) {
            addPlaceholder("No instructor selected");
            return;
        }
        
        btnRefresh.setEnabled(false);
        addPlaceholder("Loading...");
        
        new SwingWorker<List<NotificationItem>, Void>() {
            @Override
            protected List<NotificationItem> doInBackground() throws Exception {
                List<NotificationItem> items = new ArrayList<>();
                
                try (Connection conn = DBConnection.getErpConnection()) {
                    // Get recent enrollment activities
                    String sql = """
                        SELECT 
                            st.full_name AS student_name,
                            c.code AS course_code,
                            c.title AS course_title,
                            e.created_at,
                            'enrollment' AS type
                        FROM enrollments e
                        JOIN students st ON e.student_id = st.student_id
                        JOIN sections sec ON e.section_id = sec.section_id
                        JOIN courses c ON sec.course_id = c.course_id
                        WHERE sec.instructor_id = ?
                        AND e.created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)
                        ORDER BY e.created_at DESC
                        LIMIT 10
                    """;
                    
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, instructorId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String studentName = rs.getString("student_name");
                                String courseCode = rs.getString("course_code");
                                String courseTitle = rs.getString("course_title");
                                Timestamp createdAt = rs.getTimestamp("created_at");
                                
                                String message = studentName + " enrolled in " + courseCode + " - " + courseTitle;
                                items.add(new NotificationItem(
                                    message,
                                    createdAt,
                                    "NEW_ENROLLMENT"
                                ));
                            }
                        }
                    }
                    
                    // Add system notifications
                    if (items.isEmpty()) {
                        items.add(new NotificationItem(
                            "No recent activity in your courses",
                            Timestamp.valueOf(LocalDateTime.now()),
                            "INFO"
                        ));
                    }
                }
                
                return items;
            }
            
            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                try {
                    List<NotificationItem> items = get();
                    displayNotifications(items);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    addPlaceholder("Failed to load notifications: " + ex.getMessage());
                }
            }
        }.execute();
    }
    
    private void displayNotifications(List<NotificationItem> items) {
        notificationsContainer.removeAll();
        
        if (items.isEmpty()) {
            addPlaceholder("No notifications");
            return;
        }
        
        for (NotificationItem item : items) {
            notificationsContainer.add(createNotificationCard(item));
            notificationsContainer.add(Box.createVerticalStrut(8));
        }
        
        notificationsContainer.revalidate();
        notificationsContainer.repaint();
    }
    
    private JPanel createNotificationCard(NotificationItem item) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.CARD_BORDER),
            new EmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        // Icon based on type
        JLabel icon = new JLabel(getIconForType(item.type));
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        icon.setVerticalAlignment(SwingConstants.TOP);
        card.add(icon, BorderLayout.WEST);
        
        // Content
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        
        JLabel message = new JLabel("<html>" + item.message + "</html>");
        message.setFont(Theme.BODY_FONT);
        message.setForeground(Theme.NEUTRAL_DARK);
        content.add(message, BorderLayout.CENTER);
        
        JLabel time = new JLabel(formatTimestamp(item.timestamp));
        time.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        time.setForeground(Theme.NEUTRAL_MED);
        content.add(time, BorderLayout.SOUTH);
        
        card.add(content, BorderLayout.CENTER);
        
        return card;
    }
    
    private String getIconForType(String type) {
        return switch (type) {
            case "NEW_ENROLLMENT" -> "👤";
            case "ASSIGNMENT" -> "📝";
            case "GRADE" -> "📊";
            case "INFO" -> "ℹ️";
            default -> "🔔";
        };
    }
    
    private String formatTimestamp(Timestamp timestamp) {
        LocalDateTime dateTime = timestamp.toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();
        
        long minutesAgo = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutesAgo < 1) return "Just now";
        if (minutesAgo < 60) return minutesAgo + " minutes ago";
        if (minutesAgo < 1440) return (minutesAgo / 60) + " hours ago";
        if (minutesAgo < 10080) return (minutesAgo / 1440) + " days ago";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return dateTime.format(formatter);
    }
    
    private static class NotificationItem {
        final String message;
        final Timestamp timestamp;
        final String type;
        
        NotificationItem(String message, Timestamp timestamp, String type) {
            this.message = message;
            this.timestamp = timestamp;
            this.type = type;
        }
    }
}