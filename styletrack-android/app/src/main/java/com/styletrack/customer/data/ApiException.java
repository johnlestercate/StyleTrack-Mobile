package com.styletrack.customer.data;

/** An error with a message that is safe to show the customer as-is. */
public class ApiException extends RuntimeException {
    private final int code;

    public ApiException(String message) {
        this(message, 0);
    }

    public ApiException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
