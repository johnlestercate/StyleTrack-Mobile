package com.styletrack.customer.data;

public class UpdateProfileRequest {
    public String fullName;
    public String email;
    public String address;

    public UpdateProfileRequest(String fullName, String email, String address) {
        this.fullName = fullName;
        this.email = email;
        this.address = address;
    }
}
