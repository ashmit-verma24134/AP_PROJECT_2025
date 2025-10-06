package models;

import java.time.LocalDateTime;

public class Grade {
  private long gradeId;
  private long enrollmentId;
  private String component;
  private String letterGrade;
  private double points;
  private String remarks;
  private LocalDateTime createdAt;

  public long getGradeId(){ return gradeId; }
  public void setGradeId(long id){ this.gradeId = id; }
  public long getEnrollmentId(){ return enrollmentId; }
  public void setEnrollmentId(long id){ this.enrollmentId = id; }
  public String getComponent(){ return component; }
  public void setComponent(String c){ this.component = c; }
  public String getLetterGrade(){ return letterGrade; }
  public void setLetterGrade(String g){ this.letterGrade = g; }
  public double getPoints(){ return points; }
  public void setPoints(double p){ this.points = p; }
  public String getRemarks(){ return remarks; }
  public void setRemarks(String r){ this.remarks = r; }
  public LocalDateTime getCreatedAt(){ return createdAt; }
  public void setCreatedAt(LocalDateTime t){ this.createdAt = t; }
}
