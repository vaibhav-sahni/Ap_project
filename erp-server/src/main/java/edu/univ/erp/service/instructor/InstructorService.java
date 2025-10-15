package edu.univ.erp.service.instructor;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import edu.univ.erp.access.AccessChecker;
import edu.univ.erp.dao.instructor.InstructorDAO;
import edu.univ.erp.domain.EnrollmentRecord;
import edu.univ.erp.domain.Section;

/**
 * Service layer for Instructor operations. Enforces access rules, 
 * maintenance mode checks (delegated to the ClientHandler), and 
 * handles the crucial grade calculation logic.
 */
public class InstructorService {
    
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final AccessChecker accessChecker = new AccessChecker(); 
    
    // FINAL GRADE WEIGHTING RULE: Quiz 15%, Assignment 20%, Midterm 30%, Endterm 35%
    private static final double W_QUIZ = 0.15;
    private static final double W_ASSIGNMENT = 0.20;
    private static final double W_MIDTERM = 0.30;
    private static final double W_ENDTERM = 0.35;
    
    
    /** Fetches all sections assigned to the instructor. */
    public List<Section> getAssignedSections(int instructorId) throws Exception {
        try {
            return instructorDAO.getSectionsByInstructorId(instructorId);
        } catch (SQLException e) {
            throw new Exception("Database error while fetching sections: " + e.getMessage());
        }
    }
    
    /** Fetches the roster for a section, but only if the instructor is assigned to it. 
     * Enforces Authorization (Who rule).
     */
    public List<EnrollmentRecord> getSectionRoster(int instructorId, int sectionId) throws Exception {
        // 1. Authorization Check: Must be the instructor of the section
        if (!accessChecker.isInstructorOfSection(instructorId, sectionId)) {
             throw new Exception("Access Denied: You are not authorized to view this section's roster.");
        }
        
        try {
            // NOTE: The previous method name was incorrect; the DAO method is getEnrollmentRoster
            return instructorDAO.getEnrollmentRoster(sectionId); 
        } catch (SQLException e) {
            throw new Exception("Database error while fetching roster: " + e.getMessage());
        }
    }
    
    /** Records a score component, enforcing authorization. */
    public void recordScore(int instructorId, int enrollmentId, String componentName, double score) throws Exception {
        // Maintenance Mode is assumed to be checked by the ClientHandler for this write operation.

        // 1. Authorization Check: Must be the instructor of the enrollment record
        if (!accessChecker.isInstructorOfEnrollment(instructorId, enrollmentId)) {
             throw new Exception("Access Denied: You cannot modify grades for this enrollment record.");
        }

        // 2. Validation
        if (score < 0 || score > 100) { 
            throw new Exception("Invalid score value. Score must be between 0 and 100.");
        }
        
        // 3. DAO call
        try {
            instructorDAO.recordGradeComponent(enrollmentId, componentName, score);
        } catch (SQLException e) {
            throw new Exception("Database error while recording score: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Catches validation error from DAO if componentName is invalid
            throw new Exception(e.getMessage());
        }
    }
    
    /** * Computes and records the final grade based on weighted average. 
     * FIX: Now uses the new getSingleEnrollmentRecord DAO method.
     */
    public String computeAndRecordFinalGrade(int instructorId, int enrollmentId) throws Exception {
        // Maintenance Mode is assumed to be checked by the ClientHandler for this write operation.

        // 1. Authorization Check
        if (!accessChecker.isInstructorOfEnrollment(instructorId, enrollmentId)) {
             throw new Exception("Access Denied: You cannot finalize grades for this enrollment.");
        }
        
        // 2. Fetch component scores
        EnrollmentRecord record;
        try {
            // Fetch the single record with all component scores set
            record = instructorDAO.getSingleEnrollmentRecord(enrollmentId);
            
            if (record == null) {
                throw new Exception("Enrollment record not found for ID: " + enrollmentId);
            }
        } catch (SQLException e) {
            throw new Exception("Error fetching component scores for calculation: " + e.getMessage());
        }

        // 3. Weighted Average Calculation
        // Use the 'Safe' getter methods (e.g., getQuizScoreSafe()) which return 0.0 if the score is null.
        double quizScore = record.getQuizScoreSafe(); 
        double assignmentScore = record.getAssignmentScoreSafe();
        double midtermScore = record.getMidtermScoreSafe();
        double endtermScore = record.getEndtermScoreSafe();
        
        // Calculate the final score using predefined weights
        // FIX: The last weight was incorrectly set to W_MIDTERM (0.30). Corrected to W_ENDTERM (0.35).
        double finalNumericScore = 
            (quizScore * W_QUIZ) +        // 15%
            (assignmentScore * W_ASSIGNMENT) + // 20%
            (midtermScore * W_MIDTERM) +    // 30%
            (endtermScore * W_ENDTERM);   // 35% (FIXED)
            
        // 4. Letter Grade Assignment (Example scale)
        String finalLetter;
        if (finalNumericScore >= 90.0) {
            finalLetter = "A";
        } else if (finalNumericScore >= 80.0) {
            finalLetter = "B";
        } else if (finalNumericScore >= 70.0) {
            finalLetter = "C";
        } else if (finalNumericScore >= 60.0) {
            finalLetter = "D";
        } else {
            finalLetter = "F";
        }
        
        // 5. Record final grade
        try {
            instructorDAO.updateFinalGrade(enrollmentId, finalLetter);
            instructorDAO.updateEnrollmentStatus(enrollmentId, "Completed");
        } catch (SQLException e) {
            throw new Exception("Database error while recording final grade: " + e.getMessage());
        }
        
        return finalLetter;
    }

    /**
     * Exports the roster and component scores for a section as CSV.
     * CSV columns: enrollmentId,studentId,studentName,rollNo,quiz,assignment,midterm,endterm,finalGrade
     */
    public String exportGradesCsv(int instructorId, int sectionId) throws Exception {
        // Authorization
        if (!accessChecker.isInstructorOfSection(instructorId, sectionId) && !accessChecker.isAdmin(instructorId)) {
            throw new Exception("NOT_AUTHORIZED:Only the instructor or admins may export grades for this section.");
        }

        List<EnrollmentRecord> roster;
        try {
            roster = getSectionRoster(instructorId, sectionId);
        } catch (Exception e) {
            throw new Exception("Failed to fetch roster for export: " + e.getMessage(), e);
        }

        // Export in import-friendly format: enrollmentId,quiz,assignment,midterm,endterm
        StringBuilder csv = new StringBuilder();
        csv.append("enrollmentId,quiz,assignment,midterm,endterm");
        for (EnrollmentRecord r : roster) {
            csv.append('\n')
               .append(r.getEnrollmentId()).append(',')
               .append(r.getQuizScore() == null ? "" : String.valueOf(r.getQuizScore())).append(',')
               .append(r.getAssignmentScore() == null ? "" : String.valueOf(r.getAssignmentScore())).append(',')
               .append(r.getMidtermScore() == null ? "" : String.valueOf(r.getMidtermScore())).append(',')
               .append(r.getEndtermScore() == null ? "" : String.valueOf(r.getEndtermScore()));
        }
        return csv.toString();
    }

    /**
     * Imports a CSV of grades for a section and records component scores.
     * Expected CSV columns: enrollmentId,quiz,assignment,midterm,endterm
     * Returns a summary report string.
     */
    public String importGradesCsv(int instructorId, int sectionId, String csvContent) throws Exception {
        if (!accessChecker.isInstructorOfSection(instructorId, sectionId) && !accessChecker.isAdmin(instructorId)) {
            throw new Exception("NOT_AUTHORIZED:Only the instructor or admins may import grades for this section.");
        }

        String[] lines = csvContent.split("\r?\n");
        if (lines.length < 2) return "No data to import.";

        // First pass: parse and validate all rows, collect updates per enrollment
        Map<Integer, Map<String, Double>> updates = new java.util.HashMap<>();
        int processed = 0;
        int errors = 0;
        StringBuilder errorDetails = new StringBuilder();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = line.split(",");
            try {
                int enrollmentId = Integer.parseInt(cols[0].trim());

                // Security: ensure this enrollment belongs to the section and instructor
                if (!instructorDAO.isInstructorOfEnrollmentRecord(instructorId, enrollmentId)) {
                    errors++;
                    errorDetails.append("Enrollment ").append(enrollmentId).append(": not authorized or not in section.\n");
                    continue;
                }

                Map<String, Double> comps = updates.computeIfAbsent(enrollmentId, k -> new java.util.HashMap<>());

                if (cols.length > 1 && !cols[1].trim().isEmpty()) {
                    double q = Double.parseDouble(cols[1].trim());
                    comps.put("Quiz", q);
                }
                if (cols.length > 2 && !cols[2].trim().isEmpty()) {
                    double a = Double.parseDouble(cols[2].trim());
                    comps.put("Assignment", a);
                }
                if (cols.length > 3 && !cols[3].trim().isEmpty()) {
                    double m = Double.parseDouble(cols[3].trim());
                    comps.put("Midterm", m);
                }
                if (cols.length > 4 && !cols[4].trim().isEmpty()) {
                    double e = Double.parseDouble(cols[4].trim());
                    comps.put("Endterm", e);
                }

                processed++;
            } catch (Exception ex) {
                errors++;
                errorDetails.append("Line ").append(i+1).append(": ").append(ex.getMessage()).append("\n");
            }
        }

        // If any parsing/validation errors occurred, abort before making DB changes
        if (errors > 0) {
            // Log detailed parsing errors for debugging
            java.util.logging.Logger.getLogger(InstructorService.class.getName()).warning("CSV import aborted: parsing errors detected for instructorId=" + instructorId + " sectionId=" + sectionId + "; details:\n" + errorDetails.toString());
            StringBuilder summary = new StringBuilder();
            summary.append("Parsed: ").append(processed).append(", Errors: ").append(errors);
            summary.append("\nDetails:\n").append(errorDetails.toString());
            return summary.toString();
        }

        // Second pass: apply all updates atomically using DAO transaction
        try {
            instructorDAO.applyGradeUpdatesTransactional(updates);
        } catch (SQLException e) {
            throw new Exception("Database error while applying grade updates: " + e.getMessage());
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Imported and applied updates for ").append(processed).append(" rows.");

        // Audit logging: record a minimal fingerprint of the imported CSV and who performed the import.
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            String csvSha = hex.toString();

            String auditLine = String.format("%s | actor=%d | instructor=%d | section=%d | rows=%d | sha256=%s\n",
                    java.time.Instant.now().toString(), /* actorUserId to be filled by caller? */ -1, instructorId, sectionId, processed, csvSha);
            java.nio.file.Files.write(java.nio.file.Path.of("import_audit.log"), auditLine.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ex) {
            // don't fail the import because audit logging failed; just log it server-side
            java.util.logging.Logger.getLogger(InstructorService.class.getName()).warning("Failed to write import audit log: " + ex.getMessage());
        }
        return summary.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"") ) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
