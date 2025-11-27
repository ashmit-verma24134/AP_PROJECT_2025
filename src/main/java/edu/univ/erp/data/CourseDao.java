package edu.univ.erp.data;

import edu.univ.erp.model.Course;
import java.util.List;
import java.util.Optional;

public interface CourseDao {
    List<Course> findAll();
    Optional<Course> findById(long id);
    void insert(String code, String title, Double credits);
    void update(Course course);
    void delete(long id);
}
