//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import java.util.Collections;
import java.util.Map;

public class Client {
    private static final String ID_KEY = "id";
    private static final String IS_HOST_KEY = "isHost";
    private static final String CONNECT_TIME_KEY = "connectTime";
    private static final String ATTRIBUTES_KEY = "attributes";
    private final Channel channel;
    private final String id;
    private final boolean host;
    private final long connectTime;
    private final Map<String, String> attributes;

    protected static Client create(Channel channel, Map<String, Object> data) {
        String id = (String)data.get("id");
        Boolean host = (Boolean)data.get("isHost");
        Long connectTime = (Long)data.get("connectTime");
        Map attributes = Collections.unmodifiableMap((Map)data.get("attributes"));
        return new Client(channel, id, host.booleanValue(), connectTime.longValue(), attributes);
    }

    public Channel getChannel() {
        return this.channel;
    }

    public String getId() {
        return this.id;
    }

    public boolean isHost() {
        return this.host;
    }

    public long getConnectTime() {
        return this.connectTime;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    private Client(Channel channel, String id, boolean host, long connectTime, Map<String, String> attributes) {
        this.channel = channel;
        this.id = id;
        this.host = host;
        this.connectTime = connectTime;
        this.attributes = attributes;
    }

    public String toString() {
        return "Client(id=" + this.getId() + ", host=" + this.isHost() + ", connectTime=" + this.getConnectTime() + ", attributes=" + this.getAttributes() + ")";
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof Client)) {
            return false;
        } else {
            Client other = (Client)o;
            if(!other.canEqual(this)) {
                return false;
            } else {
                String this$id = this.getId();
                String other$id = other.getId();
                if(this$id == null) {
                    if(other$id != null) {
                        return false;
                    }
                } else if(!this$id.equals(other$id)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Client;
    }

    public int hashCode() {
        boolean PRIME = true;
        byte result = 1;
        String $id = this.getId();
        int result1 = result * 59 + ($id == null?0:$id.hashCode());
        return result1;
    }
}
