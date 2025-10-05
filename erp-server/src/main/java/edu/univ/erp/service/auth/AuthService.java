package edu.univ.erp.service.auth;

import edu.univ.erp.dao.auth.AuthDAO;
import edu.univ.erp.dao.auth.AuthDAO.AuthDetails;
import edu.univ.erp.domain.UserAuth; // <-- IMPORT THE NESTED RECORD
import edu.univ.erp.security.PasswordHasher;

public class AuthService {
    
    private final AuthDAO authDAO;

    public AuthService() {
        this.authDAO = new AuthDAO();
    }

    /**
     * Authenticates a user by checking credentials against the database.
     * @param username The username provided by the client.
     * @param password The raw password provided by the client.
     * @return A clean UserAuth object if successful.
     * @throws Exception If authentication fails (e.g., bad password, user not found).
     */
    public UserAuth authenticate(String username, String password) throws Exception {
        
        System.out.println("SERVER LOG: Attempting authentication for user: " + username);

        // 1. Fetch authentication details (including the hash) using the DAO
        AuthDetails details = authDAO.findUserByUsername(username); 
        
        if (details == null) {
            // User not found in the database
            throw new Exception("Invalid username or password."); // Keep message vague for security
        }
        
        // 2. Retrieve the stored hash and role from the AuthDetails record
        String storedHash = details.passwordHash(); // Accessor from the record
        
        // 3. Verify the submitted password using the secure PasswordHasher
        if (PasswordHasher.verifyPassword(password, storedHash)) {
            
            // 4. AUTHENTICATION SUCCESS: Create the clean UserAuth object
            // The UserAuth object must match the constructor (int userId, String username, String role)
            UserAuth user = new UserAuth(
                details.userID(), // Use the userID from the record
                username,         // Use the provided username
                details.role()    // Use the role from the record
            );
            
            System.out.println("SERVER LOG: SUCCESS - User authenticated: " + user.getUsername() + ", Role: " + user.getRole());
            return user;
            
        } else {
            // Password verification failed
            throw new Exception("Invalid username or password."); // Keep message vague for security
        }
    }
}