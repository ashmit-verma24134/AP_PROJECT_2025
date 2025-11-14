package edu.univ.erp.data;

import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Concrete DAO that uses the ERP schema you posted.
 */
public class StudentDaoImpl implements StudentDao {
    private final Connection conn;

    public StudentDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Long getStudentIdByRoll(String rollNo) throws Exception {
        final String sql = "SELECT student_id FROM students WHERE roll_no = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("student_id");
                return null;
            }
        }
    }

    @Override
    public Map<String, Object> getStudentOverview(String studentId) throws java.sql.SQLException {
        Map<String, Object> out = new HashMap<>();
        if (studentId == null) return out;

        // enrolled_count & total_credits for ENROLLED
        final String qEnroll =
            "SELECT COUNT(*) AS enrolled_count, COALESCE(SUM(c.credits),0) AS total_credits " +
            "FROM enrollments e " +
            "JOIN sections s ON e.section_id = s.section_id " +
            "JOIN courses c ON s.course_id = c.course_id " +
            "WHERE e.student_id = ? AND e.status = 'ENROLLED'";

        try (PreparedStatement ps = conn.prepareStatement(qEnroll)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.put("enrolled_count", rs.getInt("enrolled_count"));
                    out.put("total_credits", rs.getDouble("total_credits"));
                } else {
                    out.put("enrolled_count", 0);
                    out.put("total_credits", 0.0);
                }
            }
        } catch (Exception ex) {
            out.put("enrolled_count", 0);
            out.put("total_credits", 0.0);
        }

        // CGPA computed from completed enrollments' final_grade
        final String qCgpa =
            "SELECT CASE WHEN SUM(points * c.credits) IS NULL OR SUM(c.credits)=0 THEN NULL " +
            "            ELSE SUM(points * c.credits)/SUM(c.credits) END AS cgpa " +
            "FROM ( " +
            "  SELECT e.enrollment_id, e.section_id, " +
            "    CASE LOWER(g.final_grade) " +
            "      WHEN 'a+' THEN 10 WHEN 'a' THEN 10 WHEN 'a-' THEN 9 " +
            "      WHEN 'b+' THEN 8 WHEN 'b' THEN 7 WHEN 'b-' THEN 6 " +
            "      WHEN 'c+' THEN 5 WHEN 'c' THEN 4 WHEN 'c-' THEN 3 " +
            "      WHEN 'd' THEN 2 WHEN 'f' THEN 0 ELSE NULL END AS points " +
            "  FROM enrollments e " +
            "  JOIN grades g ON g.enrollment_id = e.enrollment_id " +
            "  WHERE e.student_id = ? AND e.status = 'COMPLETED' AND g.final_grade IS NOT NULL " +
            ") AS gmap " +
            "JOIN sections s ON gmap.section_id = s.section_id " +
            "JOIN courses c ON s.course_id = c.course_id " +
            "WHERE gmap.points IS NOT NULL";

        try (PreparedStatement ps = conn.prepareStatement(qCgpa)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Double cg = rs.getObject("cgpa") == null ? null : rs.getDouble("cgpa");
                    if (cg != null) cg = Math.round(cg * 100.0) / 100.0;
                    out.put("cgpa", cg);
                } else {
                    out.put("cgpa", null);
                }
            }
        } catch (Exception ex) {
            out.put("cgpa", null);
        }

        // attendance: compute using attendance table if exists
        try {
            Double at = getAttendancePercentage(studentId);
            out.put("attendance_percent", at);
        } catch (Exception ex) {
            out.put("attendance_percent", null);
        }

        // pending fees: not yet tracked in schema -> return 0.0
        out.put("pending_fees", 0.0);

        return out;
    }

    
@Override
public List<Map<String, Object>> getStudentTimetable(String studentId) throws Exception {
    List<Map<String, Object>> out = new ArrayList<>();
    if (studentId == null) return out;

    String sql =
        "SELECT s.section_id, s.day_time, s.room, c.code AS course_code, c.title AS course_title, " +
        "i.full_name AS instructor, e.status " +
        "FROM enrollments e " +
        "JOIN sections s ON e.section_id = s.section_id " +
        "JOIN courses c ON s.course_id = c.course_id " +
        "LEFT JOIN instructors i ON s.instructor_id = i.instructor_id " +
        "WHERE e.student_id = ? AND e.status IN ('ENROLLED','COMPLETED','WAITLISTED') " +
        "ORDER BY " +
        // try to order by day name if day_time starts with day abbrev e.g. 'Mon', 'Tue' etc:
        "FIELD(LEFT(s.day_time,3),'Mon','Tue','Wed','Thu','Fri','Sat','Sun'), s.day_time";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, studentId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> r = new HashMap<>();
                r.put("section_id", rs.getLong("section_id"));
                r.put("day_time", rs.getString("day_time"));
                r.put("room", rs.getString("room"));
                r.put("course_code", rs.getString("course_code"));
                r.put("course_title", rs.getString("course_title"));
                r.put("instructor", rs.getString("instructor"));
                r.put("status", rs.getString("status"));
                out.add(r);
            }
        }
    }
    return out;
}


    @Override
    public List<Map<String, Object>> getCurrentCourses(String studentId, String searchQuery) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        if (studentId == null) return list;

        String sql = "SELECT c.course_id, COALESCE(c.code, '') AS course_code, COALESCE(c.title, '') AS course_name, " +
                     "       COALESCE(i.full_name, '') AS instructor, s.day_time AS schedule, c.credits, e.status, s.section_id " +
                     "FROM enrollments e " +
                     "JOIN sections s ON e.section_id = s.section_id " +
                     "JOIN courses c ON s.course_id = c.course_id " +
                     "LEFT JOIN instructors i ON s.instructor_id = i.instructor_id " +
                     "WHERE e.student_id = ? " +
                     "  AND e.status IN ('ENROLLED','COMPLETED','WAITLISTED')";

        if (searchQuery != null && !searchQuery.isEmpty()) {
            sql += " AND (c.code LIKE ? OR c.title LIKE ? OR i.full_name LIKE ?)";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String q = "%" + searchQuery + "%";
                ps.setString(2, q);
                ps.setString(3, q);
                ps.setString(4, q);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> row = new HashMap<>();
                    row.put("course_id", rs.getLong("course_id"));
                    row.put("course_code", rs.getString("course_code"));
                    row.put("course_name", rs.getString("course_name"));
                    row.put("instructor", rs.getString("instructor"));
                    row.put("schedule", rs.getString("schedule"));
                    row.put("credits", rs.getDouble("credits"));
                    row.put("status", rs.getString("status"));
                    row.put("section_id", rs.getLong("section_id"));
                    list.add(row);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }
@Override
public List<Map<String, Object>> getStudentSchedule(String studentId) throws SQLException {
    List<Map<String,Object>> out = new ArrayList<>();

    final String sql =
        "SELECT s.section_id, c.code AS course_code, c.title AS course_title, " +
        "       s.day_time, s.room, e.status, COALESCE(i.full_name, '') AS instructor " +
        "FROM enrollments e " +
        "JOIN sections s ON e.section_id = s.section_id " +
        "JOIN courses c ON s.course_id = c.course_id " +
        "LEFT JOIN instructors i ON s.instructor_id = i.instructor_id " +
        "WHERE e.student_id = ? " +
        "  AND e.status IN ('ENROLLED','COMPLETED') " +
        "ORDER BY CASE WHEN e.status='ENROLLED' THEN 0 ELSE 1 END, s.day_time, s.section_id";

    // debug print (temporary)
    System.out.println("[StudentDaoImpl] getStudentSchedule SQL: " + sql);
    System.out.println("[StudentDaoImpl] studentId param: " + studentId);

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        // set numeric if possible
        try { ps.setLong(1, Long.parseLong(studentId)); }
        catch (NumberFormatException ex) { ps.setString(1, studentId); }

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("section_id", rs.getLong("section_id"));
                row.put("course_code", rs.getString("course_code"));
                row.put("course_title", rs.getString("course_title"));
                row.put("day_time", rs.getString("day_time"));
                row.put("room", rs.getString("room"));
                row.put("status", rs.getString("status"));
                row.put("instructor", rs.getString("instructor"));
                out.add(row);
            }
        }
    } catch (SQLException ex) {
        System.err.println("[StudentDaoImpl] getStudentSchedule ERROR: " + ex.getMessage());
        throw ex;
    }

    System.out.println("[StudentDaoImpl] rows returned: " + out.size());
    return out;
}


    @Override
public List<Map<String,Object>> getUpcomingSchedule(String studentId, int limit) throws Exception {
    List<Map<String,Object>> out = new ArrayList<>();
    if (studentId == null) return out;

    // Map day abbreviations to weekday order for ordering
    String sql =
        "SELECT c.code AS course_code, c.title AS course_title, s.room, s.day_time, s.section_id " +
        "FROM enrollments e " +
        "JOIN sections s ON e.section_id = s.section_id " +
        "JOIN courses c ON s.course_id = c.course_id " +
        "WHERE e.student_id = ? AND e.status = 'ENROLLED' " +
        // ensure day_time not null
        "AND s.day_time IS NOT NULL " +
        // order by day-of-week and time (assumes day_time like 'Mon 10:00' or 'Mon 10:00 AM')
        "ORDER BY FIELD(SUBSTRING_INDEX(s.day_time,' ',1),'Mon','Tue','Wed','Thu','Fri','Sat','Sun'), " +
        "STR_TO_DATE(SUBSTRING_INDEX(s.day_time,' ', -1), '%H:%i') " +
        "LIMIT ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, studentId);
        ps.setInt(2, limit);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> r = new HashMap<>();
                r.put("course_code", rs.getString("course_code"));
                r.put("course_title", rs.getString("course_title"));
                r.put("room", rs.getString("room"));
                r.put("day_time", rs.getString("day_time"));
                r.put("section_id", rs.getLong("section_id"));
                out.add(r);
            }
        }
    }
    return out;
}
@Override
public List<Map<String,Object>> getRecentGrades(String studentId, int limit) throws Exception {
    List<Map<String,Object>> out = new ArrayList<>();
    if (studentId == null) return out;

    String sql =
        "SELECT c.code AS course_code, c.title AS course_title, c.credits, g.final_grade, g.computed_at " +
        "FROM grades g " +
        "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
        "JOIN sections s ON e.section_id = s.section_id " +
        "JOIN courses c ON s.course_id = c.course_id " +
        "WHERE e.student_id = ? AND g.final_grade IS NOT NULL " +
        "ORDER BY g.computed_at DESC " +
        "LIMIT ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, studentId);
        ps.setInt(2, limit);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> r = new HashMap<>();
                r.put("course_code", rs.getString("course_code"));
                r.put("course_title", rs.getString("course_title"));
                r.put("credits", rs.getDouble("credits"));
                r.put("final_grade", rs.getString("final_grade"));
                r.put("computed_at", rs.getTimestamp("computed_at"));
                out.add(r);
            }
        }
    }
    return out;
}


    @Override
    public Double getAttendancePercentage(String studentId) throws Exception {
        // returns null if no attendance data available
        final String sql =
            "SELECT SUM(a.attended_classes) AS attended, SUM(a.total_classes) AS total " +
            "FROM attendance a " +
            "JOIN enrollments e ON a.enrollment_id = e.enrollment_id " +
            "WHERE e.student_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long attended = rs.getLong("attended");
                    long total = rs.getLong("total");
                    // if total is zero or null, return null to indicate missing data
                    if (rs.wasNull() || total == 0) return null;
                    double pct = (attended * 100.0) / (double) total;
                    pct = Math.round(pct * 100.0) / 100.0; // two decimals
                    return pct;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
