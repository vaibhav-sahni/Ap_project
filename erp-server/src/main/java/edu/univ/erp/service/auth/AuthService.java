package edu.univ.erp.service.auth;
import java.time.LocalDateTime; // Required for calculating time difference
import java.time.temporal.ChronoUnit;

import edu.univ.erp.dao.auth.AuthDAO;
import edu.univ.erp.dao.auth.AuthDAO.AuthDetails;
import edu.univ.erp.domain.UserAuth;
import edu.univ.erp.security.PasswordHasher;

public class AuthService {
    
    private final AuthDAO authDAO;

    public AuthService() {
        this.authDAO = new AuthDAO();
    }

    /**
     * Authenticates a user, checking for lockout status and updating attempt metrics.
     * @param username The username provided by the client.
     * @param password The raw password provided by the client.
     * @return A clean UserAuth object if successful.
     * @throws Exception If authentication fails (e.g., bad password, user not found, locked).
     */
    public UserAuth authenticate(String username, String password) throws Exception {
        
    // Authentication attempts are intentionally not logged to stdout to avoid verbose server output.

        // 1. Fetch authentication details (including the hash and lockout status)
        AuthDetails details = authDAO.findUserByUsername(username); 
        
        if (details == null) {
            // User not found in the database. (No lockout tracking if user doesn't exist)
            throw new Exception("Invalid username or password."); 
        }
        
        int userId = details.userID();
        
        // --- 2. Lockout Check (Bonus Feature) ---
        if (details.lockedUntil() != null && details.lockedUntil().isAfter(LocalDateTime.now())) {
            
            // Calculate total time remaining in seconds
            long totalSecondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), details.lockedUntil());
            
            // Ensure the time remaining is positive (in case of near-expiration)
            if (totalSecondsRemaining <= 0) {
                 // Account should be unlocked now, but we return a general message as a safety net
                 // near-expiration details intentionally not printed to stdout
                 throw new Exception("Account is temporarily locked due to too many failed attempts. Please try logging in again.");
            }
            
            // Calculate minutes and remaining seconds
            long minutes = totalSecondsRemaining / 60;
            long seconds = totalSecondsRemaining % 60;
            
            // Build the dynamic time display string
            StringBuilder timeDisplay = new StringBuilder();
            if (minutes > 0) {
                timeDisplay.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            }
            if (minutes > 0 && seconds > 0) {
                timeDisplay.append(" and ");
            }
            if (seconds > 0) {
                 timeDisplay.append(seconds).append(seconds == 1 ? " second" : " seconds");
            }
            
            // access-blocked details intentionally not printed to stdout
            
            // NEW: Include the precise time remaining in the error message for the client to display.
            throw new Exception("Account is temporarily locked due to too many failed attempts. Try again in " + timeDisplay.toString() + ".");
        }
        
        String storedHash = details.passwordHash(); 
        
        // 3. Verify the submitted password
        if (PasswordHasher.verifyPassword(password, storedHash)) {
            
            // SUCCESS: Reset attempts and proceed
            authDAO.updateLoginAttempts(userId, true); 
            // Update last_login timestamp
            authDAO.updateLastLogin(userId);
            
            // Fetch the latest details to get the updated last_login value
            AuthDetails refreshed = authDAO.findUserByUserId(userId);
            String lastLoginStr = null;
            if (refreshed != null && refreshed.lastLogin() != null) {
                lastLoginStr = refreshed.lastLogin().toString(); // ISO-8601 from LocalDateTime
            }

            UserAuth user = new UserAuth(
                userId,
                username,
                details.role(),
                lastLoginStr
            );
            
            // success authentication message intentionally not printed to stdout
            return user;
            
        } else {
            // FAILURE: Update attempts/lockout status
            authDAO.updateLoginAttempts(userId, false); 
            throw new Exception("Invalid username or password."); 
        }
    }

    // ----------------------------------------------------------------------
    // --- CHANGE PASSWORD FEATURE (Bonus) ---
    // ----------------------------------------------------------------------

    /**
     * Handles the secure change password logic.
     * @param userId The ID of the user changing the password.
     * @param oldPassword The current raw password.
     * @param newPassword The new raw password.
     * @return true if the password was successfully changed.
     * @throws Exception If passwords don't match or the new password is invalid.
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) throws Exception {
        AuthDetails details = authDAO.findUserByUserId(userId); 
        
        if (details == null) {
            throw new Exception("User not found.");
        }
        
        // 1. Verify old password (Security Check)
        if (!PasswordHasher.verifyPassword(oldPassword, details.passwordHash())) {
            throw new Exception("Old password does not match.");
        }
        
        // 2. Simple Validation for new password
        if (newPassword == null || newPassword.length() < 8) {
             throw new Exception("New password must be at least 8 characters long.");
        }

        // 3. Hash new password and update in the database
        String newHash = PasswordHasher.hashPassword(newPassword);
        boolean success = authDAO.updatePasswordHash(userId, newHash);
        
        if (success) {
            System.out.println("SERVER LOG: SUCCESS - User " + userId + " changed password.");
            return true;
        } else {
            throw new Exception("Database failed to update the password.");
        }
    }
}