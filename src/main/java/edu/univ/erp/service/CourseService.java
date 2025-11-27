package edu.univ.erp.service;

import java.util.List;

import edu.univ.erp.model.Course;

/**
 * Service interface for course editor screen.
 * Contains small nested DTOs so UI can import them directly.
 */
public interface CourseService {

    class CourseResult {
        public long courseId;
        public String code;
        public String title;
        public String credits;
        public List<SectionRow> sections;
    }

    class SectionRow {
        public Long sectionId;       // nullable for new rows
        public String sectionCode;
        public String dayTime;
        public String room;
        public Integer capacity;
        public String semester;
        public Integer year;
        public Long instructorId;    // optional

        public SectionRow() {}
    }

    class InstructorItem {
        public Long id;
        public String label;
        public InstructorItem(Long id, String label) { this.id = id; this.label = label; }
    }

    /**
     * Load course metadata and its sections.
     */
    CourseResult loadCourseWithSections(long courseId) throws Exception;

    /**
     * Save basic course fields (code/title/credits).
     */
    void saveCourseBasic(long courseId, String code, String title, String credits) throws Exception;

    /**
     * Create a new section for a course. Returns generated section id (or -1).
     */
    long createSection(long courseId, SectionRow row) throws Exception;

    /**
     * Update an existing section. Returns true if updated.
     */
    boolean updateSection(long sectionId, SectionRow row) throws Exception;

    /**
     * Delete section.
     */
    boolean deleteSection(long sectionId) throws Exception;

    /**
     * List available instructors (id + label).
     */
    List<InstructorItem> listInstructors() throws Exception;

    List<Course> listAllCourses() throws ServiceException;
}