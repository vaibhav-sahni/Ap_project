package edu.univ.erp.dao.notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.univ.erp.dao.db.DBConnector;
import edu.univ.erp.domain.Notification;

public class NotificationDAO {
    private static final Logger LOGGER = Logger.getLogger(NotificationDAO.class.getName());

    private static final String INSERT_SQL = "INSERT INTO erp_db.notifications (sender_id, recipient_type, recipient_id, title, message, timestamp, is_read) VALUES (?, ?, ?, ?, ?, ?, ? )";
    private static final String SELECT_RECENT_FOR_USER =
        "SELECT id, sender_id, recipient_type, recipient_id, title, message, timestamp, is_read FROM erp_db.notifications " +
        "WHERE (recipient_type = 'ALL') OR (recipient_type = ? AND recipient_id = 0) OR (recipient_type = ? AND recipient_id = ?) " +
        "ORDER BY timestamp DESC LIMIT ?";

    public boolean insertNotification(Notification n) {
        try (Connection conn = DBConnector.getErpConnection(); PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            stmt.setInt(1, n.getSenderId());
            stmt.setString(2, n.getRecipientType());
            stmt.setInt(3, n.getRecipientId());
            stmt.setString(4, n.getTitle());
            stmt.setString(5, n.getMessage());
            // store timestamp as SQL Timestamp (DATETIME/TIMESTAMP column)
            if (n.getTimestamp() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(n.getTimestamp()));
            } else {
                stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            }
            stmt.setBoolean(7, n.isRead());
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "NotificationDAO insert error: " + e.getMessage(), e);
            return false;
        }
    }

    public List<Notification> fetchRecentForUser(int userId, String recipientType, int limit) {
        List<Notification> out = new ArrayList<>();
        try (Connection conn = DBConnector.getErpConnection(); PreparedStatement stmt = conn.prepareStatement(SELECT_RECENT_FOR_USER)) {
            stmt.setString(1, recipientType);
            stmt.setString(2, recipientType);
            stmt.setInt(3, userId);
            stmt.setInt(4, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                // Deduplicate identical notifications (same id or same title+message+timestamp)
                java.util.Set<String> seen = new java.util.HashSet<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String title = rs.getString("title");
                    String message = rs.getString("message");
                    Timestamp ts = rs.getTimestamp("timestamp");
                    java.time.LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : null;
                    String sig = id + "|" + (title == null ? "" : title) + "|" + (message == null ? "" : message) + "|" + (ldt == null ? "" : ldt.toString());
                    if (seen.contains(sig)) continue;
                    seen.add(sig);
                    Notification n = new Notification(
                        id, rs.getInt("sender_id"), rs.getString("recipient_type"), rs.getInt("recipient_id"),
                        title, message, ldt, rs.getBoolean("is_read")
                    );
                    out.add(n);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "NotificationDAO fetch error: " + e.getMessage(), e);
        }
        return out;
    }
}
