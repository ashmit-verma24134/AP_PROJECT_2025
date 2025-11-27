package edu.univ.erp.service;

import java.util.List;

/**
 * DAO for fetching assessment components/results for an enrollment.
 */
public interface AssessmentDao {
    /**
     * Return assessment components and any student-specific scores for the given enrollment.
     */
    List<AssessmentComponent> getComponentsByEnrollment(long enrollmentId) throws Exception;
}