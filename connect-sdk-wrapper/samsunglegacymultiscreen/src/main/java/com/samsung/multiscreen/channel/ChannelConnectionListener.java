//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel;

import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.ChannelError;
import com.samsung.multiscreen.channel.ChannelMessageHandler;
import com.samsung.multiscreen.channel.connection.IChannelConnectionListener;
import com.samsung.multiscreen.net.json.JSONRPCMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

class ChannelConnectionListener implements IChannelConnectionListener {
    public static final Logger LOG = Logger.getLogger(ChannelConnectionListener.class.getName());
    private Channel channel;
    private ChannelMessageHandler messageHandler;

    ChannelConnectionListener(Channel channel) {
        this.channel = channel;
        this.messageHandler = new ChannelMessageHandler(channel);
    }

    public void onConnect() {
        LOG.info("ChannelConnectionListener.onConnect()");
    }

    public void onConnectError(ChannelError error) {
        LOG.info("ChannelConnectionListener.onConnectError()");
        this.channel.handleConnectError(error);
    }

    public void onDisconnect() {
        LOG.info("ChannelConnectionListener.onDisconnect()");
        this.channel.handleDisconnect();
    }

    public void onDisconnectError(ChannelError error) {
        LOG.info("ChannelConnectionListener.onDisconnectError()");
        this.channel.handleDisconnectError(error);
    }

    public void onMessage(JSONRPCMessage message) {
        LOG.info("ChannelConnectionListener.onMessage() rpcMessage: " + message);
        this.messageHandler.handleMessage(message);
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
