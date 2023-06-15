package dev.juhouse.projector.projection2;

import lombok.Data;

public class BridgeCaptureDevice {
    private String deviceName;
    private BridgeCaptureDeviceResolution[] resolutions;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public BridgeCaptureDeviceResolution[] getResolutions() {
        return resolutions;
    }

    public void setResolutions(BridgeCaptureDeviceResolution[] resolutions) {
        this.resolutions = resolutions;
    }
}
