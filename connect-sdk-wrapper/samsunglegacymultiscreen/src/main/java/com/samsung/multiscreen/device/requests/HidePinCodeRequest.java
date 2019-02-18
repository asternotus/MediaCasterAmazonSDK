//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
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

public class HidePinCodeRequest implements Runnable {
    private static final Logger LOG = Logger.getLogger(HidePinCodeRequest.class.getName());
    private URI restEndpoint;
    private DeviceAsyncResult<Boolean> callback;

    public HidePinCodeRequest(URI endpoint, DeviceAsyncResult<Boolean> callback) {
        this.restEndpoint = endpoint;
        this.callback = callback;
    }

    public void run() {
        this.performRequest();
    }

    private void performRequest() {
        try {
            JSONRPCMessage e = new JSONRPCMessage(MessageType.MESSAGE, "ms.device.hidePinCode");
            URL url = this.restEndpoint.toURL();
            Map headers = HttpSyncClient.initJSONPostHeaders(url);
            HttpSyncClient client = new HttpSyncClient();
            client.setReadTimeout(10000);
            Response response = client.post(url, headers, e.toJSONString().getBytes("UTF-8"));
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
        try {
            String e = new String(response.body, "UTF-8");
            JSONRPCMessage rpcMessage = JSONRPCMessage.createWithJSONData(e);
            LOG.info("HidePinCodeRequest result rpcMessage: " + rpcMessage);
            if(rpcMessage.isError()) {
                LOG.info("HidePinCodeRequest result rpc error: " + rpcMessage.getError());
                this.callback.onError(DeviceError.createWithJSONRPCError(rpcMessage.getError()));
            } else {
                boolean success = ((Boolean)rpcMessage.getResult().get("success")).booleanValue();
                this.callback.onResult(Boolean.valueOf(success));
            }
        } catch (UnsupportedEncodingException var5) {
            this.callback.onError(new DeviceError(var5.getLocalizedMessage()));
        }

    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
