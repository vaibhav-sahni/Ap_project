package edu.univ.erp.access;

import edu.univ.erp.dao.auth.AuthDAO;
import edu.univ.erp.dao.instructor.InstructorDAO;

/**
 Centralized utility for performing authorization checks across services.
 */
public class AccessChecker {

    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final AuthDAO authDAO = new AuthDAO();

    /*
      Checks if the given instructor ID is assigned to teach the specified section.
     */
    public boolean isInstructorOfSection(int instructorId, int sectionId) {
        try {
            return instructorDAO.isInstructorAssigned(instructorId, sectionId);
        } catch (Exception e) {
            // Log and surface the DB error so callers don't treat DB failures as simple 'not authorized'
            java.util.logging.Logger.getLogger(AccessChecker.class.getName()).severe("DB error in isInstructorOfSection: " + e.getMessage());
            throw new RuntimeException("DB_ERROR:Failed to verify instructor assignment: " + e.getMessage(), e);
        }
    }

    /*
      Checks if the given instructor is assigned to the section associated with the enrollment record.
     */
    public boolean isInstructorOfEnrollment(int instructorId, int enrollmentId) {
        try {
            return instructorDAO.isInstructorOfEnrollmentRecord(instructorId, enrollmentId);
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(AccessChecker.class.getName()).severe("DB error in isInstructorOfEnrollment: " + e.getMessage());
            throw new RuntimeException("DB_ERROR:Failed to verify instructor enrollment: " + e.getMessage(), e);
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
            java.util.logging.Logger.getLogger(AccessChecker.class.getName()).severe("DB error in isAdmin for userId " + userId + ": " + e.getMessage());
            throw new RuntimeException("DB_ERROR:Failed to verify admin role: " + e.getMessage(), e);
        }
    }
}
