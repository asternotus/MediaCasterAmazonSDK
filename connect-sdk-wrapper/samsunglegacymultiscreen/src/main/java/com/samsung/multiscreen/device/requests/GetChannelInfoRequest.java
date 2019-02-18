//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.samsung.multiscreen.channel.info.ChannelInfo;
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

public class GetChannelInfoRequest implements Runnable {
    private static final Logger LOG = Logger.getLogger(GetChannelInfoRequest.class.getName());
    private URI restEndpoint;
    private String channelId;
    private DeviceAsyncResult<ChannelInfo> callback;

    public GetChannelInfoRequest(URI endpoint, String channelId, DeviceAsyncResult<ChannelInfo> callback) {
        this.restEndpoint = endpoint;
        this.channelId = channelId;
        this.callback = callback;
    }

    public void run() {
        this.performRequest();
    }

    private void performRequest() {
        JSONRPCMessage message = new JSONRPCMessage(MessageType.MESSAGE, "ms.device.getChannelInfo");
        message.getParams().put("id", this.channelId);
        HttpSyncClient client = new HttpSyncClient();

        try {
            URL e = this.restEndpoint.toURL();
            Map headers = HttpSyncClient.initJSONPostHeaders(e);
            Response response = client.post(e, headers, message.toJSONString().getBytes("UTF-8"));
            if(response == null) {
                this.callback.onError(new DeviceError(client.getLastErrorMessage()));
                return;
            }

            if(response.status == 200) {
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

    protected void parseResponse(Response response) {
        try {
            String e = new String(response.body, "UTF-8");
            JSONRPCMessage rpcMessage = JSONRPCMessage.createWithJSONData(e);
            LOG.info("GetChannelRequest result rpcMessage: " + rpcMessage);
            if(rpcMessage.isError()) {
                LOG.info("GetChannelRequest result rpc error: " + rpcMessage.getError());
                this.callback.onError(DeviceError.createWithJSONRPCError(rpcMessage.getError()));
            } else {
                ChannelInfo channelInfo = ChannelInfo.createWithMap(rpcMessage.getResult());
                this.callback.onResult(channelInfo);
            }
        } catch (UnsupportedEncodingException var5) {
            this.callback.onError(new DeviceError(var5.getLocalizedMessage()));
        }

    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
