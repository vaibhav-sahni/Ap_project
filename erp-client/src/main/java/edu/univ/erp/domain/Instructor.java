package edu.univ.erp.domain;

/**
 * Represents the full profile of an Instructor, inheriting identity from UserAuth 
 * and adding fields from the erp_db.instructors table.
 */
public class Instructor extends UserAuth { 
    
    // Fields specific to the ERP profile
    private final String name;         // <-- CORRECTED: Use 'name'
    private final String department; 
    
    public Instructor(int userId, String username, String role, 
                      String name, String department) { // <-- CORRECTED: Use 'name'
        
        // 1. Call the parent (UserAuth) constructor
        super(userId, username, role); 
        
        // 2. Initialize the Instructor-specific fields
        this.name = name;          // <-- CORRECTED
        this.department = department;
    }

    // --- Getters ---
    public String getName() { return name; }       // <-- NEW Getter
    public String getDepartment() { return department; }
}