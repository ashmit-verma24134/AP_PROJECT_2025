package edu.univ.erp.data;

import java.sql.Date;
import java.sql.Timestamp;

public class SectionRow {
    public long sectionId;
    public long courseId;
    public String courseCode;
    public String title;
    public int credits;

    public long instructorId;
    public String instructorName;

    public String dayTime;
    public String room;
    public String sectionNo;

    public String semester;
    public int year;

    public int capacity;
    public int seatsLeft;
    public int enrolled;

    public Date dropDeadline;
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
