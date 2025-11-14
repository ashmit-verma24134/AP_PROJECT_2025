package edu.univ.erp.data;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface StudentDao {
    Long getStudentIdByRoll(String rollNo) throws Exception;

    /**
     * Overview keys:
     * - "enrolled_count" -> Integer
     * - "total_credits"  -> Double
     * - "cgpa"           -> Double or null
     * - "attendance_percent" -> Double or null
     * - "pending_fees"   -> Double
     */
    Map<String, Object> getStudentOverview(String studentId) throws Exception;
List<Map<String,Object>> getStudentSchedule(String studentId) throws SQLException;

    
    /**
     * Return current/previous courses for this student.
     * Each map contains: course_id, course_code, course_name, instructor, schedule, credits, status, section_id
     */
    List<Map<String, Object>> getCurrentCourses(String studentId, String searchQuery) throws Exception;

    List<Map<String,Object>> getUpcomingSchedule(String studentId, int limit) throws Exception;
List<Map<String,Object>> getRecentGrades(String studentId, int limit) throws Exception;
List<Map<String, Object>> getStudentTimetable(String studentId) throws Exception;


    /**
     * Compute overall attendance percent across enrollments which have attendance rows.
     * Returns a number in range [0..100], rounded to 2 decimals. Returns null if no attendance data.
     */
    Double getAttendancePercentage(String studentId) throws Exception;
}
