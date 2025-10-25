package edu.univ.erp.data;

/**
 * Plain data holder for section rows returned by SectionDao.
 */
public class SectionRow {
    public long sectionId;
    public String courseCode;
    public String title;
    public int credits;
    public String instructorName;
    public int capacity;
    public int seatsLeft;
    public String semester;
    public int year;
    public String dayTime;

    public SectionRow() {}
}
