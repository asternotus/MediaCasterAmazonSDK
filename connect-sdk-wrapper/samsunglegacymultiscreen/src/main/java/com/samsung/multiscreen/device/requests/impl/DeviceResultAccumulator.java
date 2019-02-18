//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests.impl;

import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeviceResultAccumulator implements DeviceAsyncResult<Device> {
    private static final Logger LOG = Logger.getLogger(DeviceResultAccumulator.class.getName());
    private List<Device> deviceResults;
    private List<DeviceError> deviceErrors;
    private DeviceAsyncResult<List<Device>> callback;
    int resultSize;

    public DeviceResultAccumulator(List<Device> resultList, List<DeviceError> errorList, int totalSize, DeviceAsyncResult<List<Device>> cb) {
        this.deviceResults = resultList;
        this.deviceErrors = errorList;
        this.resultSize = totalSize;
        this.callback = cb;
    }

    public void onResult(Device device) {
        this.deviceResults.add(device);
        boolean done = this.deviceResults.size() + this.deviceErrors.size() >= this.resultSize;
        if(done) {
            this.callback.onResult(this.deviceResults);
        }

    }

    public void onError(DeviceError error) {
        LOG.info("findLocal() onError() error: " + error);
        this.deviceErrors.add(error);
        boolean done = this.deviceResults.size() + this.deviceErrors.size() >= this.resultSize;
        if(done) {
            this.callback.onResult(this.deviceResults);
        }

    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
