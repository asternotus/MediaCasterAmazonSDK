//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.requests.GetDialDeviceDescriptorRequest;
import com.samsung.multiscreen.device.requests.GetDialDeviceRequest;
import com.samsung.multiscreen.device.requests.impl.DeviceResultAccumulator;
import com.samsung.multiscreen.device.requests.impl.DeviceURIResult;
import com.samsung.multiscreen.net.AsyncResult;
import com.samsung.multiscreen.net.ssdp.SSDPSearch;
import com.samsung.multiscreen.net.ssdp.SSDPSearchListener;
import com.samsung.multiscreen.net.ssdp.SSDPSearchResult;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FindLocalDialDevicesRequest implements SSDPSearchListener, Runnable {
    private static final Logger LOG = Logger.getLogger(FindLocalDialDevicesRequest.class.getName());
    protected static final String DIAL_URN = "urn:dial-multiscreen-org:service:dial:1";
    private List<Device> deviceResults = new ArrayList();
    private List<DeviceError> deviceErrors = new ArrayList();
    private DeviceAsyncResult<List<Device>> callback;
    private SSDPSearch search;
    private int timeout;
    private String targetVersion;

    public FindLocalDialDevicesRequest(int timeout, String targetVersion, DeviceAsyncResult<List<Device>> callback) {
        this.timeout = timeout;
        this.targetVersion = targetVersion;
        this.callback = callback;
    }

    public void run() {
        this.performRequest();
    }

    public void onResult(SSDPSearchResult result) {
    }

    public void onResults(List<SSDPSearchResult> ssdpResults) {
        LOG.info("Results() size: " + ssdpResults.size());
        if(ssdpResults.size() == 0) {
            this.callback.onResult(this.deviceResults);
        } else {
            LOG.info("DIAL ssdpSearchResultsSize: " + ssdpResults.size());
            List resultList = Collections.synchronizedList(new ArrayList());
            List errorList = Collections.synchronizedList(new ArrayList());
            final DeviceResultAccumulator deviceAccumulator = new DeviceResultAccumulator(resultList, errorList, ssdpResults.size(), this.callback);

            try {
                AsyncResult e = new AsyncResult<DeviceURIResult>() {
                    public void onResult(DeviceURIResult result) {
                        FindLocalDialDevicesRequest.LOG.info("Res: " + result.toString() + "\n");
                        GetDialDeviceRequest dialDeviceRequest = new GetDialDeviceRequest(result, deviceAccumulator);
                        dialDeviceRequest.run();
                    }

                    public void onException(Exception e) {
                        FindLocalDialDevicesRequest.LOG.info("FindLocalDevicesRequest: got exception: " + e.getLocalizedMessage());
                        deviceAccumulator.onError(new DeviceError(e.getMessage()));
                    }
                };
                Iterator i$ = ssdpResults.iterator();

                while(i$.hasNext()) {
                    SSDPSearchResult ssdpSearchResult = (SSDPSearchResult)i$.next();
                    LOG.info("DIAL search result: " + ssdpSearchResult);
                    URI serviceUri = URI.create(ssdpSearchResult.getLocation());
                    GetDialDeviceDescriptorRequest dialDeviceDescriptorRequest = new GetDialDeviceDescriptorRequest(serviceUri, this.targetVersion, e);
                    dialDeviceDescriptorRequest.run();
                }
            } catch (Exception var10) {
                LOG.info("FindLocalDialDevicesRequest FAILED: " + var10);
                var10.printStackTrace();
            }

        }
    }

    protected void performRequest() {
        this.search = new SSDPSearch("urn:dial-multiscreen-org:service:dial:1");
//        this.search = new SSDPSearch(targetVersion);
        this.search.start(this.timeout, this);
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
