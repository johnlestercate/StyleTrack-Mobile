package com.styletrack.customer.data;

public class Profile {
    public long id;
    public String fullName;
    public String phone;
    public String email;
    public String address;
    public int loyaltyPoints = 0;
    public int completedVisits = 0;
    public String tier = "REGULAR";
    public int visitsToNextTier = 0;
}
