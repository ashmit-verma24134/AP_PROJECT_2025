package edu.univ.erp.data;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DAO interface for grade-related operations used by the UI and services.
 *
 * Make sure the method signatures here match the implementations in GradeDaoImpl.
 */
public interface GradeDao {

    /**
     * Return assessment components for an enrollment (typed POJO).
     * includeScores == true will include student's score (if any) in the returned objects.
     */
    List<AssessmentComponent> findComponentsForEnrollment(long enrollmentId, boolean includeScores) throws Exception;

    /**
     * Upsert a final row for an enrollment (score is percent, letter is final letter grade).
     * Returns generated id or existing id.
     */
    long createOrUpdateFinalRow(long enrollmentId, double percent, String letter) throws Exception;

    /**
     * Upsert a grade row by enrollment+component (used by instructor UI).
     */
    void upsertAssessmentScore(long enrollmentId, String component, Double score, Double maxScore, Double weight) throws Exception;

    /**
     * Alternate upsert that uses assessmentId (for assessment_score table style).
     * Returns true when rows affected.
     */
    boolean upsertAssessmentScore(long assessmentId, long enrollmentId, Double score) throws SQLException;

    /**
     * Create an assessment component (section-level). Returns generated id.
     */
    long createAssessmentComponent(long sectionId, String name, Integer weight, Double maxScore, boolean published) throws SQLException;

    /**
     * Update an existing assessment component (weight/max/published). Returns true if updated.
     */
    boolean updateAssessmentComponent(long assessmentId, Integer weight, Double maxScore, Boolean published) throws SQLException;

    /**
     * Recompute final grade for an enrollment (using published components) and store it in grades/enrollments.
     */
    void recomputeFinalAndStore(long enrollmentId) throws Exception;

    /**
     * Return grade rows used by the UI (map form). Keys: enrollment_id, course_code, course_name,
     * component_name, score, max_score, weight, final_grade
     */
    List<Map<String, Object>> getGradeDetails(String studentId) throws Exception;

    /**
     * Alternate listing used elsewhere (keeps backward compatibility).
     */
    List<Map<String,Object>> getStudentGrades(long studentId) throws SQLException;
}
