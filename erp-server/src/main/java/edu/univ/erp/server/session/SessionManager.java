package edu.univ.erp.server.session;

import edu.univ.erp.domain.UserAuth;
import java.util.Objects;

/**
 * Manages the current user session for the single-user desktop application.
 */
public class SessionManager {
    
    // Static holder for the currently authenticated user profile.
    private static UserAuth currentUser = null; 

    /**
     * Called upon successful login to set the active user session.
     * @param user The authenticated UserAuth object.
     */
    public static void setCurrentUser(UserAuth user) {
        // Ensure we don't accidentally set a null user.
        currentUser = Objects.requireNonNull(user, "UserAuth object must not be null when setting session.");
    }

    /**
     * Called upon logout or application exit to terminate the session.
     */
    public static void clearSession() {
        currentUser = null;
    }

    /**
     * Retrieves the UserAuth object for the current session.
     * @return The currently logged-in UserAuth object, or null if no one is logged in.
     */
    public static UserAuth getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if a user is currently logged into the application.
     * @return true if a session is active.
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Helper to quickly get the current user's role.
     * @return The role string or null if no one is logged in.
     */
    public static String getCurrentUserRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }
}