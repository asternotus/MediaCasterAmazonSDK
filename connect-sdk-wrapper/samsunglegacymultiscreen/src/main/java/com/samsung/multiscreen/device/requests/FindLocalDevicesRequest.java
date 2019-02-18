//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.requests.GetDeviceRequest;
import com.samsung.multiscreen.device.requests.impl.DeviceResultAccumulator;
import com.samsung.multiscreen.net.ssdp.SSDPSearch;
import com.samsung.multiscreen.net.ssdp.SSDPSearchListener;
import com.samsung.multiscreen.net.ssdp.SSDPSearchResult;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FindLocalDevicesRequest implements SSDPSearchListener, Runnable {
    private static final Logger LOG = Logger.getLogger(FindLocalDevicesRequest.class.getName());
    private List<Device> deviceResults = new ArrayList();
    private List<DeviceError> deviceErrors = new ArrayList();
    private DeviceAsyncResult<List<Device>> callback;
    private SSDPSearch search;
    private int timeout;

    public FindLocalDevicesRequest(int timeout, DeviceAsyncResult<List<Device>> callback) {
        this.timeout = timeout;
        this.callback = callback;
    }

    public void run() {
        this.performRequest();
    }

    public void performRequest() {
    }

    public void onResult(SSDPSearchResult result) {
    }

    public void onResults(List<SSDPSearchResult> ssdpResults) {
        LOG.info("device.findLocal() onSSDPSearchResults() size: " + ssdpResults.size());
        if(ssdpResults.size() == 0) {
            this.callback.onResult(this.deviceResults);
        } else {
            LOG.info("device.findLocal() ssdpSearchResultsSize: " + ssdpResults.size());

            try {
                Iterator e = ssdpResults.iterator();

                while(e.hasNext()) {
                    SSDPSearchResult ssdpSearchResult = (SSDPSearchResult)e.next();
                    LOG.info("ssdpSearchResult: " + ssdpSearchResult.getDeviceUri());
                    URI serviceUri = URI.create(ssdpSearchResult.getLocation());
                    DeviceResultAccumulator accumulator = new DeviceResultAccumulator(this.deviceResults, this.deviceErrors, ssdpResults.size(), this.callback);
                    GetDeviceRequest deviceRequest = new GetDeviceRequest(serviceUri, accumulator);
                    deviceRequest.run();
                }
            } catch (Exception var7) {
                LOG.info("device.findLocal() FAILED: " + var7);
                var7.printStackTrace();
            }

        }
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
