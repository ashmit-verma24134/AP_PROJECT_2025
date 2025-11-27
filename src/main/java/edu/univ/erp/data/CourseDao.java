package edu.univ.erp.data;

import edu.univ.erp.model.Course;
import java.util.List;

public interface CourseDao {
    /**
     * Return list of courses (basic fields).
     */
    List<Course> listAll() throws Exception;
}