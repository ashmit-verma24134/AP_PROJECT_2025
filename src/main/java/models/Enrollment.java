package models;

import java.time.LocalDateTime;

public class Enrollment {
  private long enrollmentId;
  private long studentId;
  private long sectionId;
  private String status;
  private LocalDateTime enrolledAt;
  private LocalDateTime updatedAt;

  public long getEnrollmentId(){return enrollmentId;}
  public void setEnrollmentId(long id){this.enrollmentId = id;}
  public long getStudentId(){return studentId;}
  public void setStudentId(long id){this.studentId = id;}
  public long getSectionId(){return sectionId;}
  public void setSectionId(long id){ this.sectionId = id; }
  public String getStatus(){ return status; }
  public void setStatus(String s){this.status=s;}
  public LocalDateTime getEnrolledAt(){return enrolledAt;}
  public void setEnrolledAt(LocalDateTime t){this.enrolledAt=t;}
  public LocalDateTime getUpdatedAt(){return updatedAt;}
  public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
}
