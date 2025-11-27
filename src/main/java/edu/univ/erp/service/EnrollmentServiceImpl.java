package edu.univ.erp.service;

import edu.univ.erp.data.EnrollmentDao;
import edu.univ.erp.data.SectionRow;

import java.util.List;

public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentDao dao;

    public EnrollmentServiceImpl(EnrollmentDao dao) {
        this.dao = dao;
    }

    @Override
    public boolean isStudentEnrolled(long studentId, long sectionId) throws Exception {
        return dao.isStudentEnrolled(studentId, sectionId);
    }

    @Override
    public long enroll(long studentId, long sectionId) throws Exception {
        return dao.createEnrollment(studentId, sectionId);
    }

    @Override
    public boolean drop(long studentId, long sectionId) throws Exception {
        return dao.dropEnrollment(studentId, sectionId);
    }

    @Override
    public int countEnrolled(long sectionId) throws Exception {
        return dao.countEnrolled(sectionId);
    }

    @Override
    public List<SectionRow> getEnrolledSections(long studentId, String term) throws Exception {
        return dao.getEnrolledSections(studentId, term);
    }

    @Override
    public List<SectionRow> getInstructorSections(long instructorId, String term) throws Exception {
        return dao.getInstructorSections(instructorId, term);
    }
}
