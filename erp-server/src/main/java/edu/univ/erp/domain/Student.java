package edu.univ.erp.domain;
/**
 * Represents the full profile of a Student, inheriting identity from UserAuth 
 * and adding fields from the erp_db.students table.
 */
public class Student extends UserAuth { 
    
    // Fields specific to the ERP profile
    private  String rollNo;
    private  String program;
    private  int year;
    
    /**
     * Constructor for a Student profile.
     * @param userId Inherited from UserAuth, the unique ID link between auth_db and erp_db.
     * @param username Inherited from UserAuth.
     * @param role Inherited from UserAuth (should be "Student").
     * @param rollNo The student's unique roll number.
     * @param program The student's enrolled program (e.g., "Computer Engineering").
     * @param year The student's current year of study.
     */
    public Student(){}
    public Student(int userId, String username, String role, 
                   String rollNo, String program, int year) {
        
        // 1. Call the parent (UserAuth) constructor to initialize identity fields
        super(userId, username, role); 
        
        // 2. Initialize the Student-specific fields
        this.rollNo = rollNo;
        this.program = program;
        this.year = year;
    }

    // --- Getters for ERP Profile Fields ---
    
    public String getRollNo() { 
        return rollNo; 
    }
    
    public String getProgram() { 
        return program; 
    }
    
    public int getYear() { 
        return year; 
    }
}