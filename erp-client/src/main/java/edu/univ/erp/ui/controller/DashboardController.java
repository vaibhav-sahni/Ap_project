package edu.univ.erp.ui.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import edu.univ.erp.api.admin.AdminAPI;
import edu.univ.erp.api.auth.AuthAPI;
import edu.univ.erp.api.instructor.InstructorAPI;
import edu.univ.erp.api.student.StudentAPI;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.EnrollmentRecord;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.UserAuth;

public class DashboardController {
 
  private final UserAuth user;
  
  private final StudentAPI studentApi = new StudentAPI();
  private final AuthAPI authApi = new AuthAPI();
  private final InstructorAPI instructorApi = new InstructorAPI(); 
  private final AdminAPI adminApi = new AdminAPI();

  public DashboardController(UserAuth user) {
    this.user = user; 
  }

  public void initDashboard() {
        System.out.println("\n=============================================");
        System.out.println("✅ Dashboard Initializing for: " + user.getUsername());
        System.out.println("Role: " + user.getRole());
        System.out.println("=============================================");

        switch (user.getRole()) {
            case "Student":
                fetchAndDisplayGrades();
                fetchAndDisplayCatalog();
                fetchAndDisplayTimetable();
                handleDownloadTranscriptClick();
                //handleChangePasswordClick();
                //handleRegisterCourseClick(sectionId);
                //handleDropCourseClick(sectionId);
              
                
                break;

            case "Instructor":
                List<Section> assignedSections = fetchAndDisplayAssignedSections();
                if (!assignedSections.isEmpty()) {
                    int testSectionId = assignedSections.get(0).getSectionId();
                    handleViewRosterClick(testSectionId);
                    //computeFinalGradesForSection(testSectionId);
                    
                }
                break;

            case "Admin":
                fetchAndDisplayAllStudents();
                fetchAndDisplayAllCourses();
                //handleCreateStudentClick();
                //handleCreateCourseClick();
                break;

            default:
                System.out.println("Unknown role. Dashboard not initialized.");
        }

        System.out.println("LOG: Dashboard UI initialized successfully.");
    }

    // ==================== ADMIN METHODS ====================

    /**
     * Fetches and prints all students.
     */
    public void fetchAndDisplayAllStudents() {
        if (!"Admin".equals(user.getRole())) return;

        try {
            List<Student> students = adminApi.getAllStudents();
            System.out.println("\n--- ALL STUDENTS ---");
            if (students.isEmpty()) {
                System.out.println("No students found.");
                return;
            }

            System.out.printf("%-10s %-30s %-15s %-20s\n", "ID", "ROLL_NO", "USERNAME","PROGRAM");
            System.out.println("---------------------------------------------------------------------");
            for (Student s : students) {
                System.out.printf("%-10d %-30s %-15s %-20s\n",
                        s.getUserId(),
                        s.getRollNo(),
                        s.getUsername(),
                        s.getProgram()
                        );
            }
            System.out.println("--------------------------------\n");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch students via API: " + e.getMessage());
        }
    }

    /**
     * Fetches and prints all courses/sections.
     */
    public void fetchAndDisplayAllCourses() {
        if (!"Admin".equals(user.getRole())) return;

        try {
            List<CourseCatalog> courses = adminApi.getAllCourses();
            System.out.println("\n--- ALL COURSES / SECTIONS ---");
            if (courses.isEmpty()) {
                System.out.println("No courses found.");
                return;
            }

            System.out.printf("%-10s %-40s %-15s %-10s %-10s\n",
                    "CODE", "TITLE", "INSTRUCTOR", "CAPACITY", "SECTION ID");
            System.out.println("---------------------------------------------------------------------");
            for (CourseCatalog c : courses) {
                System.out.printf("%-10s %-40s %-15s %-10d %-10d\n",
                        c.getCourseCode(),
                        c.getCourseTitle(),
                        c.getInstructorName(),
                        c.getCapacity(),
                        c.getSectionId());
            }
            System.out.println("--------------------------------\n");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch courses via API: " + e.getMessage());
        }
    }

    public void handleCreateStudentClick() {
    try {
        // Prompt for inputs

        String userIdStr = JOptionPane.showInputDialog(null, "Enter User ID:");
        if (userIdStr == null) return;
        int userId = Integer.parseInt(userIdStr);

        String username = JOptionPane.showInputDialog(null, "Enter Username:");
        if (username == null) return;

        String role = "Student"; // fixed
        String rollNo = JOptionPane.showInputDialog(null, "Enter Roll Number:");
        if (rollNo == null) return;

        String program = JOptionPane.showInputDialog(null, "Enter Program Name:");
        if (program == null) return;

        String yearStr = JOptionPane.showInputDialog(null, "Enter Year of Study:");
        if (yearStr == null) return;
        int year = Integer.parseInt(yearStr);

        String password = JOptionPane.showInputDialog(null, "Enter Initial Password:");
        if (password == null) return;

        // Construct immutable Student object
        Student newStudent = new Student(userId, username, role, rollNo, program, year);
        

      
        String successMsg = adminApi.createStudent(newStudent, password);

        JOptionPane.showMessageDialog(null, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);

    } catch (NumberFormatException nfe) {
        JOptionPane.showMessageDialog(null, "Invalid number entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage(), "Student Creation Failed", JOptionPane.ERROR_MESSAGE);
        System.err.println("CLIENT ERROR: " + e.getMessage());
    }
}

    public void handleCreateCourseClick() {
    try {
        // Prompt for inputs
        String code = JOptionPane.showInputDialog(null, "Enter Course Code:");
        if (code == null) return;

        String title = JOptionPane.showInputDialog(null, "Enter Course Title:");
        if (title == null) return;

        String creditsStr = JOptionPane.showInputDialog(null, "Enter Credits:");
        if (creditsStr == null) return;
        int credits = Integer.parseInt(creditsStr);


        String dayTime = JOptionPane.showInputDialog(null, "Enter Day/Time (e.g., Mon 9-11):");
        if (dayTime == null) dayTime = "";

        String room = JOptionPane.showInputDialog(null, "Enter Room:");
        if (room == null) room = "";

        String capacityStr = JOptionPane.showInputDialog(null, "Enter Capacity:");
        if (capacityStr == null) return;
        int capacity = Integer.parseInt(capacityStr);

        String enrolledCountStr = JOptionPane.showInputDialog(null, "Enter Enrolled Count:");
        if (enrolledCountStr == null) return;
        int enrolledCount = Integer.parseInt(enrolledCountStr);

        String semester = JOptionPane.showInputDialog(null, "Enter Semester:");
        if (semester == null) semester = "";

        String yearStr = JOptionPane.showInputDialog(null, "Enter Year:");
        if (yearStr == null) return;
        int year = Integer.parseInt(yearStr);

        String instructorID = JOptionPane.showInputDialog(null, "Enter Instructor ID:");
        if (instructorID == null) return;
        int instrid = Integer.parseInt(instructorID);

        String instr_name = JOptionPane.showInputDialog(null, "Enter Instructor Name:");
        if (instr_name == null) semester = "";


        // Construct immutable CourseCatalog object
        CourseCatalog course = new CourseCatalog(
                code, title, credits, 0, dayTime, room, capacity,
                enrolledCount, semester, year, instrid, instr_name
        );

        // Call AdminAPI
        String successMsg = adminApi.createCourseAndSection(course);

        JOptionPane.showMessageDialog(null, successMsg, "Success", JOptionPane.INFORMATION_MESSAGE);

    } catch (NumberFormatException nfe) {
        JOptionPane.showMessageDialog(null, "Invalid number entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage(), "Course Creation Failed", JOptionPane.ERROR_MESSAGE);
        System.err.println("CLIENT ERROR: " + e.getMessage());
    }
}


    /**
     * Toggles maintenance mode (ON/OFF) using AdminAPI.
     */
    public void handleToggleMaintenanceClick() {
        if (!"Admin".equals(user.getRole())) return;

        try {
            int choice = JOptionPane.showConfirmDialog(null,
                    "Do you want to TURN ON maintenance mode? (Cancel will turn OFF)",
                    "Maintenance Mode", JOptionPane.YES_NO_CANCEL_OPTION);

            boolean on = choice == JOptionPane.YES_OPTION;
            String response = adminApi.toggleMaintenance(on);

            JOptionPane.showMessageDialog(null,
                    "Maintenance mode updated: " + response,
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to toggle maintenance mode: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("CLIENT ERROR: Toggle Maintenance: " + e.getMessage());
        }
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
  * NOTE: This relies on CourseCatalog, which is assumed to contain scheduling details.
  */
  public void fetchAndDisplayTimetable() {
    try {
      List<CourseCatalog> schedule = studentApi.getTimetable(this.user.getUserId());
      
      System.out.println("\n--- CURRENT STUDENT TIMETABLE ---");
      if (schedule.isEmpty()) {
        System.out.println("You are not currently registered for any courses.");
        return;
      } 
      
      // CourseCatalog methods are assumed to be correct based on prior context
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
      
      // CourseCatalog methods are assumed to be correct based on prior context
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
      
      // Grade methods are assumed to be correct based on prior context
      grades.forEach(g -> {
        System.out.println("\nCourse: " + g.getCourseName() + " (Final Grade: " + g.getFinalGrade() + ")");
        System.out.println("  Components:");
        if (g.getComponents().isEmpty()) {
          System.out.println("    - No components recorded.");
        } else {
          g.getComponents().forEach(c -> 
            // Note: Assumes 'c' is AssessmentComponent and has getComponentName/getScore
            System.out.printf("       - %-20s: %.2f\n", c.getComponentName(), c.getScore())
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
      System.out.println("\n--- ATTEMPTING TRANSCRIPT DOWNLOAD (HTML) ---");
      
      String htmlContent = studentApi.downloadTranscript(user.getUserId());
      
      // 2. Setup and show the file chooser dialog
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Save Student Transcript");
      
      // Suggest a default filename
      String defaultFileName = "transcript_" + user.getUsername() + "_" + System.currentTimeMillis() + ".html";
      fileChooser.setSelectedFile(new File(defaultFileName));

      int userSelection = fileChooser.showSaveDialog(null);

      if (userSelection == JFileChooser.APPROVE_OPTION) {
        File fileToSave = fileChooser.getSelectedFile();
        
        // 3. Write the content to the selected file
        try (FileWriter writer = new FileWriter(fileToSave)) {
          writer.write(htmlContent);
          
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
  }

  // ----------------------------------------------------------------------
  // --- 8. INSTRUCTOR SECTION VIEW FEATURE (UPDATED TO RETURN LIST) ------
  // ----------------------------------------------------------------------
  /**
  * Fetches and displays all course sections assigned to the current instructor.
  * @return The list of assigned Section objects.
  */
 public List<Section> fetchAndDisplayAssignedSections() {
    if (!"Instructor".equals(user.getRole())) return Collections.emptyList();

    try {
        List<Section> assignedSections = instructorApi.getAssignedSections(this.user.getUserId());

        System.out.println("\n--- ASSIGNED SECTIONS FOR INSTRUCTOR ---");
        if (assignedSections.isEmpty()) {
            System.out.println("You are not currently assigned to teach any sections.");
            return assignedSections;
        }

        System.out.printf("%-10s %-40s %-15s %s\n",
                "SECTION ID", "COURSE NAME", "COURSE CODE", "ENROLLED/CAPACITY");
        System.out.println("-------------------------------------------------------------------------------------------------");

        for (Section s : assignedSections) {
            // Dynamically fetch the roster size
            int enrolledCount = instructorApi.getRoster(user.getUserId(), s.getSectionId()).size();
            String enrolledCapacity = enrolledCount + "/" + s.getCapacity();

            System.out.printf("%-10s %-40s %-15s %s\n",
                    s.getSectionId(),
                    s.getCourseName(),
                    s.getCourseCode(),
                    enrolledCapacity);
        }

        System.out.println("--------------------------------\n");
        return assignedSections;

    } catch (Exception e) {
        System.err.println("ERROR: Failed to fetch assigned sections via API: " + e.getMessage());
        return Collections.emptyList();
    }
}

 // --------------------------- INSTRUCTOR DASHBOARD METHODS ---------------------------

/**
 * Displays the roster for a given section (without computing grades).
 * @param sectionId The section ID to view.
 */
public void handleViewRosterClick(int sectionId) {
    if (!"Instructor".equals(user.getRole())) {
        JOptionPane.showMessageDialog(null, "Access Denied.", "Access Denied", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        List<EnrollmentRecord> roster = instructorApi.getRoster(user.getUserId(), sectionId);

        if (roster.isEmpty()) {
            System.out.println("Roster is empty for this section.");
            return;
        }

        System.out.println("\n--- ROSTER FOR SECTION ID: " + sectionId + " ---");
        System.out.printf("%-10s %-30s %-10s %-10s\n", "ENROLL ID", "STUDENT USERNAME", "ROLL NO", "FINAL GRADE");
        System.out.println("-------------------------------------------------------------------------------------------------");

        roster.forEach(r -> {
            System.out.printf("%-10d %-30s %-10s %-10s\n",
                    r.getEnrollmentId(),
                    r.getStudentName(),
                    r.getRollNo(),
                    r.getFinalGrade());
        });
        System.out.println("-------------------------------------------------------------------------------------------------");

    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage(), "Roster Fetch Failed", JOptionPane.ERROR_MESSAGE);
        System.err.println("CLIENT ERROR: Roster Fetch Failed: " + e.getMessage());
    }
}

/**
 * Computes final grades for all students in a section.
 * Prompts instructor for confirmation before proceeding.
 * @param sectionId The section ID to compute grades for.
 */
public void computeFinalGradesForSection(int sectionId) {
    if (!"Instructor".equals(user.getRole())) {
        JOptionPane.showMessageDialog(null, "Access Denied.", "Access Denied", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // Confirmation prompt
    int confirm = JOptionPane.showConfirmDialog(null,
            "Are you sure you want to compute final grades for all students in section " + sectionId + "? " +
            "This action will finalize grades and mark enrollments as Completed.",
            "Confirm Final Grading",
            JOptionPane.YES_NO_OPTION);

    if (confirm != JOptionPane.YES_OPTION) {
        System.out.println("CLIENT LOG: Final grading cancelled by user.");
        return;
    }

    try {
        List<EnrollmentRecord> roster = instructorApi.getRoster(user.getUserId(), sectionId);

        if (roster.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No students in this section.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        System.out.println("\n--- COMPUTING FINAL GRADES FOR SECTION ID: " + sectionId + " ---");

        for (EnrollmentRecord student : roster) {
            try {
                int enrollmentId = student.getEnrollmentId();
                String finalGradeMsg = instructorApi.computeFinalGrade(user.getUserId(), enrollmentId);
                System.out.println("CLIENT LOG: Final Grade Computed for " + student.getStudentName() +
                        " (EID " + enrollmentId + "): " + finalGradeMsg);
            } catch (Exception e) {
                System.err.println("CLIENT ERROR: Failed to compute grade for " + student.getStudentName() + ": " + e.getMessage());
            }
        }

        // Re-fetch roster to display updated grades
        roster = instructorApi.getRoster(user.getUserId(), sectionId);
        System.out.println("\n--- UPDATED ROSTER AFTER FINAL GRADING ---");
        System.out.printf("%-10s %-30s %-10s %-10s\n", "ENROLL ID", "STUDENT USERNAME", "ROLL NO", "FINAL GRADE");
        System.out.println("-------------------------------------------------------------------------------------------------");
        roster.forEach(r -> {
            System.out.printf("%-10d %-30s %-10s %-10s\n",
                    r.getEnrollmentId(),
                    r.getStudentName(),
                    r.getRollNo(),
                    r.getFinalGrade());
        });
        System.out.println("-------------------------------------------------------------------------------------------------");

        JOptionPane.showMessageDialog(null,
                "Final grades computed for all " + roster.size() + " students in section " + sectionId + ".",
                "Final Grading Complete",
                JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage(), "Final Grading Failed", JOptionPane.ERROR_MESSAGE);
        System.err.println("CLIENT ERROR: Final Grading Failed: " + e.getMessage());
    }
}
}