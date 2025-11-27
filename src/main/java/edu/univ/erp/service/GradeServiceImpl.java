package edu.univ.erp.service;

import edu.univ.erp.data.GradeDao;
import edu.univ.erp.model.Grade;
import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeServiceImpl implements GradeService {

    private final GradeDao dao;

    public GradeServiceImpl(GradeDao dao) {
        this.dao = dao;
    }

    @Override
    public List<Grade> listGradesForSection(long sectionId) {
        return dao.findBySection(sectionId);
    }

    @Override
    public void updateScoreAndRecompute(long enrollmentId,
                                        String component,
                                        Double score,
                                        Double maxScore,
                                        Double weight,
                                        long studentId) throws SQLException {

        try (Connection conn = DBConnection.getErpConnection()) {
            boolean oldAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {

                // UPDATE first
                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE grades SET score = ?, max_score = ?, weight = ? " +
                                "WHERE enrollment_id = ? AND component = ?")) {

                    if (score != null) psUpd.setBigDecimal(1, new java.math.BigDecimal(score));
                    else psUpd.setNull(1, Types.DECIMAL);

                    if (maxScore != null) psUpd.setBigDecimal(2, new java.math.BigDecimal(maxScore));
                    else psUpd.setNull(2, Types.DECIMAL);

                    if (weight != null) psUpd.setBigDecimal(3, new java.math.BigDecimal(weight));
                    else psUpd.setNull(3, Types.DECIMAL);

                    psUpd.setLong(4, enrollmentId);
                    psUpd.setString(5, component);

                    int updated = psUpd.executeUpdate();

                    // INSERT if missing
                    if (updated == 0) {
                        try (PreparedStatement psIns = conn.prepareStatement(
                                "INSERT INTO grades " +
                                        "(enrollment_id, component, score, max_score, weight, final_grade, created_at) " +
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

                // Guarantee Final rows
                try (PreparedStatement psInsertFinal = conn.prepareStatement(
                        "INSERT INTO grades (enrollment_id, component, score, max_score, weight, final_grade, created_at) " +
                                "SELECT e.enrollment_id, 'Final', NULL, NULL, NULL, NULL, NOW() " +
                                "FROM enrollments e LEFT JOIN grades g " +
                                "ON g.enrollment_id = e.enrollment_id AND g.component = 'Final' " +
                                "WHERE e.student_id = ? AND g.grade_id IS NULL")) {
                    psInsertFinal.setLong(1, studentId);
                    psInsertFinal.executeUpdate();
                }

                // Recompute finals
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

    private void recomputeFinalsForStudent(Connection conn, long studentId) throws SQLException {

        // FETCH enrollments
        List<Long> enrollmentIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT enrollment_id FROM enrollments WHERE student_id = ?")) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) enrollmentIds.add(rs.getLong(1));
            }
        }
        if (enrollmentIds.isEmpty()) return;

        // For each enrollment -> compute final
        String sel = "SELECT component, score, max_score, weight FROM grades " +
                "WHERE enrollment_id = ? AND component <> 'Final'";

        try (PreparedStatement psComp = conn.prepareStatement(sel)) {
            for (Long enrollmentId : enrollmentIds) {

                psComp.setLong(1, enrollmentId);

                double weightedNum = 0;
                double weightedDen = 0;
                double sumScore = 0;
                double sumMax = 0;
                int scored = 0;

                try (ResultSet rs = psComp.executeQuery()) {
                    while (rs.next()) {

                        Double score = rs.getObject("score") == null ? null : rs.getDouble("score");
                        Double max = rs.getObject("max_score") == null ? null : rs.getDouble("max_score");
                        Double weight = rs.getObject("weight") == null ? null : rs.getDouble("weight");

                        if (score != null) scored++;

                        if (score != null && max != null && max > 0 && weight != null && weight > 0) {
                            weightedNum += (score / max) * weight;
                            weightedDen += weight;
                        } else if (score != null && max != null && max > 0) {
                            sumScore += score;
                            sumMax += max;
                        }
                    }
                }

                Double pct;
                if (weightedDen > 0) pct = (weightedNum / weightedDen) * 100.0;
                else if (sumMax > 0) pct = (sumScore / sumMax) * 100.0;
                else pct = null;

                if (scored == 0) {
                    try (PreparedStatement psClear = conn.prepareStatement(
                            "UPDATE grades SET final_grade = NULL, computed_at = NULL " +
                                    "WHERE enrollment_id = ? AND component = 'Final'")) {
                        psClear.setLong(1, enrollmentId);
                        psClear.executeUpdate();
                    }
                } else {
                    String letter = pct == null ? null : pctToLetter(pct);
                    try (PreparedStatement psUpd = conn.prepareStatement(
                            "UPDATE grades SET final_grade = ?, computed_at = NOW() " +
                                    "WHERE enrollment_id = ? AND component = 'Final'")) {
                        if (letter == null) psUpd.setNull(1, Types.VARCHAR);
                        else psUpd.setString(1, letter);

                        psUpd.setLong(2, enrollmentId);
                        psUpd.executeUpdate();
                    }
                }
            }
        }
    }

    private String pctToLetter(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 85) return "A";
        if (pct >= 75) return "B";
        if (pct >= 65) return "C";
        if (pct >= 55) return "D";
        return "F";
    }
}
