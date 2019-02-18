//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel;

import com.samsung.multiscreen.channel.Channel;
import java.util.Map;

public class ChannelClient {
    static String KEY_ID = "id";
    static String KEY_HOST = "isHost";
    static String KEY_CONNECT_TIME = "connectTime";
    static String KEY_ATTRIBUTES = "attributes";
    private Channel channel;
    private Map<String, Object> params;

    protected ChannelClient(Channel channel, Map<String, Object> params) {
        this.channel = channel;
        this.params = params;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[ChannelClient]").append(" id: ").append(this.getId()).append(", isHost: ").append(this.isHost()).append(", connectTime: ").append(this.getConnectTime()).append(", attributes: ").append(this.getAttributes());
        return builder.toString();
    }

    public boolean isMe() {
        return this.equals(this.channel.getClients().me());
    }

    public String getId() {
        return (String)this.params.get(KEY_ID);
    }

    public boolean isHost() {
        return ((Boolean)this.params.get(KEY_HOST)).booleanValue();
    }

    public long getConnectTime() {
        return ((Long)this.params.get(KEY_CONNECT_TIME)).longValue();
    }

    public Map<String, String> getAttributes() {
        return (Map)this.params.get(KEY_ATTRIBUTES);
    }

    public void send(String message) {
        this.send(message, false);
    }

    public void send(String message, boolean encryptMessage) {
        this.channel.sendToClient(this, message, encryptMessage);
    }
}
