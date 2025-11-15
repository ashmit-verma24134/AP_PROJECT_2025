package edu.univ.erp.data;

import java.util.List;
import java.util.Map;

public interface CourseDao {
    long createCourse(String code, String title, int credits) throws Exception;
    void updateCourse(long id, String code, String title, int credits) throws Exception;

    List<Map<String,Object>> listCourses() throws Exception;
}
