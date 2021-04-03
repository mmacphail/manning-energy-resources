package eu.macphail.energy.device.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Device {
    private String deviceID;
    private Long charging;

    public Device() {
    }

    public Device(String deviceID, Long charging) {
        this.deviceID = deviceID;
        this.charging = charging;
    }

    @JsonProperty("device_id")
    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    @JsonProperty("charging")
    public Long getCharging() {
        return charging;
    }

    public void setCharging(Long charging) {
        this.charging = charging;
    }

    @Override
    public String toString() {
        return "Device{" +
                "deviceID='" + deviceID + '\'' +
                ", charging=" + charging +
                '}';
    }
}
