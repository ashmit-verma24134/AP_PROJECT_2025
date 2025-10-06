package models;

import java.time.LocalDateTime;

public class Instructor {
  private long instructorId;
  private String fullName;
  private String department;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public long getInstructorId(){ return instructorId; }
  public void setInstructorId(long id){ this.instructorId = id; }
  public String getFullName(){ return fullName; }
  public void setFullName(String n){ this.fullName = n; }
  public String getDepartment(){ return department; }
  public void setDepartment(String d){ this.department = d; }
  public LocalDateTime getCreatedAt(){ return createdAt; }
  public void setCreatedAt(LocalDateTime t){ this.createdAt = t; }
  public LocalDateTime getUpdatedAt(){ return updatedAt; }
  public void setUpdatedAt(LocalDateTime t){ this.updatedAt = t; }
  @Override public String toString(){ return "Instructor{" + instructorId + ", " + fullName + "}"; }
}
