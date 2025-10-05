package edu.univ.erp.domain;

import java.io.Serializable;

public class UserAuth implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String role;
    private String hashedPassword; // Used internally by the server only

    public UserAuth(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }
    
    public UserAuth() { }

    // Getters and Setters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getHashedPassword() { return hashedPassword; } // Used by AuthService
    
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }
}