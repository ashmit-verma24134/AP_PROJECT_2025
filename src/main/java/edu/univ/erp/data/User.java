package edu.univ.erp.data;

import java.sql.Timestamp;

public class User {
    private long id;
    private String email;
    private String passwordHash;
    private String role;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    // Backwards-compatible alias methods (for UI code expecting older names)
public long getUserId() { return getId(); }
public void setUserId(long userId) { setId(userId); }

public String getUsername() { return getEmail(); }
public void setUsername(String username) { setEmail(username); }


    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
