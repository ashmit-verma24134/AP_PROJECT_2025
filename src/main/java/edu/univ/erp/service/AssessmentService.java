package edu.univ.erp.service;

import java.util.List;

/**
 * Service for fetching assessment components for an enrollment.
 */
public interface AssessmentService {
    /**
     * Return assessment components for the given enrollment id.
     */
    List<AssessmentComponent> getComponents(long enrollmentId) throws Exception;
}