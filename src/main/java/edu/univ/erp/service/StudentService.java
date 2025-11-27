package edu.univ.erp.service;

import edu.univ.erp.model.GradeDetail;
import java.util.List;
import java.util.Map;

public interface StudentService {

    List<GradeDetail> getGradeDetails(String studentId) throws ServiceException;
    List<Map<String,Object>> getCurrentCourses(String studentId, String query) throws Exception;
Result dropSection(String studentId, long sectionId);


    // Add THIS:

    Result registerForSection(String studentId, long sectionId);
    Map<String, Object> getStudentOverview(String studentId);



    // You can add more later as needed
}