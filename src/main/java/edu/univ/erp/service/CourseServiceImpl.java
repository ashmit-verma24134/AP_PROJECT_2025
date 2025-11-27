package edu.univ.erp.service;

import edu.univ.erp.data.CourseDao;
import edu.univ.erp.model.Course;
import edu.univ.erp.service.CourseService;

import java.util.List;
import java.util.Optional;

public class CourseServiceImpl implements CourseService {
    private final CourseDao dao;

    public CourseServiceImpl(CourseDao dao) {
        this.dao = dao;
    }

    @Override
    public List<Course> listAllCourses() {
        return dao.findAll();
    }

    @Override
    public Optional<Course> findById(long id) {
        return dao.findById(id);
    }

    @Override
    public void addCourse(String code, String title, Double credits) {
        dao.insert(code, title, credits);
    }

    @Override
    public void updateCourse(Course course) {
        dao.update(course);
    }

    @Override
    public void deleteCourse(long id) {
        dao.delete(id);
    }
}
