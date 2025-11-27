package edu.univ.erp.service;

import edu.univ.erp.data.AssessmentComponent;
import edu.univ.erp.data.GradeDao;
import edu.univ.erp.data.GradeDaoImpl;
import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.util.List;

/**
 * AssessmentService - abstraction over GradeDao
 * Loads all assessment components for an enrollment.
 */
public class AssessmentService {

    /**
     * Returns list of assessment components for the enrollment.
     * Includes unpublished components.
     */
    public List<AssessmentComponent> getComponents(long enrollmentId) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            GradeDao dao = new GradeDaoImpl(conn);
            return dao.findComponentsForEnrollment(enrollmentId, true);
        }
    }
}
