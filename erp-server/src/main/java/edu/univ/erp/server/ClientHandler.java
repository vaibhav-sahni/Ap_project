package edu.univ.erp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import com.google.gson.Gson;

import edu.univ.erp.dao.settings.SettingDAO; 
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.domain.Section; // NEW: Instructor's list of sections
import edu.univ.erp.domain.EnrollmentRecord; // NEW: Instructor's student roster data
import edu.univ.erp.service.auth.AuthService;
import edu.univ.erp.service.student.StudentService;
import edu.univ.erp.service.instructor.InstructorService; // NEW: Instructor's business logic

public class ClientHandler implements Runnable {
private final Socket clientSocket;
private final Gson gson = new Gson();
private final SettingDAO settingDAO = new SettingDAO(); 
    // New Service Initialization
    private final InstructorService instructorService = new InstructorService();

public ClientHandler(Socket socket) { this.clientSocket = socket; }
 @Override
  public void run() { 
    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true) 
    ) {
      // Keep reading requests until null
             String request;
             while ((request = in.readLine()) != null) {
                 String response = processRequest(request);
                 out.println(response);
             }

    } catch (IOException e) {
      System.err.println("SERVER LOG: ClientHandler network error: " + e.getMessage());
    } finally {
      try { clientSocket.close(); } catch (IOException e) { /* ignored */ }
    }
  }
  
  private String processRequest(String request) {
    try {
      if (request == null) return "ERROR:NO_REQUEST";
      
      String[] parts = request.split(":");
      String command = parts[0].toUpperCase();
      
      // --- CRITICAL STEP 1: Maintenance Mode Check (Rule #3) ---
      if (settingDAO.isMaintenanceModeOn()) {
        // Added Instructor write commands to the blocked list
        if (command.equals("REGISTER") || command.equals("DROP_SECTION") || 
          command.equals("RECORD_SCORE") || command.equals("COMPUTE_FINAL_GRADE") || // ADDED INSTRUCTOR GRADING
          command.equals("ADMIN:CREATE_USER") || 
          command.equals("ADMIN:TOGGLE_MAINTENANCE")) { 
          
          System.out.println("SERVER LOG: ACCESS DENIED: Command " + command + " blocked due to maintenance.");
          return "ERROR:MAINTENANCE_ON:The system is currently undergoing maintenance. Enrollment changes and grading operations are disabled.";
        }
      }
      
      // CENTRAL REQUEST ROUTER
      switch (command) {
        case "LOGIN":
          return handleLogin(parts);
        case "GET_GRADES":
          return handleGetGrades(parts);
        case "GET_CATALOG": 
          return handleGetCatalog();
        case "GET_TIMETABLE":
          return handleGetTimetable(parts);
        case "REGISTER": 
          return handleRegisterCourse(parts);
        case "DROP_SECTION": 
          return handleDropCourse(parts);
        case "CHANGE_PASSWORD": 
          return handleChangePassword(parts); 
        case "DOWNLOAD_TRANSCRIPT":
          return handleDownloadTranscript(parts);
                
                // --- NEW INSTRUCTOR COMMANDS ---
                case "GET_INSTRUCTOR_SECTIONS":
                    return handleGetInstructorSections(parts);
                case "GET_ROSTER":
                    return handleGetRoster(parts);
                case "RECORD_SCORE":
                    return handleRecordScore(parts);
                case "COMPUTE_FINAL_GRADE":
                    return handleComputeFinalGrade(parts);
                // -----------------------------
                
        default:
          return "ERROR:UNKNOWN_COMMAND";
      }
    } catch (Exception e) {
      // Catches exceptions thrown by services (e.g., business rules, DAO errors)
      System.err.println("SERVER EXCEPTION: " + e.getMessage());
      return "ERROR:" + e.getMessage();
    }
  }
  
    // ----------------------------------------------------------------------
  // --- NEW HANDLERS: INSTRUCTOR FUNCTIONALITY ---------------------------
  // ----------------------------------------------------------------------

    /**
     * Handles GET_INSTRUCTOR_SECTIONS. Fetches all sections taught by the instructor.
     * Command format: GET_INSTRUCTOR_SECTIONS:instructorId
     */
    private String handleGetInstructorSections(String[] parts) throws Exception {
        if (parts.length < 2) throw new Exception("Missing instructor ID.");
        
        try {
            int instructorId = Integer.parseInt(parts[1]);
            List<Section> sections = instructorService.getAssignedSections(instructorId);
            
            String jsonSections = gson.toJson(sections);
            return "SUCCESS:" + jsonSections;
        } catch (NumberFormatException e) {
            throw new Exception("Invalid instructor ID format provided.");
        }
    }

    /**
     * Handles GET_ROSTER. Fetches the roster for a specific section.
     * Command format: GET_ROSTER:instructorId:sectionId
     */
    private String handleGetRoster(String[] parts) throws Exception {
        if (parts.length < 3) throw new Exception("Missing section ID or instructor ID for roster request.");
        
        try {
            int instructorId = Integer.parseInt(parts[1]);
            int sectionId = Integer.parseInt(parts[2]);

            // Authorization check is performed inside the service before fetching data.
            List<EnrollmentRecord> rosterRecords = instructorService.getSectionRoster(instructorId, sectionId);
            
            String jsonRoster = gson.toJson(rosterRecords);
            return "SUCCESS:" + jsonRoster;
        } catch (NumberFormatException e) {
            throw new Exception("Invalid ID format provided.");
        }
    }

    /**
     * Handles RECORD_SCORE. Records or updates a single component score.
     * Command format: RECORD_SCORE:instructorId:enrollmentId:componentName:score
     */
    private String handleRecordScore(String[] parts) throws Exception {
        if (parts.length < 5) throw new Exception("Missing one or more required parameters for grade recording.");
        
        try {
            int instructorId = Integer.parseInt(parts[1]);
            int enrollmentId = Integer.parseInt(parts[2]);
            String componentName = parts[3];
            double score = Double.parseDouble(parts[4]);
            
            // Maintenance mode check and Authorization happen inside the service.
            instructorService.recordScore(instructorId, enrollmentId, componentName, score);
            
            return "SUCCESS:Score recorded successfully for " + componentName + ".";
        } catch (NumberFormatException e) {
            throw new Exception("Invalid ID or score format provided.");
        }
    }

    /**
     * Handles COMPUTE_FINAL_GRADE. Calculates final grade based on weights and records it.
     * Command format: COMPUTE_FINAL_GRADE:instructorId:enrollmentId
     */
    private String handleComputeFinalGrade(String[] parts) throws Exception {
        if (parts.length < 3) throw new Exception("Missing enrollment ID or instructor ID.");
        
        try {
            int instructorId = Integer.parseInt(parts[1]);
            int enrollmentId = Integer.parseInt(parts[2]);

            // Service handles maintenance check, calculation, and recording.
            String finalGrade = instructorService.computeAndRecordFinalGrade(instructorId, enrollmentId); 
            
            return "SUCCESS:Final grade (" + finalGrade + ") computed and recorded successfully.";
        } catch (NumberFormatException e) {
            throw new Exception("Invalid ID format provided.");
        }
    }
    
  // ----------------------------------------------------------------------
  // --- EXISTING HANDLERS (UNCHANGED) ------------------------------------
  // ----------------------------------------------------------------------
  
  private String handleDownloadTranscript(String[] parts) throws Exception {
   if (parts.length < 2) throw new Exception("Missing user ID for transcript request.");
  
      try {
      int userId = Integer.parseInt(parts[1]);
      StudentService studentService = new StudentService(); 
      
      String htmlContent = studentService.downloadTranscript(userId);
      
      return "FILE_DOWNLOAD:text/html:transcript_" + userId + ".html:" + htmlContent;

      } catch (NumberFormatException e) {
      throw new Exception("Invalid user ID format provided.");
      }
  }
  
  private String handleGetTimetable(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing user ID for timetable request.");
    
    try {
      int userId = Integer.parseInt(parts[1]);
      
      StudentService studentService = new StudentService();
      List<CourseCatalog> schedule = studentService.fetchTimetable(userId);
      
      String scheduleJson = gson.toJson(schedule);
      return "SUCCESS:" + scheduleJson;
      
    } catch (NumberFormatException e) {
      throw new Exception("Invalid user ID format provided.");
      }
  }

  private String handleDropCourse(String[] parts) throws Exception {
    if (parts.length < 3) throw new Exception("Missing user ID or section ID for drop request.");
    
    try {
      int userId = Integer.parseInt(parts[1]);
      int sectionId = Integer.parseInt(parts[2]);
      
      StudentService studentService = new StudentService();
      String message = studentService.dropCourse(userId, sectionId);
      
      return "SUCCESS:" + message;
      
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID format provided.");
      }
  }

  private String handleRegisterCourse(String[] parts) throws Exception {
    if (parts.length < 3) throw new Exception("Missing user ID or section ID for registration.");
    
    try {
      int userId = Integer.parseInt(parts[1]);
      int sectionId = Integer.parseInt(parts[2]);
      
      StudentService studentService = new StudentService();
      String message = studentService.registerCourse(userId, sectionId);
      
      return "SUCCESS:" + message;
      
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID format provided.");
      }
  }

  private String handleGetCatalog() throws Exception {
    StudentService studentService = new StudentService();
    List<CourseCatalog> catalog = studentService.fetchCourseCatalog();
    
    String catalogJson = gson.toJson(catalog); 
    return "SUCCESS:" + catalogJson;
  }

  private String handleLogin(String[] parts) throws Exception {
    if (parts.length < 3) throw new Exception("Missing username or password.");
    String username = parts[1];
    String password = parts[2];
    
    AuthService authService = new AuthService();
    UserAuth user = authService.authenticate(username, password);
    
    String userJson = gson.toJson(user);
    return "SUCCESS:" + userJson;
  }
  
  private String handleGetGrades(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing user ID for grades request.");
    
    int userId = Integer.parseInt(parts[1]); 

    StudentService studentService = new StudentService();
    List<Grade> grades = studentService.fetchGrades(userId);
    
    String gradesJson = gson.toJson(grades); 
    return "SUCCESS:" + gradesJson;
  }

  private String handleChangePassword(String[] parts) throws Exception {
    if (parts.length < 4) throw new Exception("Missing parameters for password change (ID, old, or new password).");
    
    int userId = Integer.parseInt(parts[1]);
    String oldPassword = parts[2];
    String newPassword = parts[3];

    AuthService authService = new AuthService();
    boolean success = authService.changePassword(userId, oldPassword, newPassword);

    if (success) {
             return "SUCCESS:Password successfully changed.";
    } else {
      return "ERROR:Failed to change password (Service returned false).";
    }
  }
}