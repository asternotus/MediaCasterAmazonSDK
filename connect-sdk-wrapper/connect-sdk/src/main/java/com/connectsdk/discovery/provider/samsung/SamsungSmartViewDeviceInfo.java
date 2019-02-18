package com.connectsdk.discovery.provider.samsung;

import com.samsung.multiscreenfix.Service;
import com.samsung.multiscreen.device.Device;

/**
 * Created by Dmitry on 24.05.17.
 */

public class SamsungSmartViewDeviceInfo {

    private Service smartViewService;
    private Device smartViewLegacyDevice;

    private boolean tizen = true;


    public SamsungSmartViewDeviceInfo(Service smartViewService, Device smartViewLegacyDevice, boolean tizen) {
        this.smartViewService = smartViewService;
        this.smartViewLegacyDevice = smartViewLegacyDevice;
        this.tizen = tizen;
    }

    public Service getSmartViewService() {
        return smartViewService;
    }

    public void setSmartViewService(Service smartViewService) {
        this.smartViewService = smartViewService;
    }

    public Device getSmartViewLegacyDevice() {
        return smartViewLegacyDevice;
    }

    public void setSmartViewLegacyDevice(Device smartViewLegacyDevice) {
        this.smartViewLegacyDevice = smartViewLegacyDevice;
    }

    public boolean isTizen() {
        return tizen;
    }

    public void setTizen(boolean tizen) {
        this.tizen = tizen;
    }
}
