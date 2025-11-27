package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Implementation that queries the DB and returns CourseRow DTOs grouped by semester.
 */
public class StudentGradeServiceImpl implements StudentGradeService {

    @Override
    public Map<String, List<CourseRow>> loadGradesForStudent(String studentId) throws Exception {

        Map<String, List<CourseRow>> bySemester = new LinkedHashMap<>();

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

                    long enrollmentId = rs.getLong("enrollment_id");
                    String courseCode = rs.getString("course_code");
                    String courseTitle = rs.getString("course_title");

                    Integer credits = null;
                    Object credObj = rs.getObject("credits");
                    if (credObj instanceof Number) credits = ((Number) credObj).intValue();
                    else if (credObj != null) {
                        try { credits = Integer.parseInt(String.valueOf(credObj)); } catch (Exception ignored) {}
                    }

                    String finalLetter = rs.getString("final_letter");

                    Double finalScore = null;
                    Object fsObj = rs.getObject("final_score");
                    if (fsObj instanceof Number) finalScore = ((Number) fsObj).doubleValue();
                    else if (fsObj != null) {
                        try { finalScore = Double.parseDouble(String.valueOf(fsObj)); } catch (Exception ignored) {}
                    }

                    StudentGradeService.CourseRow cr = new StudentGradeService.CourseRow(
                            enrollmentId, courseCode, courseTitle, credits, finalLetter, finalScore
                    );

                    bySemester.computeIfAbsent(key, k -> new ArrayList<>()).add(cr);
                }
            }
        }

        return bySemester;
    }
}