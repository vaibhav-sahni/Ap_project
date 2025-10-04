package edu.univ.erp.domain;

/**
 * Represents the authenticated user's identity and role.
 * This object is stored in the SessionManager after successful login.
 */
public class UserAuth {
    
    private final int userId;
    private final String username;
    private final String role; // "Student", "Instructor", "Admin"

    public UserAuth(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    // --- Getters ---
    
    public int getUserId() { 
        return userId; 
    }
    
    public String getUsername() { 
        return username; 
    }
    
    public String getRole() { 
        return role; 
    }

    // Note: No setters, making this object immutable after creation.
}