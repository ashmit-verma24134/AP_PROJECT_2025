package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class StudentGradeServiceImpl implements StudentGradeService {

    @Override
    public Map<String, List<Map<String, Object>>> loadGradesForStudent(String studentId) throws Exception {

        Map<String, List<Map<String, Object>>> bySemester = new LinkedHashMap<>();

        String sql =
                "SELECT sec.semester AS sem_label, sec.year AS sem_year, e.enrollment_id, " +
                "c.code AS course_code, c.title AS course_title, c.credits AS credits, " +
                "g_final.final_grade AS final_letter, g_final.score AS final_score " +
                "FROM enrollments e " +
                "JOIN sections sec ON e.section_id = sec.section_id " +
                "JOIN courses c ON sec.course_id = c.course_id " +
                "LEFT JOIN grades g_final ON g_final.enrollment_id = e.enrollment_id " +
                "   AND LOWER(g_final.component) = 'final' " +
                "WHERE e.student_id = ? AND e.status IN ('ENROLLED','COMPLETED') " +
                "ORDER BY sec.year DESC, c.code ASC";

        try (Connection conn = DBConnection.getErpConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            try { ps.setLong(1, Long.parseLong(studentId)); }
            catch (NumberFormatException ex) { ps.setString(1, studentId); }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String sem = rs.getString("sem_label");
                    Integer year = rs.getObject("sem_year") == null ? null : rs.getInt("sem_year");

                    String key = sem == null
                            ? (year == null ? "Unknown" : "Year " + year)
                            : (sem + (year == null ? "" : " / " + year));

                    Map<String, Object> row = new HashMap<>();
                    row.put("enrollment_id", rs.getLong("enrollment_id"));
                    row.put("course_code", rs.getString("course_code"));
                    row.put("course_title", rs.getString("course_title"));
                    row.put("credits", rs.getObject("credits"));
                    row.put("final_letter", rs.getString("final_letter"));
                    row.put("final_score", rs.getObject("final_score"));

                    bySemester.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
                }
            }
        }

        return bySemester;
    }
}
