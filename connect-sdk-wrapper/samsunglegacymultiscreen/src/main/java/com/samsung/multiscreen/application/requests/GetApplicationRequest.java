//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.application.requests;

import com.samsung.multiscreen.application.Application;
import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.application.Application.Status;
import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.net.dial.DialApplication;
import com.samsung.multiscreen.net.dial.DialClient;
import java.net.URI;

public class GetApplicationRequest implements ApplicationAsyncResult<DialApplication>, Runnable {
    private String runTitle;
    private Device device;
    private URI appURI;
    private DialClient dialClient;
    private DeviceAsyncResult<Application> callback;

    public GetApplicationRequest(String runTitle, URI appURI, Device device, DialClient dialClient, DeviceAsyncResult<Application> callback) {
        this.runTitle = runTitle;
        this.appURI = appURI;
        this.device = device;
        this.dialClient = dialClient;
        this.callback = callback;
    }

    public void onResult(DialApplication dialApplication) {
        if(dialApplication != null) {
            Application application = this.createApplication(dialApplication);
            this.callback.onResult(application);
        } else {
            this.callback.onError(new DeviceError("not found"));
        }

    }

    public void onError(ApplicationError e) {
        this.callback.onError(new DeviceError(e.getCode(), e.getMessage()));
    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        this.dialClient.getApplication(this.runTitle, this);
    }

    protected Application createApplication(DialApplication dialApplication) {
        String state = dialApplication.getState().toLowerCase();
        Status initialStatus = Status.statusFromString(state);
        String installURL = "";
        if(initialStatus.equals(Status.INSTALLABLE)) {
            String[] parts = dialApplication.getState().split("=");
            if(parts.length == 2) {
                installURL = parts[1];
            }
        }

        return new Application(this.device, this.appURI, this.runTitle, initialStatus, dialApplication.getRelLink(), installURL);
    }
}
