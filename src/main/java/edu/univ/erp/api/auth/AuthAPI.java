package edu.univ.erp.api.auth;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.auth.session.SessionManager;

/**
 * The API Facade for Authentication. This is the only layer the UI should call
 * for login, centralizing control and error handling.
 */
public class AuthAPI {

    private final AuthService authService = new AuthService();

    /**
     * Attempts to log a user into the system.
     * * @param username The username from the UI.
     * @param password The password from the UI.
     * @return The UserAuth object if successful.
     * @throws Exception with a user-friendly message on failure.
     */
    public UserAuth login(String username, String password) throws Exception {
        
        // 1. Basic Input Validation (API level check)
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new Exception("Please enter both username and password.");
        }
        
        // 2. Delegate to the business logic layer
        UserAuth userAuth = authService.login(username.trim(), password);
        
        // FUTURE STEP: At this point, we will add the logic to call StudentDAO/InstructorDAO 
        // to load the user's *full* ERP profile (e.g., student's roll number, instructor's dept).
        
        return userAuth;
    }
    
    /**
     * Clears the current user session.
     */
    public void logout() {
        authService.logout();
    }
    
    /**
     * Checks if a session is currently active.
     */
    public boolean isLoggedIn() {
        return SessionManager.isLoggedIn();
    }
}