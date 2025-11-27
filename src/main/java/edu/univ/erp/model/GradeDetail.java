package edu.univ.erp.model;

/**
 * Lightweight DTO representing a single grade/detail row.
 * Mirrors keys previously returned as Map<String,Object>.
 */
public class GradeDetail {
    private Object enrollmentId;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private String finalGrade;
    private Double gradePoint;
    private String semester;
    private Integer year;

    public GradeDetail() {}

    // getters + setters
    public Object getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Object enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }

    public String getFinalGrade() { return finalGrade; }
    public void setFinalGrade(String finalGrade) { this.finalGrade = finalGrade; }

    public Double getGradePoint() { return gradePoint; }
    public void setGradePoint(Double gradePoint) { this.gradePoint = gradePoint; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
}