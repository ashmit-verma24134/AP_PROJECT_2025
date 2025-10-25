package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

@Override
public boolean createEnrollment(long studentId, long sectionId) throws SQLException {
    String sql = "INSERT INTO enrollments (student_id, section_id, status) VALUES (?, ?, 'ENROLLED')";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setLong(1, studentId);
        ps.setLong(2, sectionId);
        return ps.executeUpdate() > 0;
    } catch (SQLException ex) {
        // MySQL FK violation error code is 1452, SQLState = "23000"
        if ("23000".equals(ex.getSQLState()) || ex.getErrorCode() == 1452) {
            throw new SQLException("Student or section does not exist (foreign key).", ex);
        }
        throw ex;
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
