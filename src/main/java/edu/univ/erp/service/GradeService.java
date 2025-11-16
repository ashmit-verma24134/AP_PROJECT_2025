package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GradeService:
 * - updateScoreAndRecompute(...) updates/creates a grade component row and recomputes finals for a student
 * - recomputeFinalsForStudent(Connection, long) recomputes "Final" grade rows for all enrollments of a given student
 *
 * Drop this in package edu.univ.erp.service
 */
public class GradeService {

    /**
     * Update (or insert) a grade component for enrollmentId, then recompute finals for the student.
     * Runs everything in a single transaction.
     *
     * @param enrollmentId DB enrollment_id
     * @param component    component name (e.g., "Quiz 1", "Midterm", "End-Sem", "Final")
     * @param score        nullable achieved score (Double)
     * @param maxScore     nullable max score (Double)
     * @param weight       nullable weight percentage (Double) - use 10.0,30.0,60.0 etc
     * @param studentId    numeric student id (DB id) this enrollment belongs to (used to recompute only for that student)
     * @throws SQLException on DB error
     */
    public void updateScoreAndRecompute(long enrollmentId, String component,
                                        Double score, Double maxScore, Double weight,
                                        long studentId) throws SQLException {
        try (Connection conn = DBConnection.getErpConnection()) {
            boolean oldAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // Try update first
                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE grades SET score = ?, max_score = ?, weight = ? WHERE enrollment_id = ? AND component = ?")) {
                    if (score != null) psUpd.setBigDecimal(1, new java.math.BigDecimal(score));
                    else psUpd.setNull(1, Types.DECIMAL);
                    if (maxScore != null) psUpd.setBigDecimal(2, new java.math.BigDecimal(maxScore));
                    else psUpd.setNull(2, Types.DECIMAL);
                    if (weight != null) psUpd.setBigDecimal(3, new java.math.BigDecimal(weight));
                    else psUpd.setNull(3, Types.DECIMAL);
                    psUpd.setLong(4, enrollmentId);
                    psUpd.setString(5, component);
                    int updated = psUpd.executeUpdate();
                    if (updated == 0) {
                        // insert
                        try (PreparedStatement psIns = conn.prepareStatement(
                                "INSERT INTO grades (enrollment_id, component, score, max_score, weight, final_grade, created_at) " +
                                        "VALUES (?, ?, ?, ?, ?, NULL, NOW())")) {
                            psIns.setLong(1, enrollmentId);
                            psIns.setString(2, component);
                            if (score != null) psIns.setBigDecimal(3, new java.math.BigDecimal(score));
                            else psIns.setNull(3, Types.DECIMAL);
                            if (maxScore != null) psIns.setBigDecimal(4, new java.math.BigDecimal(maxScore));
                            else psIns.setNull(4, Types.DECIMAL);
                            if (weight != null) psIns.setBigDecimal(5, new java.math.BigDecimal(weight));
                            else psIns.setNull(5, Types.DECIMAL);
                            psIns.executeUpdate();
                        }
                    }
                }

                // Ensure at least one Final row exists for every enrollment of this student (we only care about this student's enrollments)
                try (PreparedStatement psInsertFinal = conn.prepareStatement(
                        "INSERT INTO grades (enrollment_id, component, score, max_score, weight, final_grade, created_at) " +
                                "SELECT e.enrollment_id, 'Final', NULL, NULL, NULL, NULL, NOW() " +
                                "FROM enrollments e LEFT JOIN grades g ON g.enrollment_id = e.enrollment_id AND g.component = 'Final' " +
                                "WHERE e.student_id = ? AND g.grade_id IS NULL")) {
                    psInsertFinal.setLong(1, studentId);
                    psInsertFinal.executeUpdate();
                }

                // Recompute final letter grades for this student's enrollments
                recomputeFinalsForStudent(conn, studentId);

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(oldAuto);
            }
        }
    }

    /**
     * Recompute 'Final' grade rows for all enrollments of studentId (works inside caller-managed transaction OR standalone).
     * Uses the same algorithm we discussed:
     * - If there are scored components with valid max_score and weights > 0 -> compute weighted normalized percentage.
     * - Else if there are scored components with max_score -> compute sum(score)/sum(max_score) * 100.
     * - If an enrollment has zero scored components then final stays NULL (or cleared).
     *
     * @param conn      open Connection (can be same transaction)
     * @param studentId student id
     * @throws SQLException
     */
    public void recomputeFinalsForStudent(Connection conn, long studentId) throws SQLException {
        // 1) Get list of enrollments for this student
        List<Long> enrollmentIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT enrollment_id FROM enrollments WHERE student_id = ?")) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) enrollmentIds.add(rs.getLong(1));
            }
        }

        if (enrollmentIds.isEmpty()) return;

        // 2) For each enrollment, fetch component rows (except Final) and compute percent
        String sel = "SELECT component, score, max_score, weight FROM grades WHERE enrollment_id = ? AND component <> 'Final'";
        try (PreparedStatement psComp = conn.prepareStatement(sel)) {
            for (Long enrollmentId : enrollmentIds) {
                psComp.setLong(1, enrollmentId);
                try (ResultSet rs = psComp.executeQuery()) {
                    double sumWeightedNumerator = 0.0;
                    double sumWeightedDenom = 0.0; // sum of weights for scored components that have valid max_score>0
                    double sumScore = 0.0;
                    double sumMax = 0.0;
                    int scoredCount = 0;

                    while (rs.next()) {
                        Double score = rs.getObject("score") == null ? null : rs.getDouble("score");
                        Double maxScore = rs.getObject("max_score") == null ? null : rs.getDouble("max_score");
                        Double weight = rs.getObject("weight") == null ? null : rs.getDouble("weight");

                        if (score != null) scoredCount++;

                        if (score != null && maxScore != null && maxScore > 0 && weight != null && weight > 0) {
                            // use weight contribution: (score/max) * weight
                            sumWeightedNumerator += (score / maxScore) * weight;
                            sumWeightedDenom += weight;
                        } else if (score != null && maxScore != null && maxScore > 0) {
                            // will be used by fallback
                            sumScore += score;
                            sumMax += maxScore;
                        }
                    }

                    Double pct = null;
                    if (sumWeightedDenom > 0) {
                        pct = (sumWeightedNumerator / sumWeightedDenom) * 100.0;
                    } else if (sumMax > 0) {
                        pct = (sumScore / sumMax) * 100.0;
                    } else {
                        pct = null;
                    }

                    // If there were zero scored components, clear final (NULL)
                    if (scoredCount == 0) {
                        try (PreparedStatement psClear = conn.prepareStatement(
                                "UPDATE grades SET final_grade = NULL, computed_at = NULL WHERE enrollment_id = ? AND component = 'Final'")) {
                            psClear.setLong(1, enrollmentId);
                            psClear.executeUpdate();
                        }
                    } else {
                        // Map pct -> letter
                        String letter = pct == null ? null : pctToLetter(pct);
                        try (PreparedStatement psUpdFinal = conn.prepareStatement(
                                "UPDATE grades SET final_grade = ?, computed_at = NOW() WHERE enrollment_id = ? AND component = 'Final'")) {
                            if (letter == null) psUpdFinal.setNull(1, Types.VARCHAR);
                            else psUpdFinal.setString(1, letter);
                            psUpdFinal.setLong(2, enrollmentId);
                            psUpdFinal.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    private String pctToLetter(double pct) {
        // your mapping (modify thresholds if needed)
        if (pct >= 90.0) return "A+";
        if (pct >= 85.0) return "A";
        if (pct >= 75.0) return "B";
        if (pct >= 65.0) return "C";
        if (pct >= 55.0) return "D";
        return "F";
    }
}
