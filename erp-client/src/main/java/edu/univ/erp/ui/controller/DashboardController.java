package edu.univ.erp.ui.controller;

import java.util.List;

import edu.univ.erp.api.grade.GradeAPI;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.UserAuth;

public class DashboardController {
    
    private final UserAuth user;

    public DashboardController(UserAuth user) {
        this.user = user; 
    }

    public void initDashboard() {
        System.out.println("\n=============================================");
        System.out.println("✅ Dashboard Initializing for: " + user.getUsername());
        System.out.println("Role: " + user.getRole());
        System.out.println("=============================================");
        
        // --- FEATURE TEST: FETCH GRADES ---
        if ("Student".equals(user.getRole())) {
            fetchAndDisplayGrades();
        }
        
        // Final UI implementation would launch the Swing/JavaFX window here.
        System.out.println("LOG: Main Dashboard UI launched.");
    }

    private void fetchAndDisplayGrades() {
        GradeAPI gradeApi = new GradeAPI();
        
        try {
            List<Grade> grades = gradeApi.getMyGrades(this.user.getUserId());
            
            System.out.println("\n--- DETAILED GRADES RECEIVED ---");
            if (grades.isEmpty()) {
                System.out.println("No grade data received.");
                return;
            } 
            
            grades.forEach(g -> {
                System.out.println("\nCourse: " + g.getCourseName() + " (Final Grade: " + g.getFinalGrade() + ")");
                System.out.println("  Components:");
                if (g.getComponents().isEmpty()) {
                    System.out.println("    - No components recorded.");
                } else {
                    g.getComponents().forEach(c -> 
                        System.out.printf("    - %-20s: %.2f\n", c.getComponentName(), c.getScore())
                    );
                }
            });
            System.out.println("--------------------------------\n");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch grades via API: " + e.getMessage());
        }
    }
}