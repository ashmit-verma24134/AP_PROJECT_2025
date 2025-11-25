package edu.univ.erp.data;

import edu.univ.erp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lightweight DAO for enrollment-drop action.
 * Called from MyCoursesPanel when student clicks “Drop”.
 *
 * POLICY:
 *  - If drop_deadline IS NULL → drop is allowed.
 *  - If drop_deadline is a timestamp and NOW > deadline → drop blocked.
 */
public class RegistrationDAO {

    /**
     * Attempt to drop a section (enrollment) for given student.
     * @return true if dropped, false if deadline passed / not enrolled.
     */
    public boolean dropEnrollment(long studentId, long sectionId) throws Exception {

        String lockSql =
                "SELECT e.enrollment_id, e.status, s.drop_deadline " +
                "FROM enrollments e " +
                "JOIN sections s ON e.section_id = s.section_id " +
                "WHERE e.student_id = ? AND e.section_id = ? FOR UPDATE";

        String updateSql =
                "UPDATE enrollments SET status = 'DROPPED', updated_at = NOW() " +
                "WHERE enrollment_id = ? AND status = 'ENROLLED'";

        try (Connection conn = DBConnection.getErpConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setLong(1, studentId);
                ps.setLong(2, sectionId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        conn.rollback();
                        return false; // no such enrollment
                    }

                    long enrollmentId = rs.getLong("enrollment_id");
                    String status = rs.getString("status");

                    Timestamp ddlTs = rs.getTimestamp("drop_deadline");  // may be null

                    if (!"ENROLLED".equalsIgnoreCase(status)) {
                        conn.rollback();
                        return false;
                    }

                    // =============================================
                    //  DEADLINE CHECK (BLOCK AFTER drop_deadline)
                    // =============================================
                    if (ddlTs != null) {
                        LocalDateTime deadline = ddlTs.toLocalDateTime();
                        LocalDateTime now = LocalDateTime.now();

                        if (now.isAfter(deadline)) {
                            conn.rollback();
                            return false; // deadline passed → cannot drop
                        }
                    }
                    // if NULL → allowed by policy

                    // Perform drop
                    try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                        upd.setLong(1, enrollmentId);
                        int updated = upd.executeUpdate();
                        if (updated == 1) {
                            conn.commit();
                            return true;
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
