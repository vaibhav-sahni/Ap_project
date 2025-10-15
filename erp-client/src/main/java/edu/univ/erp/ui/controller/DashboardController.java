package edu.univ.erp.ui.controller;


import java.util.List;

import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.ui.handlers.AdminUiHandlers;
import edu.univ.erp.ui.handlers.AuthUiHandlers;
import edu.univ.erp.ui.handlers.InstructorUiHandlers;
import edu.univ.erp.ui.handlers.StudentUiHandlers;

public class DashboardController {

  private final UserAuth user;
  // optional owner frame so we can dispose it on logout
  private final javax.swing.JFrame ownerFrame;

  // UI handler adapters
  private final StudentUiHandlers studentUiHandlers;
  private final AdminUiHandlers adminUiHandlers;
  private final InstructorUiHandlers instructorUiHandlers;
  private final AuthUiHandlers authUiHandlers;

  public DashboardController(UserAuth user) {
    this(user, null);
  }

  /**
   * Construct with optional owner frame. If provided, the frame will be disposed after logout.
   */
  public DashboardController(UserAuth user, javax.swing.JFrame ownerFrame) {
    this.user = user;
    this.ownerFrame = ownerFrame;
    this.studentUiHandlers = new StudentUiHandlers(user);
    this.adminUiHandlers = new AdminUiHandlers(user);
    this.instructorUiHandlers = new InstructorUiHandlers(user);
    this.authUiHandlers = new AuthUiHandlers(user);
  }

  public void initDashboard() {
        System.out.println("\n=============================================");
        System.out.println("✅ Dashboard Initializing for: " + user.getUsername());
        System.out.println("Role: " + user.getRole());
        System.out.println("=============================================");

        switch (user.getRole()) {
      case "Student":
        // Move UI initialization into handler so controller remains UI-free
        studentUiHandlers.initStudentDashboard();
        // auto-logout for demo flows (handler will present UI if needed)
        handleLogoutClick();
              
                
                break;

            case "Instructor":
                        List<Section> assignedSections = instructorUiHandlers.displayAssignedSections(user.getUserId());
                        if (!assignedSections.isEmpty()) {
                            int testSectionId = assignedSections.get(0).getSectionId();
                            handleViewRosterClick(testSectionId);
                            //computeFinalGradesForSection(testSectionId);
                            handleLogoutClick();
                        }
                break;

            case "Admin":
                adminUiHandlers.displayAllStudents();
                adminUiHandlers.displayAllCourses();
                handleCreateStudentClick();
                //handleCreateCourseClick();
                //handleCreateInstructorClick();
                handleToggleMaintenanceClick();
                handleSetDropDeadlineClick();
                handleLogoutClick();
                break;

            default:
                System.out.println("Unknown role. Dashboard not initialized.");
        }

        System.out.println("LOG: Dashboard UI initialized successfully.");
    }

    // ==================== ADMIN METHODS ====================

  public void fetchAndDisplayAllStudents() {
    adminUiHandlers.displayAllStudents();
  }

  public void fetchAndDisplayAllCourses() {
  adminUiHandlers.displayAllCourses();
  }

  public void handleCreateStudentClick() {
    adminUiHandlers.handleCreateStudentClick();
  }

  public void handleCreateInstructorClick() {
    adminUiHandlers.handleCreateInstructorClick();
  }

  public void handleCreateCourseClick() {
    adminUiHandlers.handleCreateCourseClick();
  }

  public void handleToggleMaintenanceClick() {
    adminUiHandlers.handleToggleMaintenanceClick();
  }
  
  public void handleDropCourseClick(int sectionId) {
    studentUiHandlers.handleDropCourseClick(sectionId);
  }

  
  public void handleRegisterCourseClick(int sectionId) {
    studentUiHandlers.handleRegisterCourseClick(sectionId);
  }

  public void fetchAndDisplayTimetable() {
    studentUiHandlers.displayTimetable();
  }

  public void fetchAndDisplayCatalog() {
    studentUiHandlers.displayCatalog();
  }

  public void handleChangePasswordClick() {
    authUiHandlers.changePasswordWithUi();
  }

  public void handleDownloadTranscriptClick() {
    studentUiHandlers.handleDownloadTranscriptClick();
  }

 public List<Section> fetchAndDisplayAssignedSections() {
  return instructorUiHandlers.displayAssignedSections(user.getUserId());
}

 // Instructor actions

public void handleViewRosterClick(int sectionId) {
    if (!"Instructor".equals(user.getRole())) {
    // leave UI presentation to handlers
    System.err.println("ACCESS DENIED: Only Instructors may view rosters.");
        return;
    }

    instructorUiHandlers.displayRoster(user.getUserId(), sectionId);
}

/**
 * Computes final grades for all students in a section.
 * Prompts instructor for confirmation before proceeding.
 * @param sectionId The section ID to compute grades for.
 */
public void computeFinalGradesForSection(int sectionId) {
    if (!"Instructor".equals(user.getRole())) {
        System.err.println("ACCESS DENIED: Only Instructors may compute final grades.");
        return;
    }

    // delegate confirmation and UI to handler
    instructorUiHandlers.computeFinalGradesWithUi(user.getUserId(), sectionId);
}
  /**
   * Prompt admin to set global drop deadline (ISO YYYY-MM-DD). This action is blocked when
   * the server reports maintenance mode ON.
   */
  public void handleSetDropDeadlineClick() {
    if (!"Admin".equals(user.getRole())) return;
    // delegate UI interactions and server pre-checks to the admin handler
    adminUiHandlers.handleSetDropDeadlineClick();
  }

  /**
   * Logout handler: asks for confirmation, calls AuthAPI.logout(), then
   * re-opens the Login screen.
   */
  public void handleLogoutClick() {
    try {
      boolean didLogout = authUiHandlers.confirmAndLogout();
      if (!didLogout) return;
    } catch (Exception e) {
      // auth handler already showed the dialog; just return
      return;
    }

    // Re-open the login screen (dispose owner frame if provided)
    javax.swing.SwingUtilities.invokeLater(() -> {
      if (ownerFrame != null) {
        try { ownerFrame.dispose(); } catch (Exception ex) { System.err.println("WARN: " + ex.getMessage()); }
      }
      new edu.univ.erp.ui.loginpage.main.Login().setVisible(true);
    });
  }

}