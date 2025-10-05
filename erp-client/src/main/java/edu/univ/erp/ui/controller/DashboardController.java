package edu.univ.erp.ui.controller;

import edu.univ.erp.domain.UserAuth; 

public class DashboardController {

    // 1. Declare a private field to store the user data (the state)
    private final UserAuth user;

    // 2. Add the required constructor (the fix for the error)
    public DashboardController(UserAuth user) {
        this.user = user; // Store the authenticated user data
    }

    /**
     * Placeholder method to launch the main application dashboard 
     * based on the authenticated user's role.
     */
    public void initDashboard() {
        // --- REAL DASHBOARD LOGIC GOES HERE LATER ---
        
        System.out.println("\n=============================================");
        System.out.println("✅ AUTHENTICATION SUCCESSFUL! (Controller Instance)");
        System.out.println("User ID: " + user.getUserId());
        System.out.println("User Logged In: " + user.getUsername());
        System.out.println("Role Determined: " + user.getRole());
        System.out.println("=============================================");
        
        // In the final version, you would use:
        // if (this.user.getRole().equals("Admin")) { new AdminDashboard(this.user).setVisible(true); }
        // ... etc.
        
        System.out.println("LOG: Dashboard opening logic skipped for testing.");
    }
}