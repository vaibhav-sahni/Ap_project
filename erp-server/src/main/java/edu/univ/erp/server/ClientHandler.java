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
import edu.univ.erp.service.auth.AuthService;
import edu.univ.erp.service.student.StudentService;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Gson gson = new Gson();
    private final SettingDAO settingDAO = new SettingDAO(); 

    public ClientHandler(Socket socket) { this.clientSocket = socket; }

    @Override
    public void run() { 
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true) 
        ) {
            String request = in.readLine(); 
            String response = processRequest(request);
            out.println(response);

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
                // List all commands that MUST be blocked during maintenance
                if (command.equals("REGISTER") || command.equals("DROP_SECTION") || 
                    command.equals("ENTER_SCORE") || command.equals("ADMIN:CREATE_USER") ||
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
                case "GET_TIMETABLE": // <-- NEW ROUTE: Get Student Schedule
                    return handleGetTimetable(parts);
                case "REGISTER": 
                    return handleRegisterCourse(parts);
                case "DROP_SECTION": 
                    return handleDropCourse(parts);
                case "CHANGE_PASSWORD": 
                    return handleChangePassword(parts); 
                // --- NEW: Route the transcript command to the handler ---
                case "DOWNLOAD_TRANSCRIPT":
                    return handleDownloadTranscript(parts);
                // --------------------------------------------------------
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
    // --- NEW HANDLER: DOWNLOAD_TRANSCRIPT ---------------------------------
    // ----------------------------------------------------------------------
    
    /**
     * Handles the DOWNLOAD_TRANSCRIPT command. Calls the StudentService to generate the CSV.
     * Command format: DOWNLOAD_TRANSCRIPT:userId
     */
    private String handleDownloadTranscript(String[] parts) throws Exception {
       if (parts.length < 2) throw new Exception("Missing user ID for transcript request.");
    
    try {
        int userId = Integer.parseInt(parts[1]);
        StudentService studentService = new StudentService(); 
        
        // The service now returns HTML content
        String htmlContent = studentService.downloadTranscript(userId);
        
        // CRITICAL CHANGE: Set Content-Type to text/html and extension to .html
        return "FILE_DOWNLOAD:text/html:transcript_" + userId + ".html:" + htmlContent;

    } catch (NumberFormatException e) {
        throw new Exception("Invalid user ID format provided.");
    } catch (Exception e) {
        // Handle service-level exception
        return "ERROR:" + e.getMessage();
    }
    }
    

    // ----------------------------------------------------------------------
    // --- EXISTING HANDLERS ------------------------------------------------
    // ----------------------------------------------------------------------
    
    /**
     * Handles the GET_TIMETABLE command. Calls the StudentService to fetch the schedule.
     * Command format: GET_TIMETABLE:userId
     */
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

    /**
     * Handles the DROP_SECTION command. Calls the StudentService to perform the course drop.
     * Command format: DROP_SECTION:userId:sectionId
     */
    private String handleDropCourse(String[] parts) throws Exception {
        if (parts.length < 3) throw new Exception("Missing user ID or section ID for drop request.");
        
        try {
            int userId = Integer.parseInt(parts[1]);
            int sectionId = Integer.parseInt(parts[2]);
            
            StudentService studentService = new StudentService();
            // The service returns a success message or throws an Exception 
            String message = studentService.dropCourse(userId, sectionId);
            
            return "SUCCESS:" + message;
            
        } catch (NumberFormatException e) {
            throw new Exception("Invalid ID format provided.");
        }
    }

    /**
     * Handles the REGISTER command. Calls the StudentService to perform enrollment.
     * Command format: REGISTER:userId:sectionId
     */
    private String handleRegisterCourse(String[] parts) throws Exception {
        if (parts.length < 3) throw new Exception("Missing user ID or section ID for registration.");
        
        try {
            int userId = Integer.parseInt(parts[1]);
            int sectionId = Integer.parseInt(parts[2]);
            
            StudentService studentService = new StudentService();
            // The service returns a success message or throws an Exception on failure (e.g., Capacity full)
            String message = studentService.registerCourse(userId, sectionId);
            
            return "SUCCESS:" + message;
            
        } catch (NumberFormatException e) {
            throw new Exception("Invalid ID format provided.");
        }
    }

    /**
     * Handles the GET_CATALOG command. 
     * Command format: GET_CATALOG
     */
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