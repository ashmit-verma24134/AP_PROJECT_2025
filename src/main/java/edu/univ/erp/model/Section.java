package edu.univ.erp.model;

import java.sql.Timestamp;

public class Section {
    private Long sectionId;
    private Long courseId;
    private Long instructorId;
    private String dayTime;
    private String room;
    private Integer capacity;
    private String semester;
    private Integer year;
    private Timestamp dropDeadline;

    public Section() {}

    // basic ctor
    public Section(Long sectionId, Long courseId, Long instructorId, String dayTime,
                   String room, Integer capacity, String semester, Integer year, Timestamp dropDeadline) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.instructorId = instructorId;
        this.dayTime = dayTime;
        this.room = room;
        this.capacity = capacity;
        this.semester = semester;
        this.year = year;
        this.dropDeadline = dropDeadline;
    }

    // lightweight constructor used in DAO (sectionId, courseId, instructorId, capacity, semester)
public Section(Long sectionId, Long courseId, Long instructorId,
               Integer capacity, String semester) {
    this.sectionId = sectionId;
    this.courseId = courseId;
    this.instructorId = instructorId;
    this.capacity = capacity;
    this.semester = semester;

    // other fields remain null
    this.dayTime = null;
    this.room = null;
    this.year = null;
    this.dropDeadline = null;
}

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getInstructorId() { return instructorId; }
    public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }

    public String getDayTime() { return dayTime; }
    public void setDayTime(String dayTime) { this.dayTime = dayTime; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public java.sql.Timestamp getDropDeadline() { return dropDeadline; }
    public void setDropDeadline(java.sql.Timestamp dropDeadline) { this.dropDeadline = dropDeadline; }

    @Override
    public String toString() {
        return "Section{" + sectionId + ", course=" + courseId + ", instr=" + instructorId + "}";
    }
}
