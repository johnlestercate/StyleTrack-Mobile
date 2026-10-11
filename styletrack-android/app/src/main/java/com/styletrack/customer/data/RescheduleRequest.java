package com.styletrack.customer.data;

public class RescheduleRequest {
    public String startTime;
    public Long stylistId;

    public RescheduleRequest(String startTime, Long stylistId) {
        this.startTime = startTime;
        this.stylistId = stylistId;
    }
}
