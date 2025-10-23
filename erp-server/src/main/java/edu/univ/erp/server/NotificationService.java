package edu.univ.erp.server;

import java.util.List;

import edu.univ.erp.dao.notification.NotificationDAO;
import edu.univ.erp.domain.Notification;

/**
 * Thin service wrapper for notifications. Provides convenience methods
 * used by other server components. This keeps higher-level code decoupled
 * from DAO method names.
 */
public class NotificationService {
	private final NotificationDAO dao = new NotificationDAO();

	/**
	 * Convenience method to create a notification. If recipientId is null or 0,
	 * the notification is treated as broadcast (ALL). Otherwise recipient type
	 * defaults to STUDENT.
	 */
	public boolean createNotification(int senderId, Integer recipientId, String message) {
		String recipientType = (recipientId == null || recipientId.intValue() == 0) ? "ALL" : "STUDENT";
		Notification n = new Notification(0, senderId, recipientType, recipientId == null ? 0 : recipientId.intValue(), "", message, java.time.LocalDateTime.now(), false);
		return dao.insertNotification(n);
	}

	/**
	 * Fetch recent notifications for a user. This will return notifications
	 * addressed to ALL, to the user's role (STUDENT/INSTRUCTOR) or specifically to the user.
	 */
	public List<Notification> fetchNotificationsForUser(int userId, int limit) {
		// Default to STUDENT recipient type for fetching user-specific list.
		return dao.fetchRecentForUser(userId, "STUDENT", limit);
	}
}

