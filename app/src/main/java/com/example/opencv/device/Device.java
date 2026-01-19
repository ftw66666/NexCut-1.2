package com.example.opencv.device;

import java.util.Objects;

public class Device {
    private final String ip;
    private final int port;
    private final String model;
    private final String deviceId;
    private long lastUpdated; // 最后一次更新时间（毫秒）

    public Device(String ip, int port, String model, String deviceId) {
        this.ip = ip;
        this.port = port;
        this.model = model;
        this.deviceId = deviceId;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getter方法...
    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getModel() {
        return model;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void updateTimestamp() {
        this.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return port == device.port &&
                Objects.equals(ip, device.ip) &&
                Objects.equals(model, device.model) &&
                Objects.equals(deviceId, device.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, model, deviceId);
    }
}
