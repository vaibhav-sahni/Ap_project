package edu.univ.erp.api;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.univ.erp.domain.Notification;

public class NotificationAPI {
    private final Gson gson = new Gson();

    /**
     * Fetch recent notifications for a user. recipientType can be STUDENT or INSTRUCTOR.
     */
    public List<Notification> fetchNotificationsForUser(int userId, String recipientType, int limit) throws Exception {
        String request = String.format("GET_NOTIFICATIONS:%d:%s:%d", userId, recipientType, limit);
        String response = ClientRequest.send(request);
        if (response.startsWith("SUCCESS:")) {
            String json = response.substring("SUCCESS:".length());
            Type listType = new TypeToken<List<Notification>>() {}.getType();
            return gson.fromJson(json, listType);
        }
        throw new Exception(response.startsWith("ERROR:") ? response.substring("ERROR:".length()) : "Unknown error fetching notifications");
    }
}
