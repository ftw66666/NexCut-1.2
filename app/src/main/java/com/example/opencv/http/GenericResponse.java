package com.example.opencv.http;

// File: GenericResponse.java (用于简单JSON响应)
public class GenericResponse {
    private String message;
    private boolean state;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }
}
