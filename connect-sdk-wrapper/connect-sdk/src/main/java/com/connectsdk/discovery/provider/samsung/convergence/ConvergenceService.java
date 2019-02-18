package com.connectsdk.discovery.provider.samsung.convergence;

import android.support.annotation.NonNull;

import com.connectsdk.service.convergence.NSConnection;
import com.connectsdk.service.convergence.NSDevice;

/**
 * Created by Dmitry on 31.03.17.
 */

public class ConvergenceService {

    private NSDevice nsDevice;
    private NSConnection nsConnection;

    public ConvergenceService(NSDevice nsDevice, NSConnection nsConnection) {
        this.nsDevice = nsDevice;
        this.nsConnection = nsConnection;
    }

    public NSDevice getNsDevice() {
        return nsDevice;
    }

    public void setNsDevice(NSDevice nsDevice) {
        this.nsDevice = nsDevice;
    }

    public NSConnection getNsConnection() {
        return nsConnection;
    }

    public void setNsConnection(NSConnection nsConnection) {
        this.nsConnection = nsConnection;
    }

}
