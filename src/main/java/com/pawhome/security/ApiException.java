package com.pawhome.security;

/** Thrown anywhere in the app to produce a clean {"error": "..."} JSON response. */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
