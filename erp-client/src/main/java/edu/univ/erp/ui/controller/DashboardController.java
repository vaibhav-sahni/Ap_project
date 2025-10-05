package edu.univ.erp.ui.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane; // NEW IMPORT for file selection

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
            fetchAndDisplayTimetable(); // Load the student's schedule
            
            // --- Feature Test (Simulated Button Click) ---
            
            // Uncomment the line below to simulate a transcript download request
            //handleDownloadTranscriptClick();
            
            // int sectionToRegister = 1; 
            // handleRegisterCourseClick(sectionToRegister); 
            
            // int sectionToDrop = 1; 
            // handleDropCourseClick(sectionToDrop); 
        }
        
        System.out.println("LOG: Main Dashboard UI launched.");
    }
    
    // ----------------------------------------------------------------------
    // --- 1. COURSE DROP FEATURE (EXISTING) --------------------------------
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

            // --- REFRESH DATA ---
            fetchAndDisplayCatalog(); 
            fetchAndDisplayTimetable(); 
            

        } catch (Exception e) {
            // Display specific error message relayed from the server 
            String errorMsg = e.getMessage();
            JOptionPane.showMessageDialog(null, errorMsg, "Course Drop Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Drop Failed: " + errorMsg);
        }
    }

    // ----------------------------------------------------------------------
    // --- 2. ENROLLMENT FEATURE (EXISTING) ----------------------------------
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
            
            // --- REFRESH DATA ---
            fetchAndDisplayCatalog(); 
            fetchAndDisplayTimetable(); 

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            JOptionPane.showMessageDialog(null, errorMsg, "Registration Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Registration Failed: " + errorMsg);
        }
    }

    // ----------------------------------------------------------------------
    // --- 3. TIMETABLE FEATURE (EXISTING) ------------------------------------
    // ----------------------------------------------------------------------
    /**
     * Fetches the student's current schedule and prints it to the console.
     */
    public void fetchAndDisplayTimetable() {
        try {
            List<CourseCatalog> schedule = studentApi.getTimetable(this.user.getUserId());
            
            System.out.println("\n--- CURRENT STUDENT TIMETABLE ---");
            if (schedule.isEmpty()) {
                System.out.println("You are not currently registered for any courses.");
                return;
            } 
            
            System.out.printf("%-10s %-40s %-15s %-10s %s\n", 
                                  "SECTION", "COURSE TITLE", "INSTRUCTOR", "ROOM", "TIME/DAY");
            System.out.println("-------------------------------------------------------------------------------------------------");
            
            schedule.forEach(c -> {
                System.out.printf("%-10s %-40s %-15s %-10s %s\n", 
                                      c.getSectionId(),
                                      c.getCourseTitle(), 
                                      c.getInstructorName(),
                                      c.getRoom(),
                                      c.getDayTime());
            });
            System.out.println("--------------------------------\n");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch student timetable via API: " + e.getMessage());
        }
    }


    // ----------------------------------------------------------------------
    // --- 4. COURSE CATALOG FEATURE (EXISTING) -----------------------------
    // ----------------------------------------------------------------------
    public void fetchAndDisplayCatalog() {
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
    // --- 5. CHANGE PASSWORD FEATURE (EXISTING) ----------------------------
    // ----------------------------------------------------------------------
    public void handleChangePasswordClick() {
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
    // --- 6. GRADES FEATURE (EXISTING) -------------------------------------
    // ----------------------------------------------------------------------
    private void fetchAndDisplayGrades() {
        try {
            List<Grade> grades = studentApi.getMyGrades(this.user.getUserId());
            
            System.out.println("\n--- DETAILED GRADES RECEIVED ---");
            if (grades.isEmpty()) {
                System.out.println("No grade data received.");
                return;
            } 
            
            grades.forEach(g -> {
                System.out.println("\nCourse: " + g.getCourseName() + " (Final Grade: " + g.getFinalGrade() + ")");
                System.out.println("   Components:");
                if (g.getComponents().isEmpty()) {
                    System.out.println("     - No components recorded.");
                } else {
                    g.getComponents().forEach(c -> 
                        // Note: Assumes 'c' is AssessmentComponent and has getComponentName/getScore
                        System.out.printf("     - %-20s: %.2f\n", c.getComponentName(), c.getScore())
                    );
                }
            });
            System.out.println("--------------------------------\n");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch grades via API: " + e.getMessage());
        }
    }
    
    // ----------------------------------------------------------------------
    // --- 7. DOWNLOAD TRANSCRIPT FEATURE (UPDATED WITH FILE CHOOSER) --------
    // ----------------------------------------------------------------------
    
    /**
     * Triggers the API call to download the transcript as a CSV string,
     * then prompts the user for a save location and saves the file.
     */
    public void handleDownloadTranscriptClick() {
        if (!"Student".equals(user.getRole())) {
            JOptionPane.showMessageDialog(null, "Only students can download transcripts.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            System.out.println("\n--- ATTEMPTING TRANSCRIPT DOWNLOAD (CSV) ---");
            
            // 1. Call the API to fetch the CSV content
            String csvContent = studentApi.downloadTranscript(user.getUserId());
            
            // 2. Setup and show the file chooser dialog
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Student Transcript");
            
            // Suggest a default filename
            String defaultFileName = "transcript_" + user.getUsername() + "_" + System.currentTimeMillis() + ".csv";
            fileChooser.setSelectedFile(new File(defaultFileName));

            int userSelection = fileChooser.showSaveDialog(null);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                
                // 3. Write the content to the selected file
                try (FileWriter writer = new FileWriter(fileToSave)) {
                    writer.write(csvContent);
                    
                    JOptionPane.showMessageDialog(null, 
                        "Transcript successfully downloaded and saved to:\n" + fileToSave.getAbsolutePath(), 
                        "Download Complete", 
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    System.out.println("CLIENT LOG: Transcript saved to: " + fileToSave.getAbsolutePath());
                } catch (IOException e) {
                    // Handle local disk error
                    JOptionPane.showMessageDialog(null, "Error saving file locally: " + e.getMessage(), "File Save Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // User cancelled the dialog
                JOptionPane.showMessageDialog(null, "Transcript download cancelled.", "Download Status", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("CLIENT LOG: Transcript download cancelled by user.");
            }
            
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            JOptionPane.showMessageDialog(null, errorMsg, "Download Failed", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Transcript Download Failed: " + errorMsg);
        }
    }}