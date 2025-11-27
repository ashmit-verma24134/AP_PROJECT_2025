package edu.univ.erp.service;

import java.util.List;
import java.util.Map;

public interface StudentGradeService {

    /**
     * Loads all grades grouped by semester for a student.
     * The map key = "Semester / Year"
     * The value = list of CourseRow maps.
     */
    Map<String, List<Map<String, Object>>> loadGradesForStudent(String studentId) throws Exception;

}
