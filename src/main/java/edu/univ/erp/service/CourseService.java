package edu.univ.erp.service;

import edu.univ.erp.model.Course;
import java.util.List;
import java.util.Optional;

public interface CourseService {
    List<Course> listAllCourses();
    Optional<Course> findById(long id);
    void addCourse(String code, String title, Double credits);
    void updateCourse(Course course);
    void deleteCourse(long id);
}
