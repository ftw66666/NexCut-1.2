package com.example.opencv.http;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("Code")
    public String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
