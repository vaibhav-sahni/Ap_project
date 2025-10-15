package edu.univ.erp.dao.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.univ.erp.dao.db.DBConnector;

/**
 * Data Access Object for handling global system settings.
 * Primarily used to check or update the status of Maintenance Mode.
 */
public class SettingDAO {

    // SQL to fetch a specific setting value
    private static final String GET_SETTING_SQL = 
        "SELECT setting_value FROM settings WHERE setting_key = ?";

    // SQL to update a specific setting value
    private static final String UPDATE_SETTING_SQL =
        "UPDATE settings SET setting_value = ? WHERE setting_key = ?";

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
            if (status == null) {
                // fallback to legacy key if present
                status = getSetting("maintenance_on");
            }
            return status != null && (status.equalsIgnoreCase("ON") || status.equalsIgnoreCase("TRUE"));
        } catch (SQLException e) {
            System.err.println("SettingDAO: Error checking maintenance mode. Defaulting to OFF. " + e.getMessage());
            return false;
        }
    }

    public void setMaintenanceMode(boolean on) throws SQLException {
        String value = on ? "ON" : "OFF";
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SETTING_SQL)) {

            stmt.setString(1, value);
            stmt.setString(2, "MAINTENANCE_MODE");
            int updated = stmt.executeUpdate();

            if (updated == 0) {
                // No existing row for the canonical key - insert it
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?)")) {
                    insert.setString(1, "MAINTENANCE_MODE");
                    insert.setString(2, value);
                    insert.executeUpdate();
                }
            }

            // Also try to update legacy key for backwards compatibility (best-effort)
            try (PreparedStatement legacy = conn.prepareStatement(UPDATE_SETTING_SQL)) {
                legacy.setString(1, value);
                legacy.setString(2, "maintenance_on");
                legacy.executeUpdate();
            } catch (SQLException ignore) {
                // best-effort; ignore legacy update failures
            }
        }
    }

    /**
     * Generic upsert for a settings key/value pair.
     */
    public void setSetting(String key, String value) throws SQLException {
        try (Connection conn = DBConnector.getErpConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SETTING_SQL)) {

            stmt.setString(1, value);
            stmt.setString(2, key);
            int updated = stmt.executeUpdate();

            if (updated == 0) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?)")) {
                    insert.setString(1, key);
                    insert.setString(2, value);
                    insert.executeUpdate();
                }
            }
        }
    }
}
