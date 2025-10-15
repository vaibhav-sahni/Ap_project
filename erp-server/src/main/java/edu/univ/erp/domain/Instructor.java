package edu.univ.erp.domain;

 
/**
 * Domain model representing an Instructor user.
 * This object is used primarily during authentication and to identify the 
 * instructor when fetching their assigned sections.
 */
public class Instructor extends UserAuth {
    
    private int instructorId;
    private String Name;
    private String departmentName;
    

    public Instructor() {}

    public Instructor(int userId, String username, String role, 
                      String department) {
        super(userId, username, role);
        this.departmentName = department;
    }


    // --- Getters and Setters ---
    public int getInstructorId() { return instructorId; }
    public void setInstructorId(int instructorId) { this.instructorId = instructorId; }
    
    public String getName(){return this.Name;}
    public void setName(String Name){this.Name=Name;}
    
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    
    
}