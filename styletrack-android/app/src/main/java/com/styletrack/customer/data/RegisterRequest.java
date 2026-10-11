package com.styletrack.customer.data;

public class RegisterRequest {
    public String fullName;
    public String phone;
    public String email;
    public String password;

    public RegisterRequest(String fullName, String phone, String email, String password) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.password = password;
    }
}
