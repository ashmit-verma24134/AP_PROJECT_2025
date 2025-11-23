package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.sql.SQLException;

import edu.univ.erp.util.DBConnection;

/**
 * JDBC-based implementation of EnrollmentDao.
 * Assumes table structure:
 * enrollments(student_id, section_id, status)
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

    

// EnrollmentDaoImpl.java (inside class EnrollmentDaoImpl)
@Override
public long createEnrollment(long studentId, long sectionId) throws SQLException {
    final String insertSql = "INSERT INTO enrollments (student_id, section_id, status, enrolled_at, updated_at) "
                           + "VALUES (?, ?, 'ENROLLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet keys = null;
    boolean previousAutoCommit = true;

    try {
        conn = DBConnection.getErpConnection();

        // ensure single-transaction for insert + component-copy
        previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, studentId);
        ps.setLong(2, sectionId);
        int rows = ps.executeUpdate();
        if (rows == 0) {
            throw new SQLException("Failed to insert enrollment, no rows affected.");
        }

        keys = ps.getGeneratedKeys();
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
    } catch (Exception ex) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException rbe) { rbe.printStackTrace(); }
        }
        throw new SQLException("Error creating enrollment + components: " + ex.getMessage(), ex);
    } finally {
        try { if (keys != null) keys.close(); } catch (SQLException ignore) {}
        try { if (ps != null) ps.close(); } catch (SQLException ignore) {}
        if (conn != null) {
            try {
                conn.setAutoCommit(previousAutoCommit); // restore original auto-commit
                conn.close();
            } catch (SQLException ignore) {}
        }
    }
}



    @Override
    public boolean dropEnrollment(long studentId, long sectionId) throws Exception {
        String sql = """
            UPDATE enrollments 
            SET status = 'DROPPED' 
            WHERE student_id = ? AND section_id = ? AND status = 'ENROLLED'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, sectionId);
            return ps.executeUpdate() > 0;
        }
    }
}
