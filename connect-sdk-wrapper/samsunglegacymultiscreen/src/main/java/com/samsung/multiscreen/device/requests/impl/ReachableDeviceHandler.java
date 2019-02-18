//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests.impl;

import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReachableDeviceHandler implements DeviceAsyncResult<Device> {
    private static final Logger LOG = Logger.getLogger(ReachableDeviceHandler.class.getName());
    private Device defaultResult;
    private DeviceAsyncResult<Device> callback;

    public ReachableDeviceHandler(Device defaultResult, DeviceAsyncResult<Device> callback) {
        this.defaultResult = defaultResult;
        this.callback = callback;
    }

    public void onResult(Device localDevice) {
        LOG.info("ReachableDeviceHandler() onResult() localDevice\n: " + localDevice);
        if(localDevice != null) {
            this.callback.onResult(localDevice);
        } else {
            this.callback.onResult(this.defaultResult);
        }

    }

    public void onError(DeviceError error) {
        LOG.info("ReachableDeviceHandler() onError() error: " + error);
        this.callback.onResult(this.defaultResult);
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
