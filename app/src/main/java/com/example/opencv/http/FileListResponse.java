package com.example.opencv.http;

import java.util.List;

// File: FileListResponse.java
public class FileListResponse {
    private List<String> enc;
    private List<String> nc;

    public List<String> getEnc() {
        return enc;
    }

    public void setEnc(List<String> enc) {
        this.enc = enc;
    }

    public List<String> getNc() {
        return nc;
    }

    public void setNc(List<String> nc) {
        this.nc = nc;
    }
}
