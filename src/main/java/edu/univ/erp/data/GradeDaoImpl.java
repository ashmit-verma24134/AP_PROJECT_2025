package edu.univ.erp.data;

import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.util.*;

/**
 * GradeDaoImpl - DAO for assessment components and student scores.
 *
 * Uses `grades` table (one row per component per enrollment) and enrollments/sections/courses.
 * Assumes top-level edu.univ.erp.data.AssessmentComponent POJO exists.
 */
public class GradeDaoImpl implements GradeDao {

    private final Connection conn;

    public GradeDaoImpl(Connection conn) {
        this.conn = conn;
    }

    // ------------------------------------------------------------
    // getStudentGrades: returns map rows used by older UI (backwards compat)
    // ------------------------------------------------------------
    @Override
    public List<Map<String, Object>> getStudentGrades(long studentId) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();

        final String sql =
            "SELECT e.enrollment_id, c.code AS course_code, c.title AS course_name, " +
            "       ac.name AS component_name, s.score, ac.max_score, ac.weight, e.final_grade " +
            "FROM enrollments e " +
            "JOIN sections sec ON e.section_id = sec.section_id " +
            "JOIN courses c ON sec.course_id = c.course_id " +
            "LEFT JOIN assessment_component ac ON ac.section_id = sec.section_id " +
            "LEFT JOIN assessment_score s ON s.assessment_id = ac.id AND s.enrollment_id = e.enrollment_id " +
            "WHERE e.student_id = ? " +
            "  AND e.status IN ('ENROLLED','COMPLETED') " +
            "ORDER BY c.code, ac.id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("enrollment_id", rs.getLong("enrollment_id"));
                    row.put("course_code", rs.getString("course_code"));
                    row.put("course_name", rs.getString("course_name"));
                    row.put("component_name", rs.getString("component_name"));
                    Object scrObj = rs.getObject("score");
                    row.put("score", scrObj == null ? null : rs.getDouble("score"));
                    Object mxObj = rs.getObject("max_score");
                    row.put("max_score", mxObj == null ? null : rs.getDouble("max_score"));
                    Object wtObj = rs.getObject("weight");
                    row.put("weight", wtObj == null ? null : rs.getInt("weight"));
                    row.put("final_grade", rs.getString("final_grade"));
                    out.add(row);
                }
            }
        }

        return out;
    }

    // ------------------------------------------------------------
    // findComponentsForEnrollment: returns typed AssessmentComponent list
    // ------------------------------------------------------------
    @Override
    public List<AssessmentComponent> findComponentsForEnrollment(long enrollmentId, boolean includeScores) throws Exception {
        List<AssessmentComponent> out = new ArrayList<>();

        final String sql =
            "SELECT g.grade_id AS ac_id, e.section_id, g.component AS name, g.weight, g.max_score, " +
            (includeScores ? "g.score AS student_score " : "NULL AS student_score ") +
            "FROM grades g " +
            "JOIN enrollments e ON e.enrollment_id = g.enrollment_id " +
            "WHERE g.enrollment_id = ? " +
            "ORDER BY g.grade_id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("ac_id");
                    long sectionId = rs.getLong("section_id");
                    String name = rs.getString("name");

                    Integer weight = null;
                    Object wtObj = rs.getObject("weight");
                    if (wtObj != null) {
                        try {
                            int w = rs.getInt("weight");
                            if (!rs.wasNull()) weight = w;
                        } catch (Exception ignore) { weight = null; }
                    }

                    Double maxScore = null;
                    Object mxObj = rs.getObject("max_score");
                    if (mxObj != null) {
                        try {
                            double m = rs.getDouble("max_score");
                            if (!rs.wasNull()) maxScore = m;
                        } catch (Exception ignore) { maxScore = null; }
                    }

                    Double studentScore = null;
                    Object scObj = rs.getObject("student_score");
                    if (scObj != null) {
                        try {
                            double s = rs.getDouble("student_score");
                            if (!rs.wasNull()) studentScore = s;
                        } catch (Exception ignore) { studentScore = null; }
                    }

                    Boolean published = (studentScore != null);

                    AssessmentComponent a = new AssessmentComponent();
                    a.setId(id);
                    a.setSectionId(sectionId);
                    a.setName(name);
                    a.setWeight(weight);
                    a.setMaxScore(maxScore);
                    a.setPublished(published);
                    a.setStudentScore(studentScore);

                    out.add(a);
                }
            }
        }

        return out;
    }

    // ------------------------------------------------------------
    // createOrUpdateFinalRow
    // ------------------------------------------------------------
    @Override
    public long createOrUpdateFinalRow(long enrollmentId, double percent, String letter) throws Exception {
        final String sql = ""
            + "INSERT INTO grades (enrollment_id, component, score, max_score, weight, final_grade, computed_at, created_at, updated_at) "
            + "VALUES (?, 'Final', ?, 100.0, NULL, ?, NOW(), NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE "
            + "  score = VALUES(score), "
            + "  max_score = VALUES(max_score), "
            + "  final_grade = VALUES(final_grade), "
            + "  computed_at = NOW(), "
            + "  updated_at = NOW()";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, enrollmentId);
            ps.setDouble(2, percent);
            ps.setString(3, letter);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        // fallback: fetch id
        try (PreparedStatement ps = conn.prepareStatement("SELECT grade_id FROM grades WHERE enrollment_id = ? AND component = 'Final' LIMIT 1")) {
            ps.setLong(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        return -1L;
    }

    // ------------------------------------------------------------
    // upsertAssessmentScore (enrollment+component style) - used by instructor UI
    // ------------------------------------------------------------
    @Override
    public void upsertAssessmentScore(long enrollmentId, String component, Double score, Double maxScore, Double weight) throws Exception {
        final String sql = ""
            + "INSERT INTO grades (enrollment_id, component, score, max_score, weight, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE "
            + "  score = VALUES(score), "
            + "  max_score = VALUES(max_score), "
            + "  weight = VALUES(weight), "
            + "  updated_at = NOW()";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            ps.setString(2, component);
            if (score == null) ps.setNull(3, java.sql.Types.DOUBLE); else ps.setDouble(3, score);
            if (maxScore == null) ps.setNull(4, java.sql.Types.DOUBLE); else ps.setDouble(4, maxScore);
            if (weight == null) ps.setNull(5, java.sql.Types.DOUBLE); else ps.setDouble(5, weight);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // upsertAssessmentScore (assessmentId + enrollment) - assessment_score table style
    // ------------------------------------------------------------
    @Override
    public boolean upsertAssessmentScore(long assessmentId, long enrollmentId, Double score) throws SQLException {
        final String sql =
            "INSERT INTO assessment_score (assessment_id, enrollment_id, score, updated_at) "
            + "VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
            + "ON DUPLICATE KEY UPDATE score = VALUES(score), updated_at = CURRENT_TIMESTAMP";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assessmentId);
            ps.setLong(2, enrollmentId);
            if (score == null) ps.setNull(3, Types.DOUBLE);
            else ps.setDouble(3, score);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    // ------------------------------------------------------------
    // createAssessmentComponent
    // ------------------------------------------------------------
    @Override
    public long createAssessmentComponent(long sectionId, String name, Integer weight, Double maxScore, boolean published) throws SQLException {
        final String sql = "INSERT INTO assessment_component (section_id, name, weight, max_score, published, created_at) "
                         + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sectionId);
            ps.setString(2, name);
            if (weight == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, weight);
            if (maxScore == null) ps.setDouble(4, 100.0); else ps.setDouble(4, maxScore);
            ps.setBoolean(5, published);
            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) return g.getLong(1);
            }
        }

        return -1L;
    }

    // ------------------------------------------------------------
    // updateAssessmentComponent
    // ------------------------------------------------------------
    @Override
    public boolean updateAssessmentComponent(long assessmentId, Integer weight, Double maxScore, Boolean published) throws SQLException {
        StringBuilder sb = new StringBuilder("UPDATE assessment_component SET ");
        List<Object> params = new ArrayList<>();
        if (weight != null) { sb.append("weight = ?, "); params.add(weight); }
        if (maxScore != null) { sb.append("max_score = ?, "); params.add(maxScore); }
        if (published != null) { sb.append("published = ?, "); params.add(published); }
        if (params.isEmpty()) return false;
        sb.setLength(sb.length() - 2);
        sb.append(" WHERE id = ?");
        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            int idx = 1;
            for (Object p : params) {
                if (p instanceof Integer) ps.setInt(idx++, (Integer) p);
                else if (p instanceof Double) ps.setDouble(idx++, (Double) p);
                else if (p instanceof Boolean) ps.setBoolean(idx++, (Boolean) p);
                else ps.setObject(idx++, p);
            }
            ps.setLong(idx, assessmentId);
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------
    // getGradeDetails (used by GradesPanel)
    // ------------------------------------------------------------
    @Override
    public List<Map<String, Object>> getGradeDetails(String studentId) throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();

        String sql = ""
            + "SELECT g.enrollment_id, c.code AS course_code, c.title AS course_name, "
            + "       g.component AS component_name, g.score, g.max_score, g.weight, g.final_grade "
            + "FROM grades g "
            + "JOIN enrollments e ON g.enrollment_id = e.enrollment_id "
            + "JOIN sections s ON e.section_id = s.section_id "
            + "JOIN courses c ON s.course_id = c.course_id "
            + "WHERE e.student_id = ? "
            + "  AND e.status IN ('ENROLLED','COMPLETED') "
            + "ORDER BY c.code, g.component";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try { ps.setLong(1, Long.parseLong(studentId)); }
            catch (NumberFormatException ex) { ps.setString(1, studentId); }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("enrollment_id", rs.getLong("enrollment_id"));
                    row.put("course_code", rs.getString("course_code"));
                    row.put("course_name", rs.getString("course_name"));
                    row.put("component_name", rs.getString("component_name"));
                    row.put("score", rs.getObject("score") == null ? null : rs.getDouble("score"));
                    row.put("max_score", rs.getObject("max_score") == null ? null : rs.getDouble("max_score"));
                    row.put("weight", rs.getObject("weight") == null ? null : rs.getDouble("weight"));
                    row.put("final_grade", rs.getString("final_grade"));
                    out.add(row);
                }
            }
        }

        return out;
    }

    // ------------------------------------------------------------
    // recomputeFinalAndStore
    // ------------------------------------------------------------
    @Override
    public void recomputeFinalAndStore(long enrollmentId) throws Exception {
        final String sql = ""
            + "SELECT "
            + "  SUM((COALESCE(score,0) / NULLIF(COALESCE(max_score,0),0)) * COALESCE(weight,0)) AS weighted_sum, "
            + "  SUM(COALESCE(weight,0)) AS weight_sum, "
            + "  SUM(COALESCE(score,0)) AS raw_score_sum, "
            + "  SUM(COALESCE(max_score,0)) AS raw_max_sum "
            + "FROM grades "
            + "WHERE enrollment_id = ? AND component <> 'Final'";

        double weightedSum = 0.0, weightSum = 0.0, rawScore = 0.0, rawMax = 0.0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    weightedSum = rs.getDouble("weighted_sum"); if (rs.wasNull()) weightedSum = 0.0;
                    weightSum = rs.getDouble("weight_sum"); if (rs.wasNull()) weightSum = 0.0;
                    rawScore = rs.getDouble("raw_score_sum"); if (rs.wasNull()) rawScore = 0.0;
                    rawMax = rs.getDouble("raw_max_sum"); if (rs.wasNull()) rawMax = 0.0;
                }
            }
        }

        double percent;
        if (weightSum > 1e-6) percent = (weightedSum / weightSum) * 100.0;
        else if (rawMax > 1e-6) percent = (rawScore / rawMax) * 100.0;
        else percent = 0.0;

        percent = Math.round(percent * 100.0) / 100.0;

        String letter;
        if (percent >= 90.0) letter = "A+";
        else if (percent >= 80.0) letter = "A";
        else if (percent >= 70.0) letter = "B+";
        else if (percent >= 60.0) letter = "B";
        else if (percent >= 50.0) letter = "C";
        else if (percent >= 40.0) letter = "D";
        else letter = "F";

        createOrUpdateFinalRow(enrollmentId, percent, letter);
    }
}
