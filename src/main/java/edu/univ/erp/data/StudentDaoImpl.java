package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDaoImpl implements StudentDao {
    private final Connection conn;

    public StudentDaoImpl(Connection conn) { 
        this.conn = conn; 
    }

    @Override
    public Long getStudentIdByRoll(String rollNo) throws Exception {
        String sql = "SELECT student_id FROM students WHERE roll_no = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("student_id");
                } else {
                    return null;
                }
            }
        }
    }

    // ✅ Add this new method below
    @Override
    public List<Map<String, Object>> getCurrentCourses(String studentId, String searchQuery) {
        List<Map<String, Object>> list = new ArrayList<>();

        String sql = "SELECT c.course_code, c.course_name, i.name AS instructor, " +
                     "s.day_time AS schedule, c.credits, e.status " +
                     "FROM enrollment e " +
                     "JOIN course c ON e.course_id = c.course_id " +
                     "JOIN section s ON e.section_id = s.section_id " +
                     "JOIN instructor i ON s.instructor_id = i.id " +
                     "WHERE e.student_id = ? AND e.semester = 'Fall 2025'"; // you can later replace with current semester dynamically

        // Add search filters dynamically
        if (searchQuery != null && !searchQuery.isEmpty()) {
            sql += " AND (c.course_code LIKE ? OR c.course_name LIKE ? OR i.name LIKE ?)";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);

            if (searchQuery != null && !searchQuery.isEmpty()) {
                ps.setString(2, "%" + searchQuery + "%");
                ps.setString(3, "%" + searchQuery + "%");
                ps.setString(4, "%" + searchQuery + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("course_code", rs.getString("course_code"));
                    row.put("course_name", rs.getString("course_name"));
                    row.put("instructor", rs.getString("instructor"));
                    row.put("schedule", rs.getString("schedule"));
                    row.put("credits", rs.getInt("credits"));
                    row.put("status", rs.getString("status"));
                    list.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}

