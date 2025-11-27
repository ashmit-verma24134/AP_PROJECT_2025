package edu.univ.erp.service;

import edu.univ.erp.data.SectionRow;

import java.util.List;

public interface EnrollmentService {

    boolean isStudentEnrolled(long studentId, long sectionId) throws Exception;

    long enroll(long studentId, long sectionId) throws Exception;

    boolean drop(long studentId, long sectionId) throws Exception;

    int countEnrolled(long sectionId) throws Exception;

    List<SectionRow> getEnrolledSections(long studentId, String term) throws Exception;

    List<SectionRow> getInstructorSections(long instructorId, String term) throws Exception;
}
