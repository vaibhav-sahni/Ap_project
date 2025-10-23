package edu.univ.erp.domain;

import java.time.LocalDateTime;

/** Simple server-side Notification domain object used for DB mapping. */
public class Notification {
    private int id;
    private int senderId;
    private String recipientType; // STUDENT | INSTRUCTOR | ALL
    private int recipientId; // optional
    private String title;
    private String message;
    private LocalDateTime timestamp; // event time
    private boolean read;

    public Notification() {}

    public Notification(int id, int senderId, String recipientType, int recipientId, String title, String message, LocalDateTime timestamp, boolean read) {
        this.id = id; this.senderId = senderId; this.recipientType = recipientType; this.recipientId = recipientId;
        this.title = title; this.message = message; this.timestamp = timestamp; this.read = read;
    }

    public int getId() { return id; }
    public int getSenderId() { return senderId; }
    public String getRecipientType() { return recipientType; }
    public int getRecipientId() { return recipientId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime ts) { this.timestamp = ts; }
    public boolean isRead() { return read; }
}
