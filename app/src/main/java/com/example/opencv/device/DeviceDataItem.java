package com.example.opencv.device;

public class DeviceDataItem {
    private String name;
    private String value;

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
