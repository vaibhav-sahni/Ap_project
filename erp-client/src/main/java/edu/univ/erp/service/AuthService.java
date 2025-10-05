package edu.univ.erp.service;

import edu.univ.erp.auth.hash.PasswordHasher;
import edu.univ.erp.auth.session.SessionManager;     // NEW IMPORT
import edu.univ.erp.auth.store.AuthDAO;  // NEW IMPORT
import edu.univ.erp.auth.store.AuthDAO.AuthDetails;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.UserAuth;

/**
 * AuthService handles the core business logic for user authentication and profile loading.
 */
public class AuthService {

    private final AuthDAO authDAO = new AuthDAO();
    // NEW: Instantiate the profile DAOs
    private final StudentDAO studentDAO = new StudentDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();

    /**
     * Executes the secure login process, verifies credentials, and loads the full profile.
     * @param username The username entered by the user.
     * @param plaintextPassword The raw password entered by the user.
     * @return The complete UserAuth (or Student/Instructor) object upon successful authentication.
     * @throws Exception with a generic error message if login fails or profile is missing.
     */
    public UserAuth login(String username, String plaintextPassword) throws Exception {
        
        AuthDetails authDetails = authDAO.findUserByUsername(username);

        if (authDetails == null) {
            throw new Exception("Incorrect username or password."); 
        }
        
        boolean passwordMatches = PasswordHasher.verifyPassword(
            plaintextPassword, 
            authDetails.passwordHash()
        );

        if (!passwordMatches) {
            throw new Exception("Incorrect username or password."); 
        }

        // 1. Create the base UserAuth object
        UserAuth user = new UserAuth(
            authDetails.userID(), 
            username, 
            authDetails.role()
        );
        
        // ----------------------------------------------------
        // 2. MODIFIED: Load the full ERP Profile based on role
        // ----------------------------------------------------
        UserAuth fullProfile = loadProfile(user);
        
        if (fullProfile == null) {
             // This is a critical error: Authentication succeeded, but profile data is missing.
             throw new Exception("Authentication successful, but ERP profile not found. Contact Admin.");
        }
        
        // 3. MODIFIED: Establish the session with the FULL Profile (Student or Instructor object)
        SessionManager.setCurrentUser(fullProfile);
        
        return fullProfile;
    }
    
    /**
     * NEW HELPER METHOD: Loads the specific profile (Student or Instructor) based on the user's role.
     */
    private UserAuth loadProfile(UserAuth user) throws Exception {
        String role = user.getRole();

        if ("Student".equalsIgnoreCase(role)) {
            // Returns a Student object (which extends UserAuth)
            return studentDAO.findStudentProfile(user);
        } else if ("Instructor".equalsIgnoreCase(role)) {
            // Returns an Instructor object (which extends UserAuth)
            return instructorDAO.findInstructorProfile(user);
        } else if ("Admin".equalsIgnoreCase(role)) {
            // Admins only need the base identity data
            return user;
        } else {
            // Should not happen if roles are well-defined
            throw new Exception("Unknown user role specified in the database: " + role);
        }
    }
    
    /**
     * Clears the current user session.
     */
    public void logout() {
        SessionManager.clearSession();
    }
    
    /**
     * Gets the currently logged-in user from the session.
     */
    public UserAuth getCurrentUser() {
        return SessionManager.getCurrentUser();
    }
}