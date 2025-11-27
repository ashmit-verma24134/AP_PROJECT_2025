package edu.univ.erp.service;

import edu.univ.erp.data.SectionRow;
import edu.univ.erp.model.Section;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectionService {
    // high-level helpers used by UI & other services
    List<SectionRow> searchOpenSections(String query);
    List<SectionRow> getSectionsByInstructor(long instructorId, String term);

    boolean isMaintenanceOn();
    boolean isDropDeadlineOver(long sectionId);
    int getSeatsLeft(long sectionId);
    List<Section> listAllSections();
    List<Section> findFiltered(String courseId, String instructorId);

    // CRUD
    List<Section> listSectionsForCourse(String courseId);
    Optional<Section> findById(long id);
    void addSection(Section s);
    void updateSection(Section s);
    //List<Section> getSectionsByInstructor(long instructorId, String term);

    // drop deadline operations
    LocalDate getDropDeadline(long sectionId) throws Exception;
    boolean updateDropDeadline(long sectionId, LocalDate date) throws Exception;
    boolean clearDropDeadline(long sectionId) throws Exception;

}
