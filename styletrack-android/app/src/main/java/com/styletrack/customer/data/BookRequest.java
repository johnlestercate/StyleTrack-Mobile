package com.styletrack.customer.data;

public class BookRequest {
    public long stylistId;
    public long serviceId;
    public String type;
    public String startTime;
    public String address;
    public String notes;

    public BookRequest(long stylistId, long serviceId, String type, String startTime, String address, String notes) {
        this.stylistId = stylistId;
        this.serviceId = serviceId;
        this.type = type;
        this.startTime = startTime;
        this.address = address;
        this.notes = notes;
    }
}
