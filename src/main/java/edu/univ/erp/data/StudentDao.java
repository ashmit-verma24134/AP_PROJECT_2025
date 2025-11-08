package edu.univ.erp.data;

import java.util.List;
import java.util.Map;

public interface StudentDao {
    /**
     * Get internal student ID from roll number.
     */
    Long getStudentIdByRoll(String rollNo) throws Exception;

    /**
     * Get all currently enrolled courses for a given student.
     * @param studentId the student's unique ID
     * @param searchQuery optional text to filter by course code, name, or instructor
     * @return list of course records (each as a Map<String,Object>)
     */
    List<Map<String, Object>> getCurrentCourses(String studentId, String searchQuery);
}


