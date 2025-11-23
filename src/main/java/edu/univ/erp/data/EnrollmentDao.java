package edu.univ.erp.data;

import java.sql.SQLException;

/**
 * DAO for managing student enrollments in course sections.
 */
public interface EnrollmentDao {

    /**
     * Check if a student is already enrolled in a section.
     */
    boolean isStudentEnrolled(long studentId, long sectionId) throws Exception;

    /**
     * Create a new enrollment (student registers for section).
     */
// EnrollmentDao.java
// old: boolean createEnrollment(long studentId, long sectionId) ...
long createEnrollment(long studentId, long sectionId) throws SQLException;

    /**
     * Mark an existing enrollment as dropped.
     */
    boolean dropEnrollment(long studentId, long sectionId) throws Exception;
}
