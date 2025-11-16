package edu.univ.erp.data;

import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * Lightweight DAO for enrollment drop action.
 * Uses DBConnection.getErpConnection() for consistency with the rest of your code.
 */
public class RegistrationDAO {

    /**
     * Attempt to drop a section (enrollment) for given student.
     * Returns true if dropped, false if not enrolled or deadline passed.
     *
     * Policy: if drop_deadline is NULL, this method ALLOWS drop.
     * Change behavior if you want NULL to mean "dropping is not allowed".
     */
    public boolean dropEnrollment(long studentId, long sectionId) throws Exception {
        String lockSql = "SELECT e.enrollment_id, e.status, s.drop_deadline " +
                         "FROM enrollments e JOIN sections s ON e.section_id = s.section_id " +
                         "WHERE e.student_id = ? AND e.section_id = ? FOR UPDATE";
        String updateEnrollment = "UPDATE enrollments SET status = 'DROPPED', updated_at = NOW() WHERE enrollment_id = ? AND status = 'ENROLLED'";

        try (Connection conn = DBConnection.getErpConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setLong(1, studentId);
                ps.setLong(2, sectionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false; // no enrollment found
                    }
                    long enrollmentId = rs.getLong("enrollment_id");
                    String status = rs.getString("status");
                    Date dd = rs.getDate("drop_deadline");

                    if (!"ENROLLED".equalsIgnoreCase(status)) {
                        conn.rollback();
                        return false; // not in enrolled state
                    }

                    // enforce deadline: allow drop when today <= deadline
                    LocalDate today = LocalDate.now();
                    if (dd != null) {
                        LocalDate deadline = dd.toLocalDate();
                        if (today.isAfter(deadline)) { // after deadline -> disallow
                            conn.rollback();
                            return false;
                        }
                    }
                    // if dd == null -> policy currently allows drop. Change if you want disallow.

                    try (PreparedStatement upr = conn.prepareStatement(updateEnrollment)) {
                        upr.setLong(1, enrollmentId);
                        int updated = upr.executeUpdate();
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
