package edu.univ.erp.api;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.univ.erp.domain.Notification;

public class NotificationAPI {

    private final Gson gson = new Gson();

    /**
     * Fetch recent notifications for a user. recipientType can be STUDENT or
     * INSTRUCTOR.
     */
    public List<Notification> fetchNotificationsForUser(int userId, String recipientType, int limit) throws Exception {
        String request = String.format("GET_NOTIFICATIONS:%d:%s:%d", userId, recipientType, limit);
        String response = ClientRequest.send(request);
        if (response.startsWith("SUCCESS:")) {
            String json = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<Notification>>() {
            }.getType();
            return gson.fromJson(json, listType);
        }
        throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error fetching notifications");
    }

    /**
     * Send a notification via the server. recipientType is e.g. "ALL",
     * "STUDENT", "INSTRUCTOR". recipientId may be 0 for broadcast/all.
     */
    public boolean sendNotification(String recipientType, int recipientId, String title, String message) throws Exception {
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("title", title == null ? "" : title);
        payload.put("message", message == null ? "" : message);
        String json = gson.toJson(payload);
        String base64 = java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String request = String.format("SEND_NOTIFICATION:%s:%d:BASE64:%s", recipientType == null ? "ALL" : recipientType, recipientId, base64);
        String resp = ClientRequest.send(request);
        if (resp != null && resp.startsWith("SUCCESS")) {
            return true;
        }
        throw new Exception(resp == null ? "No response" : (resp.startsWith("ERROR:") ? resp.substring("ERROR:".length()) : resp));
    }
}
