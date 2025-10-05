package edu.univ.erp.security;
import org.mindrot.jbcrypt.BCrypt; 

public class PasswordHasher {

    // Define the secure workload factor (cost). 12 is a common standard.
    private static final int WORKLOAD = 12;

    /**
     * Hashes a plaintext password for secure storage in the Auth DB.
     * @param plaintextPassword The password to hash (e.g., during user creation).
     * @return The securely hashed password string, including the salt.
     */
    public static String hashPassword(String plaintextPassword) {
        if (plaintextPassword == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }
        // REAL SECURE IMPLEMENTATION
        return BCrypt.hashpw(plaintextPassword, BCrypt.gensalt(WORKLOAD));
    }

    /**
     * Verifies a plaintext password entered during login against a stored hash.
     * @param plaintextPassword The password entered by the user.
     * @param storedHash The hash retrieved from the users_auth table.
     * @return true if the passwords match, false otherwise.
     */
    public static boolean verifyPassword(String plaintextPassword, String storedHash) {
        if (plaintextPassword == null || storedHash == null) {
            return false;
        }
        // REAL SECURE IMPLEMENTATION
        try {
            return BCrypt.checkpw(plaintextPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Handle cases where the stored hash might be improperly formatted (e.g., from an old system)
            // For production, you'd log this. For now, it means verification failed.
            return false;
        }
    }
}