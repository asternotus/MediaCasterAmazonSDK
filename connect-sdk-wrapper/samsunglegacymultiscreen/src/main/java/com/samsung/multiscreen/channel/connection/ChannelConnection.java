//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel.connection;

import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.connection.IChannelConnectionListener;
import com.samsung.multiscreen.net.json.JSONRPCMessage;

public abstract class ChannelConnection {
    private Channel channel;
    private IChannelConnectionListener connectionListener;

    protected ChannelConnection(Channel channel) {
        this.channel = channel;
    }

    public String toString() {
        String s = "[ChannelConnection]";
        s = s + " connected: " + this.isConnected();
        return s;
    }

    protected Channel getChannel() {
        return this.channel;
    }

    public void setListener(IChannelConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    protected IChannelConnectionListener getListener() {
        return this.connectionListener;
    }

    public abstract boolean isConnected();

    public abstract void connect();

    public abstract void disconnect();

    public abstract void send(JSONRPCMessage var1, boolean var2);
}
