package edu.univ.erp.service;

import java.util.List;
import java.util.Map;

/**
 * Service that loads student grades grouped by semester.
 * Exposes a nested CourseRow DTO that UI can import as:
 *   edu.univ.erp.service.StudentGradeService.CourseRow
 */
public interface StudentGradeService {

    /**
     * Loads all grades grouped by semester for a student.
     * Map key = "Semester / Year"
     * Value = list of CourseRow DTOs.
     */
    Map<String, List<CourseRow>> loadGradesForStudent(String studentId) throws Exception;

    /**
     * Lightweight DTO representing a single course row in a semester.
     * Kept as a static nested class so UI can reference StudentGradeService.CourseRow.
     */
    public static class CourseRow {
        public long enrollmentId;
        public String courseCode;
        public String courseTitle;
        public Integer credits;
        public String finalLetter;
        public Double finalScore;

        public CourseRow() {}

        // Optional: convenience constructor
        public CourseRow(long enrollmentId, String courseCode, String courseTitle,
                         Integer credits, String finalLetter, Double finalScore) {
            this.enrollmentId = enrollmentId;
            this.courseCode = courseCode;
            this.courseTitle = courseTitle;
            this.credits = credits;
            this.finalLetter = finalLetter;
            this.finalScore = finalScore;
        }
    }
}