package edu.univ.erp.service;

public class StudentSummary {
    private long studentId;
    private String fullName;
    private String rollNo;
    private String program;
    private Integer currentSem; // may be null
    private Double currentCgpa; // may be null

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public Integer getCurrentSem() { return currentSem; }
    public void setCurrentSem(Integer currentSem) { this.currentSem = currentSem; }

    public Double getCurrentCgpa() { return currentCgpa; }
    public void setCurrentCgpa(Double currentCgpa) { this.currentCgpa = currentCgpa; }
}
