package edu.univ.erp.model;

public class Instructor {

    private long instructorId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String department;

    public Instructor() {}

    public Instructor(long instructorId, String firstName, String lastName) {
        this.instructorId = instructorId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(long instructorId) {
        this.instructorId = instructorId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}
