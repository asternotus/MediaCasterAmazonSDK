//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.DeviceFactory;
import com.samsung.multiscreen.net.http.client.HttpSyncClient;
import com.samsung.multiscreen.net.http.client.Response;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FindDeviceByCodeRequest implements Runnable {
    private static final Logger LOG = Logger.getLogger(FindDeviceByCodeRequest.class.getName());
    private String targetVersion;
    private DeviceAsyncResult<Device> callback;
    private URI requestURI;

    public FindDeviceByCodeRequest(URI requestURI, String targetVersion, DeviceAsyncResult<Device> callback) {
        this.requestURI = requestURI;
        this.targetVersion = targetVersion;
        this.callback = callback;
    }

    public void run() {
        try {
            this.performRequest(this.requestURI.toURL());
        } catch (MalformedURLException var2) {
            this.callback.onError(new DeviceError("Invalid request URL"));
        }

    }

    protected void performRequest(URL requestURL) {
        HttpSyncClient client = new HttpSyncClient();
        client.setReadTimeout(20000);
        Map headers = HttpSyncClient.initJSONGetHeaders(requestURL);
        Response response = client.get(requestURL, headers);
        if(response == null) {
            this.callback.onError(new DeviceError(client.getLastErrorMessage()));
        } else {
            if(response.status == 200) {
                this.parseReponse(response);
            } else {
                LOG.info("FindDeviceByCodeRequest ERROR: " + response.status);
                this.callback.onError(new DeviceError(response.message));
            }

        }
    }

    protected void parseReponse(Response response) {
        try {
            String e = new String(response.body, "UTF-8");
            Device cloudDevice = DeviceFactory.parseDeviceWithCapability(e, this.targetVersion);
            if(cloudDevice == null) {
                LOG.info("FindDeviceByCodeRequest readResponse() FAILED TO CREATE DEVICE");
                this.callback.onError(new DeviceError("Could not create device"));
            } else {
                this.callback.onResult(cloudDevice);
            }
        } catch (UnsupportedEncodingException var4) {
            this.callback.onError(new DeviceError("Unable to parse device data"));
        }

    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
