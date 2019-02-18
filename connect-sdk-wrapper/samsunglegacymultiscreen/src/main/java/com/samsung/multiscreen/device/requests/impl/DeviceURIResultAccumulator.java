//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests.impl;

import com.samsung.multiscreen.device.requests.impl.DeviceURIResult;
import com.samsung.multiscreen.net.AsyncResult;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeviceURIResultAccumulator implements AsyncResult<DeviceURIResult> {
    private static final Logger LOG = Logger.getLogger(DeviceURIResultAccumulator.class.getName());
    private List<DeviceURIResult> results = new LinkedList();
    private int count = 0;
    private AsyncResult<List<DeviceURIResult>> callback;

    public DeviceURIResultAccumulator(int totalSize, AsyncResult<List<DeviceURIResult>> callback) {
        this.callback = callback;
    }

    public void onResult(DeviceURIResult result) {
        this.results.add(result);
        --this.count;
        if(this.count <= 0) {
            this.callback.onResult(this.results);
        }

    }

    public void onException(Exception e) {
        --this.count;
        if(this.count <= 0) {
            this.callback.onResult(this.results);
        }

    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
