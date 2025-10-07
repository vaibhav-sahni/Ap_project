package edu.univ.erp.dao.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.univ.erp.dao.db.DBConnector;

/**
 * Data Access Object for handling global system settings.
 * Primarily used to check the status of Maintenance Mode.
 */
public class SettingDAO {

    // SQL to fetch a specific setting value
    private static final String GET_SETTING_SQL = 
        "SELECT setting_value FROM GlobalSettings WHERE setting_key = ?";

    /**
     * Retrieves the value of a specific global setting key.
     * @param settingKey The key (e.g., "MAINTENANCE_MODE").
     * @return The setting value as a String, or null if not found.
     */
    public String getSetting(String settingKey) throws SQLException {
        String value = null;
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_SETTING_SQL)) {
            
            stmt.setString(1, settingKey);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    value = rs.getString("setting_value");
                }
            }
        }
        return value;
    }
    
    /**
     * Checks if the system is currently in maintenance mode.
     * @return true if maintenance mode is "ON", false otherwise.
     */
    public boolean isMaintenanceModeOn() {
        try {
            String status = getSetting("MAINTENANCE_MODE");
            // Case-insensitive check for "ON"
            return status != null && status.equalsIgnoreCase("ON");
        } catch (SQLException e) {
            System.err.println("SettingDAO: Error checking maintenance mode. Defaulting to OFF. " + e.getMessage());
            // Fail-safe: if DB access fails, we assume maintenance is OFF 
            // to avoid blocking all traffic unintentionally.
            return false;
        }
    }
}