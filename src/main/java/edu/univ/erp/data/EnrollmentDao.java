package edu.univ.erp.data;

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
    boolean createEnrollment(long studentId, long sectionId) throws Exception;

    /**
     * Mark an existing enrollment as dropped.
     */
    boolean dropEnrollment(long studentId, long sectionId) throws Exception;
}
