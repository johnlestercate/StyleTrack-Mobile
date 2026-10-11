package com.styletrack.customer.data;

public class Appointment {
    public long id;
    public long stylistId;
    public String stylistName;
    public long serviceId;
    public String serviceName;
    public String startTime;
    public String endTime;
    public String type;
    public String status;
    public String address;
    public String notes;
    public String rejectionReason;
    public double homeServiceFee = 0.0;
    public double totalAmount = 0.0;
    public String paymentStatus = "UNPAID";
    public String paymentMethod;
    public int pointsEarned = 0;

    public boolean isHomeService() {
        return "HOME_SERVICE".equals(type);
    }

    public boolean isOpen() {
        return "PENDING".equals(status) || "ACCEPTED".equals(status);
    }
}
