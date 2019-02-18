//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel;

import com.mega.cast.utils.log.SmartLog;
import com.samsung.multiscreen.net.json.JSONRPCMessage;
import com.samsung.multiscreen.net.json.JSONUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class ChannelMessageHandler {
    private static final Logger LOG = Logger.getLogger(ChannelMessageHandler.class.getName());
    private static final String LOG_TAG = ChannelMessageHandler.class.getSimpleName();
    private Channel channel;

    ChannelMessageHandler(Channel channel) {
        this.channel = channel;
    }

    protected void handleMessage(JSONRPCMessage rpcMessage) {
        if (rpcMessage != null) {
            String method = rpcMessage.getMethod();
            LOG.info("ChannelMessageHandler.handleMessage() method: " + method);
            if (method != null) {
                LOG.info("CHANNEL_CONNECT: ms.channel.onConnect");
                if (method.equals("ms.channel.onConnect")) {
                    this.handleConnect(rpcMessage);
                } else if (method.equals("ms.channel.onClientConnect")) {
                    this.handleClientConnected(rpcMessage);
                } else if (method.equals("ms.channel.onClientDisconnect")) {
                    this.handleClientDisconnected(rpcMessage);
                } else if (method.equals("ms.channel.onClientMessage")) {
                    this.handleClientMessage(rpcMessage);
                } else {
                    LOG.info("ChannelMessageHandler.handleMessage() NO HANDLER");
                }

            }
        }
    }

    protected void handleConnect(JSONRPCMessage rpcMessage) {
        LOG.info("ChannelMessageHandler.handleConnect()");
        String clientId = (String) rpcMessage.getParams().get(JSONRPCMessage.KEY_CLIENT_ID);
        LOG.info("ChannelMessageHandler.handleConnect() clientId: " + clientId);
        ArrayList clients = new ArrayList();
        List clientMaps = (List) rpcMessage.getParams().get(JSONRPCMessage.KEY_CLIENTS);
        Iterator i$ = clientMaps.iterator();

        while (i$.hasNext()) {
            Map clientMap = (Map) i$.next();
            ChannelClient client = new ChannelClient(this.channel, clientMap);
            clients.add(client);
        }

        this.channel.handleConnect(clientId, clients);
    }

    protected void handleClientConnected(JSONRPCMessage rpcMessage) {
        SmartLog.d(LOG_TAG, "handleClientConnected ");
        ChannelClient client = new ChannelClient(this.channel, rpcMessage.getParams());
        this.channel.getClients().add(client);
        if (this.channel.getListener() != null) {
            this.channel.getListener().onClientConnected(client);
        }

    }

    protected void handleClientDisconnected(JSONRPCMessage rpcMessage) {
        SmartLog.d(LOG_TAG, "handleClientDisconnected ");
        String clientId = (String) rpcMessage.getParams().get(JSONRPCMessage.KEY_ID);
        ChannelClient client = this.channel.getClients().get(clientId);
        if (client != null) {
            this.channel.getClients().remove(client);
            if (this.channel.getListener() != null) {
                this.channel.getListener().onClientDisconnected(client);
            }

        }
    }

    protected void handleClientMessage(JSONRPCMessage rpcMessage) {
        SmartLog.d(LOG_TAG, "handleClientMessage ");
        String clientId = (String) rpcMessage.getParams().get(JSONRPCMessage.KEY_FROM);
        ChannelClient client = this.channel.getClients().get(clientId);
        if (client != null) {
            String message = (String) rpcMessage.getParams().get(JSONRPCMessage.KEY_MESSAGE);
            if (this.channel.getListener() != null) {
                this.channel.getListener().onClientMessage(client, message);
            }

        }
    }

    static JSONRPCMessage createWithJSONData(String jsonData) {
        Map map = JSONUtil.parse(jsonData);
        JSONRPCMessage rpcMessage = new JSONRPCMessage(map);
        return rpcMessage;
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
