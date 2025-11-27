package edu.univ.erp.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    /** Returns CGPA, enrolled count, credits, attendance, pending fees */
    Map<String, Object> loadOverview(String studentId) throws Exception;

    /** Returns list of upcoming classes (course title, room, day_time) */
    List<Map<String, Object>> loadUpcomingSchedule(String studentId, int limit) throws Exception;

    /** Returns recent grades (course title, credits, final_grade) */
    List<Map<String, Object>> loadRecentGrades(String studentId, int limit) throws Exception;

    /** Returns best display name (username, full_name, or roll_no) */
    String loadDisplayName(String studentId) throws Exception;
}
