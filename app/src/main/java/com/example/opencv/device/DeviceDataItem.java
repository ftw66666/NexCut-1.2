package com.example.opencv.device;

public class DeviceDataItem {
    private final String name;
    private final String value;

    public DeviceDataItem(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
