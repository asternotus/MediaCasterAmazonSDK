//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.DeviceFactory;
import com.samsung.multiscreen.device.requests.impl.DeviceURIResult;
import com.samsung.multiscreen.net.http.client.HttpSyncClient;
import com.samsung.multiscreen.net.http.client.Response;
import com.samsung.multiscreen.net.json.JSONRPCMessage;
import com.samsung.multiscreen.net.json.JSONRPCMessage.MessageType;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetDialDeviceRequest implements Runnable {
    private static final Logger LOG = Logger.getLogger(GetDialDeviceRequest.class.getName());
    private boolean responseReceived;
    private DeviceAsyncResult<Device> callback;
    private DeviceURIResult deviceURIs;

    public GetDialDeviceRequest(DeviceURIResult deviceURIs, DeviceAsyncResult<Device> callback) {
        this.deviceURIs = deviceURIs;
        this.callback = callback;
        this.responseReceived = false;
    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        JSONRPCMessage message = new JSONRPCMessage(MessageType.MESSAGE, "ms.device.getInfo");
        HttpSyncClient client = new HttpSyncClient();

        try {
            URL e = this.deviceURIs.getServiceURI().toURL();
            Map headers = HttpSyncClient.initJSONPostHeaders(e);
            client.setReadTimeout(2000);
            Response response = client.post(e, headers, message.toJSONString().getBytes("UTF-8"));
            if(response == null) {
                this.callback.onError(new DeviceError(client.getLastErrorMessage()));
                return;
            }

            if(response.status == 200) {
                this.handleResponse(response);
            } else {
                this.callback.onError(new DeviceError(response.message));
            }
        } catch (MalformedURLException var6) {
            this.callback.onError(new DeviceError(var6.getLocalizedMessage()));
        } catch (UnsupportedEncodingException var7) {
            this.callback.onError(new DeviceError(var7.getLocalizedMessage()));
        }

    }

    protected void handleResponse(Response response) {
        if(!this.responseReceived) {
            this.responseReceived = true;
            LOG.info("response() status: " + response.status);
            if(response.status == 200) {
                try {
                    String e = new String(response.body, "UTF-8");
                    JSONRPCMessage rpcMessage = JSONRPCMessage.createWithJSONData(e);
                    LOG.info("getDevice() rpcMessage: " + rpcMessage);
                    if(rpcMessage.isError()) {
                        this.callback.onError(DeviceError.createWithJSONRPCError(rpcMessage.getError()));
                    } else {
                        Device device = DeviceFactory.createWithMap(rpcMessage.getResult());
                        if(device != null) {
                            this.callback.onResult(device);
                        } else {
                            LOG.info("GetDialDeviceRequest FAILED TO CREATE DEVICE");
                            this.callback.onError(new DeviceError("Could not create device"));
                        }
                    }
                } catch (UnsupportedEncodingException var5) {
                    this.callback.onError(new DeviceError("Could not create device"));
                }
            } else {
                this.callback.onError(new DeviceError(response.message));
            }

        }
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
