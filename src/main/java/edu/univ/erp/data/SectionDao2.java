package edu.univ.erp.data;

import java.util.List;
import java.util.Map;

public interface SectionDao2 {
    long createSection(long courseId, String days, String start, String end,
                       String room, int capacity, String sem, int year, Long instructorId) throws Exception;

    void updateSection(long id, long courseId, String days, String start, String end,
                       String room, int capacity, String sem, int year, Long instructorId) throws Exception;

    List<Map<String,Object>> listSectionsForCourse(long courseId) throws Exception;
}
