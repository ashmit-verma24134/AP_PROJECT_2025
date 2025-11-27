package edu.univ.erp.data;

import edu.univ.erp.model.Instructor;
import java.util.List;

/**
 * Admin-level DAO: some implementations in your repo throw checked Exception,
 * so the interface declares throws Exception to match the implementation.
 */
public interface AdminDao {
    List<Instructor> findAllInstructors() throws Exception;
    void deleteCourseCascade(long courseId) throws Exception;
    void deleteSectionCascade(long sectionId) throws Exception;
    int countEnrolled(long sectionId) throws Exception;
}
