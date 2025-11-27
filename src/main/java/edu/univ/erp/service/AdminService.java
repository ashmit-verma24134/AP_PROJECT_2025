package edu.univ.erp.service;

import edu.univ.erp.model.Instructor;

import java.util.List;


public interface AdminService {
    List<Instructor> listAllInstructors();
    void deleteCourseCascade(long courseId);
    void deleteSectionCascade(long sectionId);
    int countEnrolled(long sectionId);
    String getUsernameById(long userId) throws Exception;
     String getAdminUsername(long userId) throws Exception;
}
