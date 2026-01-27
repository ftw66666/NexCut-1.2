package com.example.opencv.http;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WarningResponse {


    @SerializedName("Warning")
    private List<WarningEntity> warnings;

    // 默认构造方法
    public WarningResponse() {
    }

    // 带参数的构造方法
    public WarningResponse(List<WarningEntity> warnings) {
        this.warnings = warnings;
    }

    // Getter 和 Setter 方法

    public List<WarningEntity> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<WarningEntity> warnings) {
        this.warnings = warnings;
    }

    // toString 方法（可选，便于调试）
    @Override
    public String toString() {
        return "WarningsResponse{" +
                "warnings=" + warnings +
                '}';
    }

    public static class WarningEntity {
        @SerializedName("ErrorCode")
        private int errorCode;

        @SerializedName("ErrorInfo")
        private String errorInfo;

        // 默认构造方法
        public WarningEntity() {
        }

        // 带参数的构造方法
        public WarningEntity(int errorCode, String errorInfo) {
            this.errorCode = errorCode;
            this.errorInfo = errorInfo;
        }

        // Getter 和 Setter 方法

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorInfo() {
            return errorInfo;
        }

        public void setErrorInfo(String errorInfo) {
            this.errorInfo = errorInfo;
        }

        // toString 方法（可选，便于调试）
        @Override
        public String toString() {
            return "WarningEntity{" +
                    "errorCode=" + errorCode +
                    ", errorInfo='" + errorInfo + '\'' +
                    '}';
        }
    }
}
