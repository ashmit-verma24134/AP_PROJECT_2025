package edu.univ.erp.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Default JDBC-based implementation.
 *
 * NOTE: This SQL assumes you have two tables:
 * - assessment_components (component_id, name, weight, max_score, published, component_order, ...)
 * - assessment_results    (result_id, enrollment_id, component_id, score, ...)
 *
 * If your schema uses other column/table names, adjust the SQL column names accordingly.
 */
public class AssessmentDaoImpl implements AssessmentDao {

    private final Connection conn;

    public AssessmentDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<AssessmentComponent> getComponentsByEnrollment(long enrollmentId) throws Exception {
        final String sql = 
            "SELECT ac.component_id, ac.name AS component_name, ac.weight, ac.max_score, "
          + "       COALESCE(ar.score, NULL) AS student_score, COALESCE(ac.published, 0) AS published "
          + "FROM assessment_components ac "
          + "LEFT JOIN assessment_results ar ON ac.component_id = ar.component_id AND ar.enrollment_id = ? "
          + "ORDER BY COALESCE(ac.component_order, ac.component_id) ASC";

        List<AssessmentComponent> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AssessmentComponent c = new AssessmentComponent();
                    // adapt these getters/setters to your AssessmentComponent class if names differ
                    c.setComponentId(rs.getLong("component_id"));
                    c.setName(rs.getString("component_name"));
                    int weight = rs.getInt("weight");
                    if (rs.wasNull()) c.setWeight(null); else c.setWeight(weight);
                    double mx = rs.getDouble("max_score");
                    if (rs.wasNull()) c.setMaxScore(null); else c.setMaxScore(mx);
                    double sc = rs.getDouble("student_score");
                    if (rs.wasNull()) c.setStudentScore(null); else c.setStudentScore(sc);
                    // published stored as tinyint(0/1) or boolean
                    int pub = rs.getInt("published");
                    if (rs.wasNull()) c.setPublished(null);
                    else c.setPublished(pub != 0);
                    out.add(c);
                }
            }
        }
        return out;
    }
}