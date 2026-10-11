package com.styletrack.customer.data;

/** What to do next after starting an online payment: open redirectUrl (GCash) or show qrImage (QR Ph). */
public class PayStarted {
    public long paymentId;
    public String method;
    public String status = "PENDING";
    public double amount = 0.0;
    public String redirectUrl;
    public String qrImage;
    public String expiresAt;
}
