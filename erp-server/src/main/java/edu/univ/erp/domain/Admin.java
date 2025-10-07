package edu.univ.erp.domain;

// Represents an admin user; mainly metadata since auth is in UserAuth
public class Admin {
    private int adminId;
    private String username;

    public Admin(int adminId, String username) {
        this.adminId = adminId;
        this.username = username;
    }

    public int getAdminId() { return adminId; }
    public String getUsername() { return username; }
}
