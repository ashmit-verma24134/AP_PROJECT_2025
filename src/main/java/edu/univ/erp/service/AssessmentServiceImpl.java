package edu.univ.erp.service;

import edu.univ.erp.util.DBConnection;

import java.sql.Connection;
import java.util.List;

/**
 * Default implementation that delegates to AssessmentDao.
 * Assumes you have AssessmentDaoImpl(Connection) or adjust if signature differs.
 */
public class AssessmentServiceImpl implements AssessmentService {

    public AssessmentServiceImpl() {
        // no-op ctor; DAO/connection created per-call
    }

    @Override
    public List<AssessmentComponent> getComponents(long enrollmentId) throws Exception {
        try (Connection conn = DBConnection.getErpConnection()) {
            // If your DAO has a different constructor, change this line accordingly.
            AssessmentDao dao = new AssessmentDaoImpl(conn);
            return dao.getComponentsByEnrollment(enrollmentId);
        }
    }
}