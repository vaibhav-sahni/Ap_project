package edu.univ.erp.access;

import edu.univ.erp.dao.auth.AuthDAO;
import edu.univ.erp.dao.instructor.InstructorDAO;

/**
 * Centralized utility for performing authorization checks across services.
 * This class ensures a user has the correct role and association 
 * (e.g., is the correct instructor for a section) before sensitive actions.
 */
public class AccessChecker {

    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final AuthDAO authDAO = new AuthDAO();

    /**
     * Checks if the given instructor ID is assigned to teach the specified section.
     */
    public boolean isInstructorOfSection(int instructorId, int sectionId) {
        try {
            return instructorDAO.isInstructorAssigned(instructorId, sectionId);
        } catch (Exception e) {
            System.err.println("AccessChecker DB error in isInstructorOfSection: " + e.getMessage());
            return false; // Fail safe on error
        }
    }

    /**
     * Checks if the given instructor is assigned to the section associated 
     * with the enrollment record. This is crucial for grade recording.
     */
    public boolean isInstructorOfEnrollment(int instructorId, int enrollmentId) {
        try {
            return instructorDAO.isInstructorOfEnrollmentRecord(instructorId, enrollmentId);
        } catch (Exception e) {
            System.err.println("AccessChecker DB error in isInstructorOfEnrollment: " + e.getMessage());
            return false; // Fail safe on error
        }
    }

    /**
     * Checks if the given user ID has the 'ADMIN' role.
     */
    public boolean isAdmin(int userId) {
        try {
            AuthDAO.AuthDetails details = authDAO.findUserByUserId(userId);
            return details != null && "ADMIN".equalsIgnoreCase(details.role());
        } catch (Exception e) {
            System.err.println("AccessChecker error in isAdmin for userId " + userId + ": " + e.getMessage());
            return false;
        }
    }
}
