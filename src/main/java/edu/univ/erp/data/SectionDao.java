package edu.univ.erp.data;

import edu.univ.erp.model.Section;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interface matching the richer SectionDaoImpl used across the codebase.
 * Some methods throw SQLException because the implementation interacts with JDBC directly.
 */
public interface SectionDao {

    // student/instructor UI helpers that many callers expect
    List<SectionRow> searchOpenSections(String query) throws SQLException;
    List<SectionRow> getSectionsByInstructor(long instructorId, String term) throws SQLException;

    // status helpers used by services
    boolean isMaintenanceOn() throws SQLException;
    boolean isDropDeadlineOver(long sectionId) throws SQLException;
    int getSeatsLeft(long sectionId) throws SQLException;

    // CRUD
    List<Section> findFiltered(String courseId, String instructorId);
    Optional<Section> findById(long sectionId);
    void insert(Section s);
    void update(Section s);

    // drop deadline operations
    java.time.LocalDate getDropDeadline(long sectionId) throws SQLException;
    boolean updateDropDeadline(long sectionId, java.time.LocalDate date) throws SQLException;
    boolean clearDropDeadline(long sectionId) throws SQLException;
}
