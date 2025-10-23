package edu.univ.erp.domain;

/**
 * Lightweight Notification domain object used by client UI.
 */
public class Notification {
    private int id;
    private int senderId;
    private String recipientType; // STUDENT | INSTRUCTOR | ALL
    private int recipientId; // optional (0 means not used)
    private String title;
    private String message;
    private String timestamp; // ISO-8601 string
    private boolean read;

    public Notification() {}

    public Notification(int id, int senderId, String recipientType, int recipientId, String title, String message, String timestamp, boolean read) {
        this.id = id;
        this.senderId = senderId;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }

    public int getRecipientId() { return recipientId; }
    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
