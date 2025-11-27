package edu.univ.erp.service;

import edu.univ.erp.data.SectionDao;
import edu.univ.erp.data.SectionRow;
import edu.univ.erp.model.Section;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SectionServiceImpl implements SectionService {
    private final SectionDao dao;

    public SectionServiceImpl(SectionDao dao) {
        this.dao = dao;
    }

    @Override
    public List<SectionRow> searchOpenSections(String query) {
        try {
            return dao.searchOpenSections(query);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to search open sections", ex);
        }
    }

    @Override
    public List<SectionRow> getSectionsByInstructor(long instructorId, String term) {
        try {
            return dao.getSectionsByInstructor(instructorId, term);
        } catch (Exception ex) {
            // Prefer to return empty list on failure (original behavior).
            ex.printStackTrace();
            return List.of();
        }
    }

    @Override
    public boolean isMaintenanceOn() {
        try {
            return dao.isMaintenanceOn();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to determine maintenance state", ex);
        }
    }

    @Override
    public boolean isDropDeadlineOver(long sectionId) {
        try {
            return dao.isDropDeadlineOver(sectionId);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check drop deadline", ex);
        }
    }

    @Override
    public int getSeatsLeft(long sectionId) {
        try {
            return dao.getSeatsLeft(sectionId);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to get seats left for section", ex);
        }
    }

    @Override
    public List<Section> listSectionsForCourse(String courseId) {
        return dao.findFiltered(courseId, null);
    }

    @Override
    public Optional<Section> findById(long id) {
        return dao.findById(id);
    }

    @Override
    public void addSection(Section s) {
        dao.insert(s);
    }

    @Override
    public void updateSection(Section s) {
        dao.update(s);
    }

    @Override
    public List<Section> findFiltered(String courseId, String instructorId) {
        return dao.findFiltered(courseId, instructorId);
    }

    @Override
    public List<Section> listAllSections() {
        return dao.findFiltered(null, null);
    }

    @Override
    public LocalDate getDropDeadline(long sectionId) throws Exception {
        return dao.getDropDeadline(sectionId);
    }

    @Override
    public boolean updateDropDeadline(long sectionId, LocalDate date) throws Exception {
        return dao.updateDropDeadline(sectionId, date);
    }

    @Override
    public boolean clearDropDeadline(long sectionId) throws Exception {
        return dao.clearDropDeadline(sectionId);
    }
}