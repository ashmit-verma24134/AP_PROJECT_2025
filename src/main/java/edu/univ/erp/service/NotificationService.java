package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService
 * Loads instructor notifications (recent enrollments, system notices, etc.)
 */
public class NotificationService {

    public static class NotificationItem {
        public final String message;
        public final Timestamp timestamp;
        public final String type;

        public NotificationItem(String message, Timestamp timestamp, String type) {
            this.message = message;
            this.timestamp = timestamp;
            this.type = type;
        }
    }

    /**
     * Loads recent notifications for an instructor.
     */
    public List<NotificationItem> loadInstructorNotifications(long instructorId) throws Exception {
        List<NotificationItem> items = new ArrayList<>();

        try (Connection conn = DBConnection.getErpConnection()) {

            String sql = """
                SELECT 
                    st.full_name AS student_name,
                    c.code AS course_code,
                    c.title AS course_title,
                    e.created_at
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

            // If no recent items, show system info
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
}
