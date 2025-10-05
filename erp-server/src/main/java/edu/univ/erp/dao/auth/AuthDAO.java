package edu.univ.erp.dao.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import edu.univ.erp.dao.db.DBConnector;

public class AuthDAO {
    
    // --- 1. UPDATED: AuthDetails record now includes lockout fields ---
    public static record AuthDetails (
        int userID,
        String passwordHash,
        String role,
        int failedAttempts,      // NEW: For tracking lockout status
        LocalDateTime lockedUntil // NEW: For checking lockout expiration
    ){}

    // --- SQL Queries ---
    private static final String FIND_USER_SQL = 
        "SELECT user_id, password_hash, role, failed_attempts, locked_until FROM users_auth WHERE username = ?";
    
    private static final String FIND_USER_BY_ID_SQL = 
        "SELECT user_id, password_hash, role, failed_attempts, locked_until FROM users_auth WHERE user_id = ?";

    private static final String UPDATE_HASH_SQL = 
        "UPDATE users_auth SET password_hash = ? WHERE user_id = ?";

    // SQL for lockout management (5 attempts max)
    private static final String UPDATE_ATTEMPTS_SUCCESS_SQL = 
        "UPDATE users_auth SET failed_attempts = 0, locked_until = NULL WHERE user_id = ?";
    
    private static final String UPDATE_ATTEMPTS_FAILURE_SQL = 
        "UPDATE users_auth SET failed_attempts = failed_attempts + 1, " +
        "locked_until = CASE WHEN failed_attempts + 1 >= 5 THEN ? ELSE NULL END " +
        "WHERE user_id = ?";

    /**
     * Helper method to map a ResultSet row to the AuthDetails record.
     */
    private AuthDetails mapResultSetToAuthDetails(ResultSet rs) throws SQLException {
        Timestamp lockedTimestamp = rs.getTimestamp("locked_until");
        LocalDateTime lockedUntil = (lockedTimestamp != null) ? lockedTimestamp.toLocalDateTime() : null;
        
        return new AuthDetails( 
            rs.getInt("user_id"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getInt("failed_attempts"),
            lockedUntil
        );
    }
    
    /**
     * Retrieves authentication details from the Auth DB using the username.
     */
    public AuthDetails findUserByUsername(String username) {
        try (Connection conn = DBConnector.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_USER_SQL)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuthDetails(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error during auth lookup for user: " + username + ". " + e.getMessage());
        }
        return null;
    }
    
    /**
     * NEW: Retrieves authentication details from the Auth DB using the user ID (used for Change Password).
     */
    public AuthDetails findUserByUserId(int userId) {
        try (Connection conn = DBConnector.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_USER_BY_ID_SQL)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuthDetails(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error during auth lookup for ID: " + userId + ". " + e.getMessage());
        }
        return null;
    }

    /**
     * NEW: Updates the password hash for a given user ID (for Change Password).
     */
    public boolean updatePasswordHash(int userId, String newHash) {
        try (Connection conn = DBConnector.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_HASH_SQL)) {
            stmt.setString(1, newHash);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DB Error updating password for ID: " + userId + ". " + e.getMessage());
            return false;
        }
    }

    /**
     * NEW: Updates lockout metrics upon a successful or failed login.
     * Lockout is set for 30 minutes if failed_attempts >= 5.
     */
    public void updateLoginAttempts(int userId, boolean success) {
        String sql = success ? UPDATE_ATTEMPTS_SUCCESS_SQL : UPDATE_ATTEMPTS_FAILURE_SQL;
        
        try (Connection conn = DBConnector.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (!success) {
                // If it's a failure, set the potential lockout time (30 min from now)
                LocalDateTime lockoutTime = LocalDateTime.now().plusMinutes(2);
                stmt.setTimestamp(1, Timestamp.valueOf(lockoutTime));
                stmt.setInt(2, userId);
            } else {
                // If it's a success, only the user ID is needed
                stmt.setInt(1, userId);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB Error updating login attempts for ID: " + userId + ". " + e.getMessage());
        }
    }
}