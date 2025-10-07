package edu.univ.erp.service.instructor;

import java.sql.SQLException;
import java.util.List;

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
}
