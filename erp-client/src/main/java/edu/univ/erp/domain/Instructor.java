package edu.univ.erp.domain;

import java.io.Serializable;

/**
 * Domain model representing an Instructor user.
 * This object is used primarily during authentication and to identify the 
 * instructor when fetching their assigned sections.
 */
public class Instructor implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int instructorId;
    private String firstName;
    private String lastName;
    private String departmentName;
    private String email;

    public Instructor() {}

    public Instructor(int instructorId, String firstName, String lastName, String departmentName, String email) {
        this.instructorId = instructorId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.departmentName = departmentName;
        this.email = email;
    }

    // --- Getters and Setters ---
    public int getInstructorId() { return instructorId; }
    public void setInstructorId(int instructorId) { this.instructorId = instructorId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}