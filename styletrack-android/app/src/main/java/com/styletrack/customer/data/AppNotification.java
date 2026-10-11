package com.styletrack.customer.data;

public class AppNotification {
    public long id;
    public String type;
    public String title;
    public String message;
    public Long appointmentId;
    public boolean seen = false;
    public String createdAt;
}
