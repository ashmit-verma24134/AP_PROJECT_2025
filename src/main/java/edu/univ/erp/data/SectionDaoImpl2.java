package edu.univ.erp.data;

import edu.univ.erp.util.DBConnection;
import java.sql.*;
import java.util.*;

public class SectionDaoImpl2 implements SectionDao2 {

    @Override
    public long createSection(long courseId, String days, String start, String end,
                              String room, int capacity, String sem, int year, Long instructorId) throws Exception {

        String sql = "INSERT INTO sections (course_id, instructor_id, day_of_week, start_time, end_time, room, capacity, semester, year) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, courseId);
            if (instructorId == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, instructorId);
            ps.setString(3, days);
            ps.setString(4, start);
            ps.setString(5, end);
            ps.setString(6, room);
            ps.setInt(7, capacity);
            ps.setString(8, sem);
            ps.setInt(9, year);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new Exception("Failed to insert section");
    }

    @Override
    public void updateSection(long id, long courseId, String days, String start, String end,
                              String room, int capacity, String sem, int year, Long instructorId) throws Exception {

        String sql = "UPDATE sections SET course_id=?, instructor_id=?, day_of_week=?, start_time=?, end_time=?, room=?, capacity=?, semester=?, year=? WHERE id=?";

        try (Connection con = DBConnection.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, courseId);
            if (instructorId == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, instructorId);
            ps.setString(3, days);
            ps.setString(4, start);
            ps.setString(5, end);
            ps.setString(6, room);
            ps.setInt(7, capacity);
            ps.setString(8, sem);
            ps.setInt(9, year);
            ps.setLong(10, id);

            ps.executeUpdate();
        }
    }

    @Override
    public List<Map<String,Object>> listSectionsForCourse(long courseId) throws Exception {
        List<Map<String,Object>> result = new ArrayList<>();

        String sql = "SELECT * FROM sections WHERE course_id=? ORDER BY id";

        try (Connection con = DBConnection.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, courseId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("days", rs.getString("day_of_week"));
                    row.put("start", rs.getString("start_time"));
                    row.put("end", rs.getString("end_time"));
                    row.put("room", rs.getString("room"));
                    row.put("capacity", rs.getInt("capacity"));
                    row.put("semester", rs.getString("semester"));
                    row.put("year", rs.getInt("year"));
                    row.put("instructor_id", rs.getLong("instructor_id"));
                    result.add(row);
                }
            }
        }
        return result;
    }
}
