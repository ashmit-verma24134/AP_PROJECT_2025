package edu.univ.erp.model;

import java.sql.Timestamp;

public class Grade {

    private Long gradeId;        // PK of grades table
    private Long enrollmentId;   // FK -> enrollments.enrollment_id
    private Long studentId;      // FK -> students.student_id
    private String studentName;  // joined from students table

    private String component;    // Quiz, Midterm, Final etc.
    private Double score;
    private Double maxScore;
    private Double weight;
    private String finalLetter;

    private Timestamp createdAt;
    private Timestamp computedAt;

    public Grade() {}

    // =============================
    //        GETTERS/SETTERS
    // =============================

    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }

    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getFinalLetter() { return finalLetter; }
    public void setFinalLetter(String finalLetter) { this.finalLetter = finalLetter; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getComputedAt() { return computedAt; }
    public void setComputedAt(Timestamp computedAt) { this.computedAt = computedAt; }

    public String getLetterGrade() {
        return finalLetter;
    }

    @Override
    public String toString() {
        return "Grade{ enrollment=" + enrollmentId +
                ", component='" + component +
                "', score=" + score +
                ", final='" + finalLetter + "' }";
    }
}
