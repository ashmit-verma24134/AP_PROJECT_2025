package edu.univ.erp.service;

import edu.univ.erp.model.Grade;
import java.util.List;

public interface GradeService {

    /**
     * Returns all grade rows (component grades + final grade rows)
     * for a given section.
     */
    List<Grade> listGradesForSection(long sectionId);

    /**
     * Updates or inserts a score component and recomputes final grade.
     */
    void updateScoreAndRecompute(long enrollmentId,
                                 String component,
                                 Double score,
                                 Double maxScore,
                                 Double weight,
                                 long studentId) throws Exception;
}
