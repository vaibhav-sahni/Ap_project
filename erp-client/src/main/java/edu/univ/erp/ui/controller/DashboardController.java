package edu.univ.erp.ui.controller;

import java.util.List;

import javax.swing.JOptionPane; 

import edu.univ.erp.api.auth.AuthAPI;
import edu.univ.erp.api.student.StudentAPI;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Grade; 
import edu.univ.erp.domain.UserAuth;

public class DashboardController {
    
    private final UserAuth user;
    
    private final StudentAPI studentApi = new StudentAPI();
    private final AuthAPI authApi = new AuthAPI();

    public DashboardController(UserAuth user) {
        this.user = user; 
    }

    public void initDashboard() {
        System.out.println("\n=============================================");
        System.out.println("✅ Dashboard Initializing for: " + user.getUsername());
        System.out.println("Role: " + user.getRole());
        System.out.println("=============================================");
        
        // --- INITIAL DATA LOAD ---
        if ("Student".equals(user.getRole())) {
            fetchAndDisplayGrades();
            fetchAndDisplayCatalog(); 
            
            // --- Feature Test (Simulated Button Click) ---
            int sectionToRegister = 1; 
            // handleRegisterCourseClick(sectionToRegister); // Uncomment to test registration!
            
            int sectionToDrop = 1; 
            // handleDropCourseClick(sectionToDrop); // Uncomment to test the new drop feature!
        }
        
        System.out.println("LOG: Main Dashboard UI launched.");
    }
    
    // ----------------------------------------------------------------------
    // --- 1. COURSE DROP FEATURE (NEW CONTROLLER METHOD) -------------------
    // ----------------------------------------------------------------------

    /**
     * This method is triggered when a student attempts to drop a course section.
     * It handles the UI confirmation and calls the StudentAPI.
     * @param sectionId The ID of the section the student wishes to drop.
     */
    public void handleDropCourseClick(int sectionId) {
        if (!"Student".equals(user.getRole())) {
            JOptionPane.showMessageDialog(null, "Only students can drop courses.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Get confirmation before calling the API
            int confirm = JOptionPane.showConfirmDialog(null, 
                "Are you sure you want to drop section ID " + sectionId + "? This action is final.",
                "Confirm Course Drop", JOptionPane.YES_NO_OPTION);
                
            if (confirm != JOptionPane.YES_OPTION) {
                return; // User cancelled
            }
            
            System.out.println("\n--- ATTEMPTING COURSE DROP ---");
            
            // Call the API
            String successMsg = studentApi.dropCourse(user.getUserId(), sectionId);
            
            // Display success message from the server
            JOptionPane.showMessageDialog(null, successMsg, "Course Drop Success", JOptionPane.INFORMATION_MESSAGE);
            System.out.println("CLIENT LOG: Drop Success: " + successMsg);

            // In a real application, refresh your UI data here:
            // fetchAndDisplayCatalog(); 

        } catch (Exception e) {
            // Display specific error message relayed from the server (e.g., Deadline passed, Not Registered)
            String errorMsg = e.getMessage();
            JOptionPane.showMessageDialog(null, errorMsg, "Course Drop Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Drop Failed: " + errorMsg);
        }
    }

    // ----------------------------------------------------------------------
    // --- 2. ENROLLMENT FEATURE (EXISTING) ---------------------------------
    // ----------------------------------------------------------------------

    /**
     * This method is triggered when a student attempts to register for a course section.
     */
    public void handleRegisterCourseClick(int sectionId) {
        if (!"Student".equals(user.getRole())) {
            JOptionPane.showMessageDialog(null, "Only students can register for courses.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            System.out.println("\n--- ATTEMPTING COURSE REGISTRATION ---");
            
            String successMsg = studentApi.registerCourse(user.getUserId(), sectionId);
            
            JOptionPane.showMessageDialog(null, successMsg, "Registration Success", JOptionPane.INFORMATION_MESSAGE);
            System.out.println("CLIENT LOG: Registration Success: " + successMsg);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            JOptionPane.showMessageDialog(null, errorMsg, "Registration Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Registration Failed: " + errorMsg);
        }
    }


    // ----------------------------------------------------------------------
    // --- 3. COURSE CATALOG FEATURE (EXISTING) -----------------------------
    // ----------------------------------------------------------------------
    public void fetchAndDisplayCatalog() {
        // ... (existing code for fetchAndDisplayCatalog) ...
        try {
            List<CourseCatalog> catalog = studentApi.getCourseCatalog();
            
            System.out.println("\n--- COURSE CATALOG RECEIVED ---");
            if (catalog.isEmpty()) {
                System.out.println("No courses available in the catalog.");
                return;
            } 
            
            System.out.printf("%-10s %-40s %-15s %-10s %s\n", 
                              "CODE", "TITLE", "INSTRUCTOR", "CAPACITY", "TIME");
            System.out.println("-------------------------------------------------------------------------------------------------");
            
            catalog.forEach(c -> {
                System.out.printf("%-10s %-40s %-15s %-10s %s\n", 
                                  c.getCourseCode(), 
                                  c.getCourseTitle(), 
                                  c.getInstructorName(),
                                  c.getEnrolledCount() + "/" + c.getCapacity(),
                                  c.getDayTime());
            });
            System.out.println("--------------------------------\n");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch course catalog via API: " + e.getMessage());
        }
    }


    // ----------------------------------------------------------------------
    // --- 4. CHANGE PASSWORD FEATURE (EXISTING) ----------------------------
    // ----------------------------------------------------------------------
    public void handleChangePasswordClick() {
        // ... (existing code for handleChangePasswordClick) ...
        try {
            String oldPass = JOptionPane.showInputDialog(null, "Enter Current Password:");
            if (oldPass == null) return; 

            String newPass = JOptionPane.showInputDialog(null, "Enter New Password:");
            if (newPass == null) return; 
            
            String confirmPass = JOptionPane.showInputDialog(null, "Confirm New Password:");
            if (confirmPass == null) return; 

            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(null, "New password and confirmation do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String successMsg = authApi.changePassword(user.getUserId(), oldPass, newPass);
            
            JOptionPane.showMessageDialog(null, successMsg, "Password Change Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Password Change Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Password Change: " + e.getMessage());
        }
    }


    // ----------------------------------------------------------------------
    // --- 5. GRADES FEATURE (EXISTING) -------------------------------------
    // ----------------------------------------------------------------------
    private void fetchAndDisplayGrades() {
        // ... (existing code for fetchAndDisplayGrades) ...
        try {
            List<Grade> grades = studentApi.getMyGrades(this.user.getUserId());
            
            System.out.println("\n--- DETAILED GRADES RECEIVED ---");
            if (grades.isEmpty()) {
                System.out.println("No grade data received.");
                return;
            } 
            
            grades.forEach(g -> {
                System.out.println("\nCourse: " + g.getCourseName() + " (Final Grade: " + g.getFinalGrade() + ")");
                System.out.println("  Components:");
                if (g.getComponents().isEmpty()) {
                    System.out.println("    - No components recorded.");
                } else {
                    g.getComponents().forEach(c -> 
                        System.out.printf("    - %-20s: %.2f\n", c.getComponentName(), c.getScore())
                    );
                }
            });
            System.out.println("--------------------------------\n");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch grades via API: " + e.getMessage());
        }
    }
}