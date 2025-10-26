package edu.univ.erp.data;

/**
 * Simple POJO representing a section row returned by SectionDao.
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
    public String sectionNo; // optional if you have one in DB
}
