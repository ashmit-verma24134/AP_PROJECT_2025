package edu.univ.erp.data;

import java.util.List;

public interface SectionDao {
    List<SectionRow> searchOpenSections(String query) throws Exception;
    boolean isStudentEnrolled(long studentId, long sectionId) throws Exception;
    int getSeatsLeft(long sectionId) throws Exception;
    boolean registerStudentInSection(long studentId, long sectionId) throws Exception;
    boolean isMaintenanceOn() throws Exception;
    boolean isDropDeadlineOver(long sectionId) throws Exception;
}
