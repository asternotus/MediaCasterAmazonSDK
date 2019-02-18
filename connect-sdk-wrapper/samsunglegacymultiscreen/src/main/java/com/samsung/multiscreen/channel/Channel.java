//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel;

import com.samsung.multiscreen.channel.ChannelAsyncResult;
import com.samsung.multiscreen.channel.ChannelClient;
import com.samsung.multiscreen.channel.ChannelClients;
import com.samsung.multiscreen.channel.ChannelConnectionListener;
import com.samsung.multiscreen.channel.ChannelError;
import com.samsung.multiscreen.channel.IChannelListener;
import com.samsung.multiscreen.channel.connection.ChannelConnection;
import com.samsung.multiscreen.channel.connection.ConnectionFactory;
import com.samsung.multiscreen.channel.connection.IChannelConnectionListener;
import com.samsung.multiscreen.channel.info.ChannelInfo;
import com.samsung.multiscreen.net.json.JSONRPCMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Channel {
    private static final Logger LOG = Logger.getLogger(Channel.class.getName());
    private ChannelInfo channelInfo;
    private IChannelListener channelListener;
    private ChannelConnection connection;
    private ConnectionFactory factory;
    private ChannelClients clients;
    private ChannelAsyncResult<Boolean> connectCallback;
    private ChannelAsyncResult<Boolean> disconnectCallback;

    public Channel(ChannelInfo channelInfo, ConnectionFactory factory) {
        this.channelInfo = channelInfo;
        this.factory = factory;
        this.clients = new ChannelClients();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[Channel]").append("\nconnected: ").append(this.isConnected()).append("\nchannelInfo: ").append(this.channelInfo).append("\nconnection: ").append(this.connection).append("\nclients: ").append(this.clients);
        return builder.toString();
    }

    private void shutdown() {
        this.clients.clear();
        this.connection = null;
    }

    public void setListener(IChannelListener channelListener) {
        this.channelListener = channelListener;
    }

    protected IChannelListener getListener() {
        return this.channelListener;
    }

    public ChannelClients getClients() {
        return this.clients;
    }

    protected ChannelConnection getConnection() {
        return this.connection;
    }

    public boolean isConnected() {
        return this.connection != null && this.connection.isConnected();
    }

    public void connect() {
        this.connect((Map)null, (ChannelAsyncResult)null);
    }

    public void connect(Map<String, String> clientAttributes) {
        this.connect(clientAttributes, (ChannelAsyncResult)null);
    }

    public void connect(ChannelAsyncResult<Boolean> callback) {
        this.connect((Map)null, callback);
    }

    public void connect(Map<String, String> clientAttributes, ChannelAsyncResult<Boolean> callback) {
        LOG.info("Channel.connect() connected: " + this.isConnected() + ", clientAttributes: " + clientAttributes);
        if(this.isConnected()) {
            if(callback != null) {
                callback.onError(new ChannelError(-1, "Already Connected"));
            }

        } else {
            this.connectCallback = callback;
            this.connection = this.factory.getConnection(this, this.channelInfo, clientAttributes);
            this.connection.setListener(new ChannelConnectionListener(this));
            this.connection.connect();
        }
    }

    public void disconnect() {
        this.disconnect((ChannelAsyncResult)null);
    }

    public void disconnect(ChannelAsyncResult<Boolean> callback) {
        LOG.info("Channel.disconnect() connected: " + this.isConnected());
        if(!this.isConnected()) {
            if(callback != null) {
                callback.onError(new ChannelError(-1, "Not Connected"));
            }

        } else {
            this.disconnectCallback = callback;
            this.connection.disconnect();
        }
    }

    public void broadcast(String message) {
        this.broadcast(message, false);
    }

    public void broadcast(String message, boolean encryptMessage) {
        LOG.info("Channel.broadcast() message: " + message);
        if(this.isConnected()) {
            JSONRPCMessage rpcMessage = JSONRPCMessage.createSendMessage(JSONRPCMessage.KEY_BROADCAST, message);
            this.connection.send(rpcMessage, encryptMessage);
        }
    }

    public void sendToHost(String message) {
        this.sendToHost(message, false);
    }

    public void sendToHost(String message, boolean encryptMessage) {
        LOG.info("Channel.sendToHost() message: " + message);
        if(this.isConnected()) {
            JSONRPCMessage rpcMessage = JSONRPCMessage.createSendMessage(JSONRPCMessage.KEY_HOST, message);
            this.connection.send(rpcMessage, encryptMessage);
        }
    }

    public void sendToClient(ChannelClient client, String message) {
        this.sendToClient(client, message, false);
    }

    public void sendToClient(ChannelClient client, String message, boolean encryptMessage) {
        LOG.info("Channel.sendToClient() client: " + client.getId() + ", message: " + message);
        if(this.isConnected()) {
            JSONRPCMessage rpcMessage = JSONRPCMessage.createSendMessage(client.getId(), message);
            this.connection.send(rpcMessage, encryptMessage);
        }
    }

    public void sendToAll(String message) {
        this.sendToAll(message, false);
    }

    public void sendToAll(String message, boolean encryptMessage) {
        LOG.info("Channel.sendToAll() message: " + message);
        if(this.isConnected()) {
            JSONRPCMessage rpcMessage = JSONRPCMessage.createSendMessage(JSONRPCMessage.KEY_ALL, message);
            this.connection.send(rpcMessage, encryptMessage);
        }
    }

    public void sendToClientList(List<ChannelClient> clientList, String message) {
        this.sendToClientList(clientList, message, false);
    }

    public void sendToClientList(List<ChannelClient> clientList, String message, boolean encryptMessage) {
        LOG.info("Channel.sendToClientList() message: " + message);
        if(this.isConnected()) {
            ArrayList clientIds = new ArrayList();
            Iterator rpcMessage = clientList.iterator();

            while(rpcMessage.hasNext()) {
                ChannelClient client = (ChannelClient)rpcMessage.next();
                clientIds.add(client.getId());
            }

            JSONRPCMessage rpcMessage1 = JSONRPCMessage.createSendMessage(clientIds, message);
            this.connection.send(rpcMessage1, encryptMessage);
        }
    }

    protected ChannelInfo getChannelInfo() {
        return this.channelInfo;
    }

    protected void handleConnect(String myClientId, List<ChannelClient> clientList) {
        LOG.info("Channel.handleConnect()");
        this.clients.reset(myClientId, clientList);
        if(this.connectCallback != null) {
            this.connectCallback.onResult(Boolean.TRUE);
            this.connectCallback = null;
        }

        if(this.channelListener != null) {
            this.channelListener.onConnect();
        }

    }

    protected void handleConnectError(ChannelError error) {
        LOG.info("Channel.handleConnectError() error: " + error);
        this.shutdown();
        if(this.connectCallback != null) {
            this.connectCallback.onError(error);
            this.connectCallback = null;
        }

    }

    protected void handleDisconnect() {
        LOG.info("Channel.handleDisconnect() channelListener: " + this.channelListener);
        this.connection.setListener((IChannelConnectionListener)null);
        this.shutdown();
        if(this.disconnectCallback != null) {
            this.disconnectCallback.onResult(Boolean.TRUE);
            this.disconnectCallback = null;
        }

        if(this.channelListener != null) {
            this.channelListener.onDisconnect();
        }

    }

    protected void handleDisconnectError(ChannelError error) {
        LOG.info("Channel.handleDisconnectError() " + error);
        if(this.disconnectCallback != null) {
            this.disconnectCallback.onError(error);
            this.disconnectCallback = null;
        }

    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
