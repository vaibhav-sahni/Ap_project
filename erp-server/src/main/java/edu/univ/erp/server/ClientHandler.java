package edu.univ.erp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import edu.univ.erp.dao.auth.AuthDAO; 
import edu.univ.erp.dao.settings.SettingDAO;
import edu.univ.erp.domain.CourseCatalog;
import edu.univ.erp.domain.EnrollmentRecord;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section; // NEW: Instructor's list of sections
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.service.admin.AdminService;
import edu.univ.erp.service.admin.MysqldumpBackupService;
import edu.univ.erp.service.auth.AuthService;
import edu.univ.erp.service.instructor.InstructorService;
import edu.univ.erp.service.student.StudentService;
import edu.univ.erp.util.MailUtil;

public class ClientHandler implements Runnable {
private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
private final Socket clientSocket;
private final Gson gson = new Gson();
private final SettingDAO settingDAO = new SettingDAO(); 
    
private final InstructorService instructorService = new InstructorService();
private final AdminService adminService = new AdminService();

  // Per-connection authenticated user (replaces global SessionManager usage inside this handler)
  private edu.univ.erp.domain.UserAuth currentUser = null;

  // ----------------- Session / Authorization helpers -----------------
  private edu.univ.erp.domain.UserAuth requireAuthenticated() throws Exception {
    if (this.currentUser == null) {
      throw new Exception("NOT_AUTHENTICATED:Login required to perform this action.");
    }
    return this.currentUser;
  }

  private void requireAdmin(edu.univ.erp.domain.UserAuth current) throws Exception {
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (!checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only admins may perform this action.");
    }
  }

  private void requireSameUserOrAdmin(edu.univ.erp.domain.UserAuth current, int userId) throws Exception {
    if (current.getUserId() != userId) {
      requireAdmin(current);
    }
  }


public ClientHandler(Socket socket) { this.clientSocket = socket; }
 @Override
  public void run() { 
    // Apply a socket read timeout to avoid handler threads blocking forever on dead clients
    try {
      int timeoutMs = Integer.parseInt(System.getProperty("erp.socketReadTimeoutMs", "300000")); // default 5 minutes
      clientSocket.setSoTimeout(timeoutMs);
    } catch (Exception ignore) {}

    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
    ) {
      // Keep reading requests until null
             String request;
      while (true) {
        try {
          request = in.readLine();
        } catch (java.net.SocketTimeoutException ste) {
          try { LOGGER.log(Level.INFO, "ClientHandler read timeout from " + clientSocket.getRemoteSocketAddress()); } catch (Exception ignore) { LOGGER.log(Level.INFO, "ClientHandler read timeout"); }
          break; // exit loop and close connection
        }
        if (request == null) break;
         String response = processRequest(request);
         out.println(response);
       }

    } catch (java.net.SocketException se) {
      // Common client disconnects (connection reset, closed by peer) — log compactly with remote address
      try {
        LOGGER.log(Level.INFO, "ClientHandler network disconnect from " + clientSocket.getRemoteSocketAddress() + ": " + se.getMessage());
      } catch (Exception ignore) {
        LOGGER.log(Level.INFO, "ClientHandler network disconnect: " + se.getMessage());
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "ClientHandler network error: " + e.getMessage(), e);
    } finally {
      // clear per-connection session on exit
      this.currentUser = null;
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
        // Block write operations during maintenance. Note: admin creation commands
        // are represented as top-level commands (CREATE_STUDENT, CREATE_INSTRUCTOR, CREATE_COURSE_SECTION).
        if (command.equals("REGISTER") || command.equals("DROP_SECTION") || 
          command.equals("RECORD_SCORE") || command.equals("COMPUTE_FINAL_GRADE") || // instructor grading
          command.equals("CREATE_STUDENT") || command.equals("CREATE_INSTRUCTOR") || command.equals("CREATE_COURSE_SECTION")) {
          
          LOGGER.warning(() -> "SERVER LOG: ACCESS DENIED: Command " + command + " blocked due to maintenance.");
          return "ERROR:MAINTENANCE_ON:The system is currently undergoing maintenance. Enrollment changes and grading operations are disabled.";
        }
      }
      
      // CENTRAL REQUEST ROUTER
  switch (command) {
        case "LOGIN":
          return handleLogin(parts);
        case "LOGOUT":
          return handleLogout(parts);
        case "GET_GRADES":
          return handleGetGrades(parts);
        case "GET_CATALOG": 
          return handleGetCatalog();
        case "GET_TIMETABLE":
          return handleGetTimetable(parts);
        case "GET_CGPA":
          return handleGetCgpa(parts);
        case "REGISTER": 
          return handleRegisterCourse(parts);
        case "DROP_SECTION": 
          return handleDropCourse(parts);
        case "CHANGE_PASSWORD": 
          return handleChangePassword(parts); 
        case "DOWNLOAD_TRANSCRIPT":
          return handleDownloadTranscript(parts);
        case "GET_INSTRUCTOR_SECTIONS":
          return handleGetInstructorSections(parts);
              case "RESET_PASSWORD":
                return handleResetPassword(parts);
              case "SET_ADMIN_EMAIL":
                return handleSetAdminEmail(parts);
        case "GET_ROSTER":
          return handleGetRoster(parts);
        case "RECORD_SCORE":
          return handleRecordScore(parts);
        case "COMPUTE_FINAL_GRADE":
          return handleComputeFinalGrade(parts);
        case "EXPORT_GRADES":
          return handleExportGrades(parts);
        case "IMPORT_GRADES":
          return handleImportGrades(parts);
        case "CREATE_STUDENT":
        return handleCreateStudent(parts);
  case "CREATE_COURSE_SECTION":
    return handleCreateCourseSection(parts);
  case "CREATE_COURSE":
    return handleCreateCourse(parts);
  case "CREATE_SECTION":
    return handleCreateSection(parts);
  case "TOGGLE_MAINTENANCE":
    return handleToggleMaintenance(parts);
  case "SET_DROP_DEADLINE":
    return handleSetDropDeadline(parts);
  case "CHECK_MAINTENANCE":
    return handleCheckMaintenance();
  case "DB_BACKUP":
    return handleDbBackup(parts);
  case "DB_RESTORE":
    return handleDbRestore(parts);
  case "GET_ALL_COURSES":
      return handleGetAllCourses();
    case "GET_ALL_INSTRUCTORS":
      return handleGetAllInstructors();
  case "GET_ALL_STUDENTS":
      return handleGetAllStudents();
  case "CREATE_INSTRUCTOR":
      return handleCreateInstructor(parts);
  case "REASSIGN_INSTRUCTOR":
    return handleReassignInstructor(parts);
  case "SEND_NOTIFICATION":
    return handleSendNotification(parts);
  case "GET_NOTIFICATIONS":
    return handleGetNotifications(parts);
  default:
      return "ERROR:UNKNOWN_COMMAND";
  }
    } catch (Exception e) {
      // Catches exceptions thrown by services (e.g., business rules, DAO errors)
      LOGGER.log(Level.SEVERE, "SERVER EXCEPTION: " + e.getMessage(), e);
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
    // Require authenticated session and ensure caller is the instructor or an admin
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int instructorId;
    try {
      instructorId = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid instructor ID format provided.");
    }
    

    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != instructorId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the instructor or admins may view assigned sections.");
    }

    List<Section> sections = instructorService.getAssignedSections(instructorId);
    String jsonSections = gson.toJson(sections);
    return "SUCCESS:" + jsonSections;
    }

/**
 * Handles CREATE_COURSE:code:title:credits
 */
private String handleCreateCourse(String[] parts) throws Exception {
  if (parts.length < 4) throw new Exception("Missing parameters. Expected CREATE_COURSE:code:title:credits");
  edu.univ.erp.domain.UserAuth current = requireAuthenticated();
  requireAdmin(current);

  String code = parts[1];
  String title = parts[2];
  int credits;
  try { credits = Integer.parseInt(parts[3]); } catch (NumberFormatException e) { throw new Exception("Invalid credits value."); }

  edu.univ.erp.domain.CourseCatalog course = new CourseCatalog(code, title, credits, 0, "", "", 0, 0, "", 0, 0, "");
  String message = adminService.createCourse(course);
  return "SUCCESS:" + message;
}

  /**
   * Handles RESET_PASSWORD:username:newPassword
   * This is an unauthenticated request that notifies the configured admin
   * by email containing the username and the requested new password. It does NOT change the DB.
   */
  private String handleResetPassword(String[] parts) throws Exception {
    if (parts.length < 3) throw new Exception("Missing parameters. Expected RESET_PASSWORD:username:newPassword");
    String username = parts[1];
    String newPassword = parts[2];

    if (username == null || username.trim().isEmpty()) throw new Exception("Username is required.");
    if (newPassword == null || newPassword.length() < 6) throw new Exception("New password must be at least 6 characters long.");

    // Verify user exists in auth DB
    AuthDAO authDao = new AuthDAO();
    var details = authDao.findUserByUsername(username);
    if (details == null) {
      throw new Exception("User not found.");
    }

    // Build email content
    String subject = "Password Reset Request for user: " + username;
    StringBuilder body = new StringBuilder();
    body.append("A password reset request was submitted from the client application.\n\n");
    body.append("Username: ").append(username).append("\n");
    body.append("Requested New Password: ").append(newPassword).append("\n\n");
    body.append("Please review and take any necessary action (do not forward the password).\n");

    try {
      MailUtil.sendEmailToAdmin(subject, body.toString());
    } catch (Exception e) {
      // Wrap email errors so the client sees a readable message
      throw new Exception("Failed to notify admin: " + e.getMessage());
    }

    return "SUCCESS:Admin notified";
  }

  /**
   * Admin-only handler to set the global admin email address used for password reset notifications.
   * Command: SET_ADMIN_EMAIL:email
   */
  private String handleSetAdminEmail(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing email parameter. Expected SET_ADMIN_EMAIL:email");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    requireAdmin(current);
    String email = parts[1];
    if (email == null || !email.contains("@")) throw new Exception("Invalid email format.");
    settingDAO.setSetting("ADMIN_EMAIL", email);
    return "SUCCESS:Admin email configured";
  }

/**
 * Handles CREATE_SECTION:courseCode:instructorId:dayTime:room:capacity:semester:year
 */
private String handleCreateSection(String[] parts) throws Exception {
  if (parts.length < 8) throw new Exception("Missing parameters. Expected CREATE_SECTION:courseCode:instructorId:dayTime:room:capacity:semester:year");
  edu.univ.erp.domain.UserAuth current = requireAuthenticated();
  requireAdmin(current);

  String courseCode = parts[1];
  int instrId;
  try { instrId = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { throw new Exception("Invalid instructor id."); }
  String dayTime = parts[3];
  // decode URL-encoded dayTime (client may encode to preserve ':' characters)
  try { dayTime = java.net.URLDecoder.decode(dayTime, java.nio.charset.StandardCharsets.UTF_8.toString()); } catch (IllegalArgumentException ex) { /* keep as-is */ }
  String room = parts[4];
  int capacity;
  try { capacity = Integer.parseInt(parts[5]); } catch (NumberFormatException e) { throw new Exception("Invalid capacity."); }
  String semester = parts[6];
  int year;
  try { year = Integer.parseInt(parts[7]); } catch (NumberFormatException e) { throw new Exception("Invalid year."); }

  CourseCatalog section = new CourseCatalog(courseCode, "", 0, 0, dayTime, room, capacity, 0, semester, year, instrId, "");
  String message = adminService.createSection(section);
  return "SUCCESS:" + message;
}

    /**
     * Handles GET_ROSTER. Fetches the roster for a specific section.
     * Command format: GET_ROSTER:instructorId:sectionId
     */
    private String handleGetRoster(String[] parts) throws Exception {
        if (parts.length < 3) throw new Exception("Missing section ID or instructor ID for roster request.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int instructorId;
    int sectionId;
    try {
      instructorId = Integer.parseInt(parts[1]);
      sectionId = Integer.parseInt(parts[2]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID format provided.");
    }
    // Only the assigned instructor or admin can view roster
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != instructorId && !checker.isAdmin(current.getUserId()) && !checker.isInstructorOfSection(instructorId, sectionId)) {
      throw new Exception("NOT_AUTHORIZED:Only the instructor or admins may view the roster.");
    }

    List<EnrollmentRecord> rosterRecords = instructorService.getSectionRoster(instructorId, sectionId);
    String jsonRoster = gson.toJson(rosterRecords);
    return "SUCCESS:" + jsonRoster;
    }

    /**
     * Handles RECORD_SCORE. Records or updates a single component score.
     * Command format: RECORD_SCORE:instructorId:enrollmentId:componentName:score
     */
    private String handleRecordScore(String[] parts) throws Exception {
        if (parts.length < 5) throw new Exception("Missing one or more required parameters for grade recording.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int instructorId;
    int enrollmentId;
    String componentName;
    double score;
    try {
      instructorId = Integer.parseInt(parts[1]);
      enrollmentId = Integer.parseInt(parts[2]);
      componentName = parts[3];
      score = Double.parseDouble(parts[4]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID or score format provided.");
    }

    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != instructorId && !checker.isAdmin(current.getUserId()) && !checker.isInstructorOfEnrollment(instructorId, enrollmentId)) {
      throw new Exception("NOT_AUTHORIZED:Only the instructor for this enrollment or admins may record scores.");
    }

    instructorService.recordScore(instructorId, enrollmentId, componentName, score);
    return "SUCCESS:Score recorded successfully for " + componentName + ".";
    }

    /**
     * Handles COMPUTE_FINAL_GRADE. Calculates final grade based on weights and records it.
     * Command format: COMPUTE_FINAL_GRADE:instructorId:enrollmentId
     */
    private String handleComputeFinalGrade(String[] parts) throws Exception {
        if (parts.length < 3) throw new Exception("Missing enrollment ID or instructor ID.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int instructorId;
    int enrollmentId;
    try {
      instructorId = Integer.parseInt(parts[1]);
      enrollmentId = Integer.parseInt(parts[2]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID format provided.");
    }

    // Authorization: only the assigned instructor or admin may compute final grade
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != instructorId && !checker.isAdmin(current.getUserId()) && !checker.isInstructorOfEnrollment(instructorId, enrollmentId)) {
      throw new Exception("NOT_AUTHORIZED:Only the instructor for this enrollment or admins may compute final grades.");
    }

    String finalGrade = instructorService.computeAndRecordFinalGrade(instructorId, enrollmentId);
    return "SUCCESS:Final grade (" + finalGrade + ") computed and recorded successfully.";
    }

  /**
   * Handles EXPORT_GRADES. Returns a base64-encoded CSV file content to avoid newlines in single-line protocol.
   * Command: EXPORT_GRADES:instructorId:sectionId
   */
  private String handleExportGrades(String[] parts) throws Exception {
    if (parts.length < 3) throw new Exception("Missing instructor ID or section ID for export.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int instructorId;
    int sectionId;
    try {
      instructorId = Integer.parseInt(parts[1]);
      sectionId = Integer.parseInt(parts[2]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID format provided.");
    }

    // Only assigned instructor or admin may export
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != instructorId && !checker.isAdmin(current.getUserId()) && !checker.isInstructorOfSection(instructorId, sectionId)) {
      throw new Exception("NOT_AUTHORIZED:Only the instructor or admins may export grades for this section.");
    }

    String csv = instructorService.exportGradesCsv(instructorId, sectionId);
    String base64 = Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8));
    // Return as file download with a base64 payload
    return "FILE_DOWNLOAD:text/csv:grades_section_" + sectionId + ".csv:BASE64:" + base64;
  }

  /**
   * Handles IMPORT_GRADES. Expects a base64-encoded CSV string to avoid multi-line transport.
   * Command: IMPORT_GRADES:instructorId:sectionId:BASE64:<payload>
   */
  private String handleImportGrades(String[] parts) throws Exception {
    if (parts.length < 5) throw new Exception("Missing parameters for import (instructorId, sectionId, BASE64, payload).");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int instructorId;
    int sectionId;
    try {
      instructorId = Integer.parseInt(parts[1]);
      sectionId = Integer.parseInt(parts[2]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID format provided.");
    }
    if (!"BASE64".equalsIgnoreCase(parts[3])) {
      throw new Exception("Unsupported payload encoding. Expected BASE64.");
    }
    String base64 = parts[4];
    // If the payload might contain colons, join remaining parts
    if (parts.length > 5) {
      StringBuilder sb = new StringBuilder(base64);
      for (int i = 5; i < parts.length; i++) {
        sb.append(":").append(parts[i]);
      }
      base64 = sb.toString();
    }

    // Authorization
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != instructorId && !checker.isAdmin(current.getUserId()) && !checker.isInstructorOfSection(instructorId, sectionId)) {
      throw new Exception("NOT_AUTHORIZED:Only the instructor or admins may import grades for this section.");
    }

  byte[] decoded = Base64.getDecoder().decode(base64);
  String csv = new String(decoded, StandardCharsets.UTF_8);

  LOGGER.info(() -> "IMPORT_GRADES invoked by user " + current.getUserId() + " for instructorId=" + instructorId + " sectionId=" + sectionId + " payloadBytes=" + decoded.length);

  String summary = instructorService.importGradesCsv(instructorId, sectionId, csv);
  LOGGER.info(() -> "IMPORT_GRADES completed for instructorId=" + instructorId + " sectionId=" + sectionId + " summary=" + summary);
    // The summary may contain newlines; encode as JSON string so the single-line protocol is preserved
    return "SUCCESS:" + gson.toJson(summary);
  }

    
    
  // ----------------------------------------------------------------------
  // --- EXISTING HANDLERS (UNCHANGED) ------------------------------------
  // ----------------------------------------------------------------------
  
  private String handleDownloadTranscript(String[] parts) throws Exception {
   if (parts.length < 2) throw new Exception("Missing user ID for transcript request.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int userId;
    try {
      userId = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid user ID format provided.");
    }
    // Allow either the user themselves or an admin to download the transcript
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != userId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the student or admins may download this transcript.");
    }

    StudentService studentService = new StudentService(); 
    String htmlContent = studentService.downloadTranscript(userId);
    return "FILE_DOWNLOAD:text/html:transcript_" + userId + ".html:" + htmlContent;
  }
  
  private String handleGetTimetable(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing user ID for timetable request.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int userId;
    try {
      userId = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid user ID format provided.");
    }
    // allow owner or admin
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != userId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the user or admins may fetch the timetable.");
    }

    StudentService studentService = new StudentService();
    List<CourseCatalog> schedule = studentService.fetchTimetable(userId);
    String scheduleJson = gson.toJson(schedule);
    return "SUCCESS:" + scheduleJson;
  }

  /**
   * Handles GET_CGPA:userId
   * Returns JSON payload: { "cgpa": <number|null>, "totalCreditsEarned": <number> }
   */
  private String handleGetCgpa(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing user ID for CGPA request.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int userId;
    try {
      userId = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid user ID format provided.");
    }

    // allow owner or admin
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != userId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the user or admins may fetch the CGPA.");
    }

  StudentService studentService = new StudentService();
  double cgpa = studentService.computeCgpa(userId);
  double creditsEarned = studentService.computeTotalCreditsEarned(userId);

  // suppressed info log for CGPA requests to reduce verbosity

    // Build JSON manually to avoid Gson edge-cases with local classes or null serialization.
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("\"cgpa\":");
    if (Double.isNaN(cgpa)) sb.append("null"); else sb.append(String.format(java.util.Locale.ROOT, "%.2f", cgpa));
    sb.append(',');
    sb.append("\"totalCreditsEarned\":");
    sb.append(String.format(java.util.Locale.ROOT, "%.2f", creditsEarned));
    sb.append('}');
    String json = sb.toString();
    return "SUCCESS:" + json;
  }

  /**
   * Handles DB_BACKUP. Only admins may invoke. Returns a gzipped SQL dump as BASE64 file download.
   * Command: DB_BACKUP
   */
  private String handleDbBackup(String[] parts) throws Exception {
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    requireAdmin(current);

    MysqldumpBackupService backupService = new MysqldumpBackupService();
    Path gz = backupService.createGzippedBackup();
    if (gz == null) throw new Exception("DB_BACKUP_FAILED:Backup service returned no file.");
    try {
      byte[] gzippedDump = Files.readAllBytes(gz);
      if (gzippedDump == null || gzippedDump.length == 0) {
        throw new Exception("DB_BACKUP_FAILED:Empty backup payload produced.");
      }
      // compute SHA-256 fingerprint for audit
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(gzippedDump);
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) sb.append(String.format("%02x", b));
      String sha256 = sb.toString();
      long size = gzippedDump.length;
      // append audit line
      String auditLine = java.time.Instant.now().toString() + " | user=" + current.getUserId() + " | OP=DB_BACKUP | sha256=" + sha256 + " | size=" + size + java.lang.System.lineSeparator();
      try {
        java.nio.file.Path audit = java.nio.file.Paths.get("db_backup_audit.log");
        java.nio.file.Files.writeString(audit, auditLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
      } catch (Exception ignore) { LOGGER.log(Level.WARNING, "Failed to write DB backup audit: " + ignore.getMessage()); }

      String base64 = Base64.getEncoder().encodeToString(gzippedDump);
      return "FILE_DOWNLOAD:application/gzip:erp_backup.gz:BASE64:" + base64;
    } finally {
      try { Files.deleteIfExists(gz); } catch (Exception ignore) {}
    }
  }

  /**
   * Handles DB_RESTORE. Only admins may invoke. Accepts a BASE64 gzipped SQL dump and restores it.
   * Command: DB_RESTORE:BASE64:<payload>
   */
  private String handleDbRestore(String[] parts) throws Exception {
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    requireAdmin(current);
    if (parts.length < 3) throw new Exception("Missing payload encoding for DB_RESTORE. Expected BASE64 and payload.");
    if (!"BASE64".equalsIgnoreCase(parts[1])) throw new Exception("Unsupported payload encoding. Expected BASE64.");

    String base64 = parts[2];
    if (parts.length > 3) {
      StringBuilder sb = new StringBuilder(base64);
      for (int i = 3; i < parts.length; i++) sb.append(":").append(parts[i]);
      base64 = sb.toString();
    }

    byte[] gzipped = Base64.getDecoder().decode(base64);
    MysqldumpBackupService backupService = new MysqldumpBackupService();

    // Require server-side maintenance mode ON for restore
    if (!settingDAO.isMaintenanceModeOn()) {
      throw new Exception("NOT_ALLOWED:DB_RESTORE requires maintenance mode to be ON on the server.");
    }

    Path tmp = Files.createTempFile("erp-restore-client-", ".sql.gz");
    try {
      Files.write(tmp, gzipped);
      // compute SHA-256 for audit
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(gzipped);
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) sb.append(String.format("%02x", b));
      String sha256 = sb.toString();
      long size = gzipped.length;
      // append audit entry (pre-restore)
      String auditLine = java.time.Instant.now().toString() + " | user=" + current.getUserId() + " | OP=DB_RESTORE | sha256=" + sha256 + " | size=" + size + java.lang.System.lineSeparator();
      try {
        java.nio.file.Path audit = java.nio.file.Paths.get("db_backup_audit.log");
        java.nio.file.Files.writeString(audit, auditLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
      } catch (Exception ignore) { LOGGER.log(Level.WARNING, "Failed to write DB restore audit: " + ignore.getMessage()); }

      backupService.restoreFromGzippedDump(tmp);
    } finally {
      try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
    }
    return "SUCCESS:DB restore completed.";
  }

  private String handleDropCourse(String[] parts) throws Exception {
    if (parts.length < 3) throw new Exception("Missing user ID or section ID for drop request.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int userId;
    int sectionId;
    try {
      userId = Integer.parseInt(parts[1]);
      sectionId = Integer.parseInt(parts[2]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID format provided.");
    }
    // only the same student or admin may drop
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != userId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the student or admins may drop a course.");
    }

    StudentService studentService = new StudentService();
    String message = studentService.dropCourse(current.getUserId(), userId, sectionId);
    return "SUCCESS:" + message;
  }

  private String handleRegisterCourse(String[] parts) throws Exception {
    if (parts.length < 3) throw new Exception("Missing user ID or section ID for registration.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int userId;
    int sectionId;
    try {
      userId = Integer.parseInt(parts[1]);
      sectionId = Integer.parseInt(parts[2]);
    } catch (NumberFormatException e) {
      throw new Exception("Invalid ID format provided.");
    }
    // only the same student or admin may register
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != userId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the student or admins may register for a course.");
    }

    StudentService studentService = new StudentService();
    String message = studentService.registerCourse(current.getUserId(), userId, sectionId);
    return "SUCCESS:" + message;
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
  // set the per-connection authenticated user for this handler
  this.currentUser = user;
    
    String userJson = gson.toJson(user);
    return "SUCCESS:" + userJson;
  }
  
  private String handleGetGrades(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing user ID for grades request.");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int userId = Integer.parseInt(parts[1]); 
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != userId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the user or admins may fetch grades.");
    }

    StudentService studentService = new StudentService();
    List<Grade> grades = studentService.fetchGrades(userId);
    String gradesJson = gson.toJson(grades); 
    return "SUCCESS:" + gradesJson;
  }

  /**
   * Handles LOGOUT command which clears the per-connection authenticated user.
   * Command format: LOGOUT
   */
  private String handleLogout(String[] parts) throws Exception {
    // No parameters expected
    this.currentUser = null;
    return "SUCCESS:Logged out";
  }

  private String handleChangePassword(String[] parts) throws Exception {
    if (parts.length < 4) throw new Exception("Missing parameters for password change (ID, old, or new password).");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int userId = Integer.parseInt(parts[1]);
    String oldPassword = parts[2];
    String newPassword = parts[3];
    // allow user themselves or admin to change password
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != userId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the user or admins may change this password.");
    }

    AuthService authService = new AuthService();
    boolean success = authService.changePassword(userId, oldPassword, newPassword);
    if (success) {
      return "SUCCESS:Password successfully changed.";
    } else {
      return "ERROR:Failed to change password (Service returned false).";
    }
  }



  private String handleCreateStudent(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing student payload.");
    // only admins can create students
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (!checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only admins may create students.");
    }

    // parts[0] = CREATE_STUDENT
    int userId = Integer.parseInt(parts[1]);
    String username = parts[2];
    String role = parts[3];
    String rollNo = parts[4];
    String program = parts[5];
    int year = Integer.parseInt(parts[6]);
    String password = parts[7];

    // If client sent 0 for userId, allocate next available id server-side
    if (userId == 0) {
      userId = adminService.getNextUserId();
    }

    // Construct the Student domain object manually
    Student student = new Student(userId, username, role, rollNo, program, year);

    // Call the service
    String message = adminService.createStudent(student, password);
    return "SUCCESS:" + message;

}
private String handleCreateCourseSection(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing course/section payload.");
    // only admins
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (!checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only admins may create course sections.");
    }
    
    /*String request = String.format("CREATE_COURSE_SECTION:%s:%s:%d:%d:%s:%d:%s:%d:%s:%d:%s:%d",
            course.getCourseCode(),
            course.getCourseTitle(),
            course.getCredits(),
            course.getSectionId(),
            course.getRoom(),
            course.getCapacity(),
            course.getDayTime(),
            course.getEnrolledCount(),
            course.getSemester(),
            course.getInstructorId(),
            course.getInstructorName(),
            course.getYear() */
    String code = parts[1];
    String title = parts[2];
    int credits = Integer.parseInt(parts[3]);
    int sectionID = Integer.parseInt(parts[4]);
  String room = parts[5];
  int capacity = Integer.parseInt(parts[6]);
  String dayTime = parts[7];
  // decode URL-encoded dayTime (client encodes to preserve ':' characters)
  try { dayTime = java.net.URLDecoder.decode(dayTime, java.nio.charset.StandardCharsets.UTF_8.toString()); } catch (IllegalArgumentException ex) { /* keep as-is */ }
    int enrolledCount = Integer.parseInt(parts[8]);
    String semester = parts[9];
    int instructorId = Integer.parseInt(parts[10]);
    String instructor = parts[11];
    int year = Integer.parseInt(parts[12]);

    CourseCatalog course = new CourseCatalog(code,title,credits,sectionID,dayTime,room,capacity,enrolledCount,semester,year,instructorId,instructor);
    String message = adminService.createCourseAndSection(course);
    return "SUCCESS:" + message;
}

private String handleToggleMaintenance(String[] parts) throws Exception {
    if (parts.length < 2) throw new Exception("Missing maintenance toggle parameter.");
  edu.univ.erp.domain.UserAuth current = requireAuthenticated();
  requireAdmin(current);

  boolean on = parts[1].equalsIgnoreCase("ON");
  adminService.toggleMaintenance(on);
  // Best-effort: create a broadcast notification announcing maintenance mode change
  try {
    String title = on ? "Maintenance mode ON" : "Maintenance mode OFF";
    String message = on
        ? "The system is now under maintenance. Enrollment changes and grading operations are temporarily disabled."
        : "Maintenance has ended. Normal operations have resumed.";
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    edu.univ.erp.domain.Notification n = new edu.univ.erp.domain.Notification(0, current.getUserId(), "ALL", 0, title, message, now, false);
    edu.univ.erp.dao.notification.NotificationDAO dao = new edu.univ.erp.dao.notification.NotificationDAO();
    boolean ok = dao.insertNotification(n);
    if (!ok) {
      LOGGER.log(Level.WARNING, "Failed to persist maintenance notification (insert returned false)");
    }
  } catch (Exception ex) {
    // Non-fatal: log and continue. The maintenance toggle should not fail because notifications couldn't be saved.
    LOGGER.log(Level.WARNING, "Non-fatal: failed to create maintenance notification: " + ex.getMessage(), ex);
  }

  return "SUCCESS:Maintenance mode turned " + (on ? "ON" : "OFF");
}

private String handleCheckMaintenance(){
  boolean status = adminService.isMaintenanceModeOn();
  return "SUCCESS:" + (status ? "ON" : "OFF");
}

private String handleGetAllCourses() throws Exception {
  edu.univ.erp.domain.UserAuth current = requireAuthenticated();
  requireAdmin(current);

  List<CourseCatalog> courses = adminService.getAllCourses();
  String json = gson.toJson(courses);
  return "SUCCESS:" + json;
}


private String handleGetAllStudents() throws Exception {
  edu.univ.erp.domain.UserAuth current = requireAuthenticated();
  requireAdmin(current);

  List<Student> students = adminService.getAllStudents();
  String json = gson.toJson(students);
  return "SUCCESS:" + json;
}

private String handleGetAllInstructors() throws Exception {
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    requireAdmin(current);
  java.util.List<java.util.Map<String,Object>> list = adminService.getAllInstructors();
  String json = gson.toJson(list);
    return "SUCCESS:" + json;
}

private String handleCreateInstructor(String[] parts) throws Exception {
  // Support two formats:
  // 1) CREATE_INSTRUCTOR:userId:username:role:name:department:password
  // 2) CREATE_INSTRUCTOR:username:role:name:department:password  (server will allocate userId)
  edu.univ.erp.domain.UserAuth current = requireAuthenticated();
  requireAdmin(current);

  String username, role, name, department, password;
  int userId = 0;

  if (parts.length == 7) {
    try { userId = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { userId = 0; }
    username = parts[2];
    role = parts[3];
    name = parts[4];
    department = parts[5];
    password = parts[6];
  } else if (parts.length == 6) {
    // no userId provided
    username = parts[1];
    role = parts[2];
    name = parts[3];
    department = parts[4];
    password = parts[5];
  } else {
    throw new Exception("Incomplete instructor creation request.");
  }

  if (userId == 0) {
    userId = adminService.getNextUserId();
  }

  Instructor instructor = new Instructor(userId, username, role, department);
  instructor.setName(name);

  String message = adminService.createInstructor(instructor, password);
  return "SUCCESS:" + message;
}

/**
 * Handles REASSIGN_INSTRUCTOR:sectionId:newInstructorId
 * Only admins may perform this action.
 */
private String handleReassignInstructor(String[] parts) throws Exception {
  if (parts.length < 3) throw new Exception("Missing parameters. Expected REASSIGN_INSTRUCTOR:sectionId:newInstructorId");
  edu.univ.erp.domain.UserAuth current = requireAuthenticated();
  requireAdmin(current);

  int sectionId;
  int newInstructorId;
  try {
    sectionId = Integer.parseInt(parts[1]);
    newInstructorId = Integer.parseInt(parts[2]);
  } catch (NumberFormatException e) {
    throw new Exception("Invalid numeric parameter provided.");
  }

  String msg = adminService.reassignInstructor(sectionId, newInstructorId);
  return "SUCCESS:" + msg;
}

  /**
   * Handles admin request to set the global drop deadline.
   * Command format: SET_DROP_DEADLINE:YYYY-MM-DD
   */
  private String handleSetDropDeadline(String[] parts) throws Exception {
    // Expecting: SET_DROP_DEADLINE:YYYY-MM-DD
    if (parts.length < 2) throw new Exception("Missing parameters. Expected SET_DROP_DEADLINE:YYYY-MM-DD");
    String isoDate = parts[1];

    // Block changes while maintenance mode is ON
    if (settingDAO.isMaintenanceModeOn()) {
        return "ERROR:MAINTENANCE_ON:Cannot change settings while maintenance is ON.";
    }

  edu.univ.erp.domain.UserAuth current = requireAuthenticated();
  requireAdmin(current);

  this.adminService.setDropDeadline(isoDate);
  return "SUCCESS:Drop deadline set to " + isoDate;
  }
  /**
   * Handles SEND_NOTIFICATION:recipientType:recipientId:BASE64:<payload>
   * payload JSON: { "title": "...", "message": "..." }
   */
  private String handleSendNotification(String[] parts) throws Exception {
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    requireAdmin(current);
    if (parts.length < 5) throw new Exception("Missing parameters for SEND_NOTIFICATION. Expected SEND_NOTIFICATION:recipientType:recipientId:BASE64:<payload>");
    String recipientType = parts[1];
    int recipientId;
    try { recipientId = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { throw new Exception("Invalid recipientId"); }
    if (!"BASE64".equalsIgnoreCase(parts[3])) throw new Exception("Unsupported payload encoding. Expected BASE64.");
    String base64 = parts[4];
    if (parts.length > 5) {
      StringBuilder sb = new StringBuilder(base64);
      for (int i = 5; i < parts.length; i++) sb.append(":").append(parts[i]);
      base64 = sb.toString();
    }
    byte[] decoded = java.util.Base64.getDecoder().decode(base64);
    String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
  com.google.gson.Gson gson = new com.google.gson.Gson();
  @SuppressWarnings("unchecked")
  java.util.Map<String,String> map = (java.util.Map<String,String>) gson.fromJson(json, java.util.Map.class);
    String title = map.getOrDefault("title", "(no title)");
    String message = map.getOrDefault("message", "");

    // Build notification and insert (use LocalDateTime for DB Timestamp)
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    edu.univ.erp.domain.Notification n = new edu.univ.erp.domain.Notification(0, current.getUserId(), recipientType, recipientId, title, message, now, false);
    edu.univ.erp.dao.notification.NotificationDAO dao = new edu.univ.erp.dao.notification.NotificationDAO();
    boolean ok = dao.insertNotification(n);
    if (!ok) throw new Exception("Failed to persist notification");
    return "SUCCESS:Notification sent";
  }

  /**
   * Handles GET_NOTIFICATIONS:userId:recipientType:limit
   */
  private String handleGetNotifications(String[] parts) throws Exception {
    if (parts.length < 4) throw new Exception("Missing parameters. Expected GET_NOTIFICATIONS:userId:recipientType:limit");
    edu.univ.erp.domain.UserAuth current = requireAuthenticated();
    int userId;
    try { userId = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { throw new Exception("Invalid userId"); }
    String recipientType = parts[2];
    int limit;
    try { limit = Integer.parseInt(parts[3]); } catch (NumberFormatException e) { limit = 10; }

    // allow the user themselves or admins
    edu.univ.erp.access.AccessChecker checker = new edu.univ.erp.access.AccessChecker();
    if (current.getUserId() != userId && !checker.isAdmin(current.getUserId())) {
      throw new Exception("NOT_AUTHORIZED:Only the user or admins may fetch notifications.");
    }

    edu.univ.erp.dao.notification.NotificationDAO dao = new edu.univ.erp.dao.notification.NotificationDAO();
    java.util.List<edu.univ.erp.domain.Notification> list = dao.fetchRecentForUser(userId, recipientType, limit);
    // Convert server Notification objects to simple maps, serializing timestamp as ISO string
    java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
    for (edu.univ.erp.domain.Notification nn : list) {
      java.util.Map<String,Object> m = new java.util.HashMap<>();
      m.put("id", nn.getId());
      m.put("senderId", nn.getSenderId());
      m.put("recipientType", nn.getRecipientType());
      m.put("recipientId", nn.getRecipientId());
      m.put("title", nn.getTitle());
      m.put("message", nn.getMessage());
      java.time.LocalDateTime ts = nn.getTimestamp();
      m.put("timestamp", ts != null ? ts.toString() : null);
      m.put("read", nn.isRead());
      out.add(m);
    }
    String json = new com.google.gson.Gson().toJson(out);
    return "SUCCESS:" + json;
  }
}