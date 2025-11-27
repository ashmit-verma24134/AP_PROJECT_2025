package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.sql.SQLException;
//import edu.univ.erp.model.SectionRow;

import java.util.List;

import edu.univ.erp.util.DBConnection;

/**
 * JDBC-based implementation of EnrollmentDao.
 * Assumes table structure:
 * enrollments(student_id, section_id, status, enrolled_at, updated_at)
 * status ∈ {'ENROLLED', 'DROPPED'}
 */
public class EnrollmentDaoImpl implements EnrollmentDao {

    private final Connection conn;

    public EnrollmentDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean isStudentEnrolled(long studentId, long sectionId) throws Exception {
        String sql = """
            SELECT 1 
            FROM enrollments 
            WHERE student_id = ? AND section_id = ? AND status = 'ENROLLED' 
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Create a new enrollment and copy grade components.
     *
     * Before inserting, check for an existing enrollment row:
     *  - if status = 'ENROLLED' -> throw SQLException("ALREADY_ENROLLED")
     *  - if status = 'DROPPED'  -> throw SQLException("PREVIOUSLY_DROPPED")
     *
     * The caller (UI) should catch SQLException and inspect the message to show
     * friendly messages to the user.
     *
     * Note: method uses the injected `conn` so that transaction boundaries are
     * controlled by this DAO (auto-commit is toggled here).
     */
    @Override
    public long createEnrollment(long studentId, long sectionId) throws SQLException {
        // 1) check existing enrollment for this student+section
        final String checkSql = "SELECT status FROM enrollments WHERE student_id = ? AND section_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    if (status != null) status = status.trim().toUpperCase();
                    else status = "";

                    if ("ENROLLED".equals(status)) {
                        throw new SQLException("ALREADY_ENROLLED");
                    } else if ("DROPPED".equals(status) || "WITHDRAWN".equals(status)) {
                        throw new SQLException("PREVIOUSLY_DROPPED");
                    } else {
                        // Unknown status — refuse to insert to avoid duplicate/inconsistent rows.
                        throw new SQLException("EXISTING_ENROLLMENT_IN_UNKNOWN_STATE:" + status);
                    }
                }
            }
        } catch (SQLException ex) {
            // rethrow known cases or unknown check failure
            if ("ALREADY_ENROLLED".equals(ex.getMessage()) || "PREVIOUSLY_DROPPED".equals(ex.getMessage())) {
                throw ex;
            } else {
                // wrap and propagate
                throw new SQLException("Error checking existing enrollment: " + ex.getMessage(), ex);
            }
        }

        // 2) insert new enrollment and create grade components in same transaction
        final String insertSql = "INSERT INTO enrollments (student_id, section_id, status, enrolled_at, updated_at) "
                               + "VALUES (?, ?, 'ENROLLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        boolean previousAutoCommit = true;
        PreparedStatement psInsert = null;
        ResultSet keys = null;

        try {
            // begin transaction on the provided connection
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            psInsert.setLong(1, studentId);
            psInsert.setLong(2, sectionId);
            int rows = psInsert.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Failed to insert enrollment, no rows affected.");
            }

            keys = psInsert.getGeneratedKeys();
            if (!keys.next()) {
                throw new SQLException("Failed to obtain generated enrollment_id.");
            }
            long newEnrollmentId = keys.getLong(1);

            // Use same connection for GradeDaoImpl so copy happens inside same transaction
            GradeDaoImpl gradeDao = new GradeDaoImpl(conn);
            int created = gradeDao.createComponentsForEnrollment(newEnrollmentId, sectionId);
            // (created = number of per-enrollment component rows inserted)

            // commit everything together
            conn.commit();

            return newEnrollmentId;
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException rbe) { rbe.printStackTrace(); }
            // propagate the meaningful exception (ALREADY_ENROLLED/PREVIOUSLY_DROPPED) as-is
            if ("ALREADY_ENROLLED".equals(ex.getMessage()) || "PREVIOUSLY_DROPPED".equals(ex.getMessage())) {
                throw ex;
            }
            throw new SQLException("Error creating enrollment + components: " + ex.getMessage(), ex);
        } finally {
            try { if (keys != null) keys.close(); } catch (SQLException ignore) {}
            try { if (psInsert != null) psInsert.close(); } catch (SQLException ignore) {}
            try {
                conn.setAutoCommit(previousAutoCommit); // restore original auto-commit
            } catch (SQLException ignore) {}
        }
    }



    @Override
    public boolean dropEnrollment(long studentId, long sectionId) throws Exception {
        String sql = """
            UPDATE enrollments 
            SET status = 'DROPPED', updated_at = CURRENT_TIMESTAMP
            WHERE student_id = ? AND section_id = ? AND status = 'ENROLLED'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, sectionId);
            return ps.executeUpdate() > 0;
        }
    }

      // ======================================================
    // NEW METHODS ADDED FOR UI COMPATIBILITY
    // ======================================================

    @Override
    public int countEnrolled(long sectionId) throws Exception {
        String sql = """
            SELECT COUNT(*) 
            FROM enrollments
            WHERE section_id = ? AND status='ENROLLED'
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private SectionRow mapRow(ResultSet rs) throws Exception {
        return new SectionRow(
                rs.getLong("section_id"),
                rs.getLong("course_id"),
                rs.getString("code"),
                rs.getString("title"),
                rs.getDouble("credits"),
                rs.getLong("instructor_id"),
                rs.getString("instructor"),
                rs.getInt("capacity"),
                rs.getInt("seats_left"),
                rs.getString("semester")
        );
    }

    @Override
    public List<SectionRow> getEnrolledSections(long studentId, String term) throws Exception {
        List<SectionRow> list = new ArrayList<>();

        String sql = """
            SELECT s.section_id, s.course_id, c.code, c.title, c.credits,
                   s.instructor_id, COALESCE(i.full_name, 'TBA') AS instructor,
                   s.capacity,
                   (s.capacity - (
                      SELECT COUNT(*) FROM enrollments e WHERE e.section_id=s.section_id AND e.status='ENROLLED'
                   )) AS seats_left,
                   s.semester
            FROM enrollments e2
            JOIN sections s ON e2.section_id = s.section_id
            JOIN courses c ON s.course_id = c.course_id
            LEFT JOIN instructors i ON s.instructor_id=i.instructor_id
            WHERE e2.student_id = ? 
              AND e2.status='ENROLLED'
              AND (? IS NULL OR s.semester=?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setString(2, term);
            ps.setString(3, term);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<SectionRow> getInstructorSections(long instructorId, String term) throws Exception {
        List<SectionRow> list = new ArrayList<>();

        String sql = """
            SELECT s.section_id, s.course_id, c.code, c.title, c.credits,
                   s.instructor_id, COALESCE(i.full_name, 'TBA') AS instructor,
                   s.capacity,
                   (s.capacity - (
                      SELECT COUNT(*) FROM enrollments e WHERE e.section_id=s.section_id AND e.status='ENROLLED'
                   )) AS seats_left,
                   s.semester
            FROM sections s
            JOIN courses c ON s.course_id=c.course_id
            LEFT JOIN instructors i ON i.instructor_id=s.instructor_id
            WHERE s.instructor_id=? 
              AND (? IS NULL OR s.semester=?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, instructorId);
            ps.setString(2, term);
            ps.setString(3, term);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

}
