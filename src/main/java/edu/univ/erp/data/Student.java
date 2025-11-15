package edu.univ.erp.data;

public class Student {
    private long id;
    private String rollNo;
    private String fullName;
    private String program;
    private Integer year;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
}
