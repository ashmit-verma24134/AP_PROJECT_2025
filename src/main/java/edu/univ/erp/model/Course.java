package edu.univ.erp.model;

public class Course {

    private Long courseId;
    private String code;
    private String title;
    private Double credits;

    public Course() {}

    public Course(Long courseId, String code, String title, Double credits) {
        this.courseId = courseId;
        this.code = code;
        this.title = title;
        this.credits = credits;
    }

    // === getters ===
    public Long getCourseId() { return courseId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public Double getCredits() { return credits; }

    // === setters ===
    public void setCourseId(Long id) { this.courseId = id; }
    public void setCode(String code) { this.code = code; }
    public void setTitle(String title) { this.title = title; }
    public void setCredits(Double credits) { this.credits = credits; }
}
