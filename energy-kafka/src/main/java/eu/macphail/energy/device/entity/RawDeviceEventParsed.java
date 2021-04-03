package eu.macphail.energy.device.entity;

import java.util.List;

public class RawDeviceEventParsed {

    private final Long receivedTimestamp;
    private final String regionId;
    private final List<Device> devices;

    public RawDeviceEventParsed(Long receivedTimestamp, String regionId, List<Device> devices) {
        this.receivedTimestamp = receivedTimestamp;
        this.regionId = regionId;
        this.devices = devices;
    }

    public Long getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public String getRegionId() {
        return regionId;
    }

    public List<Device> getDevices() {
        return devices;
    }
}
