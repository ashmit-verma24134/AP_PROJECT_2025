package edu.univ.erp.service;

public class SemesterRecord {
    private int semNo;
    private int year;
    private Double credits; // optional - your schema might not store credits per semester
    private Double sgpa;
    private Double cgpa;

    public int getSemNo() { return semNo; }
    public void setSemNo(int semNo) { this.semNo = semNo; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public Double getCredits() { return credits; }
    public void setCredits(Double credits) { this.credits = credits; }

    public Double getSgpa() { return sgpa; }
    public void setSgpa(Double sgpa) { this.sgpa = sgpa; }

    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }

// add these (adjust types/names to your actual fields)
private Integer courseCount;
private Integer registeredCredits;

public Integer getCourseCount() { return courseCount; }
public Integer getRegisteredCredits() { return registeredCredits; }


}
