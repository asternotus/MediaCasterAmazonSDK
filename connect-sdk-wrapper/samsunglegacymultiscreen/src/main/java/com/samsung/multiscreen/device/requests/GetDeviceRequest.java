//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.mega.cast.utils.log.SmartLog;
import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.DeviceFactory;
import com.samsung.multiscreen.net.http.client.HttpSyncClient;
import com.samsung.multiscreen.net.http.client.Response;
import com.samsung.multiscreen.net.json.JSONRPCMessage;
import com.samsung.multiscreen.net.json.JSONRPCMessage.MessageType;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetDeviceRequest implements Runnable {
    private static final Logger LOG = Logger.getLogger(GetDeviceRequest.class.getName());
    private static final String LOG_TAG = GetDeviceRequest.class.getSimpleName();

    private DeviceAsyncResult<Device> callback;
    private URI requestURI;

    public GetDeviceRequest(URI requestURI, DeviceAsyncResult<Device> callback) {
        this.requestURI = requestURI;
        this.callback = callback;
    }

    public void setRequestURI(URI requestURI) {
        this.requestURI = requestURI;
    }

    public void setCallback(DeviceAsyncResult<Device> callback) {
        this.callback = callback;
    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        JSONRPCMessage message = new JSONRPCMessage(MessageType.MESSAGE, "ms.device.getInfo");

        try {
            URL e = this.requestURI.toURL();
            Map headers = HttpSyncClient.initJSONPostHeaders(e);
            HttpSyncClient client = new HttpSyncClient();
            client.setReadTimeout(10 * 1000);

            Response response = client.post(e, headers, message.toJSONString().getBytes("UTF-8"));
            if (response == null) {
                this.callback.onError(new DeviceError(client.getLastErrorMessage()));
                return;
            }

            if (response.status == 200) {
                this.parseResponse(response);
            } else {
                this.callback.onError(new DeviceError(response.message));
            }
        } catch (MalformedURLException var6) {
            this.callback.onError(new DeviceError(var6.getLocalizedMessage()));
        } catch (UnsupportedEncodingException var7) {
            this.callback.onError(new DeviceError(var7.getLocalizedMessage()));
        }

    }

    private void parseResponse(Response response) {
        try {
            String e = new String(response.body, "UTF-8");
            JSONRPCMessage rpcMessage = JSONRPCMessage.createWithJSONData(e);
            if (rpcMessage.isError()) {
                this.callback.onError(DeviceError.createWithJSONRPCError(rpcMessage.getError()));
            } else {
                Device device = DeviceFactory.createWithMap(rpcMessage.getResult());
                if (device != null) {
                    this.callback.onResult(device);
                } else {
                    this.callback.onError(new DeviceError("Could not create device"));
                }
            }
        } catch (UnsupportedEncodingException var5) {
            var5.printStackTrace();
        }

    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
