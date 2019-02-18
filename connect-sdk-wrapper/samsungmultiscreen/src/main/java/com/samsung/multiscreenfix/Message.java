//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import java.util.Arrays;

public class Message {
    public static final String TARGET_BROADCAST = "broadcast";
    public static final String TARGET_ALL = "all";
    public static final String TARGET_HOST = "host";
    static final String MEHOD_APPLICATION_GET = "ms.application.get";
    static final String MEHOD_APPLICATION_START = "ms.application.start";
    static final String MEHOD_APPLICATION_STOP = "ms.application.stop";
    static final String MEHOD_APPLICATION_INSTALL = "ms.application.install";
    static final String MEHOD_WEB_APPLICATION_GET = "ms.webapplication.get";
    static final String MEHOD_WEB_APPLICATION_START = "ms.webapplication.start";
    static final String MEHOD_WEB_APPLICATION_STOP = "ms.webapplication.stop";
    static final String METHOD_EMIT = "ms.channel.emit";
    public static final String PROPERTY_MESSAGE_ID = "id";
    static final String PROPERTY_MESSAGE = "message";
    static final String PROPERTY_METHOD = "method";
    static final String PROPERTY_PARAMS = "params";
    static final String PROPERTY_ID = "id";
    static final String PROPERTY_URL = "url";
    static final String PROPERTY_ARGS = "args";
    static final String PROPERTY_EVENT = "event";
    static final String PROPERTY_DATA = "data";
    static final String PROPERTY_TO = "to";
    static final String PROPERTY_FROM = "from";
    static final String PROPERTY_CLIENTS = "clients";
    static final String PROPERTY_RESULT = "result";
    static final String PROPERTY_ERROR = "error";
    static final String PROPERTY_OS = "os";
    static final String PROPERTY_LIBRARY = "library";
    static final String PROPERTY_VERSION = "version";
    static final String PROPERTY_APP_NAME = "appName";
    static final String PROPERTY_MOD_NUMBER = "modelNumber";
    private final Channel channel;
    private final String event;
    private final Object data;
    private final Client from;
    private final byte[] payload;

    public Channel getChannel() {
        return this.channel;
    }

    public String getEvent() {
        return this.event;
    }

    public Object getData() {
        return this.data;
    }

    public Client getFrom() {
        return this.from;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof Message)) {
            return false;
        } else {
            Message other = (Message)o;
            if(!other.canEqual(this)) {
                return false;
            } else {
                label63: {
                    Channel this$channel = this.getChannel();
                    Channel other$channel = other.getChannel();
                    if(this$channel == null) {
                        if(other$channel == null) {
                            break label63;
                        }
                    } else if(this$channel.equals(other$channel)) {
                        break label63;
                    }

                    return false;
                }

                String this$event = this.getEvent();
                String other$event = other.getEvent();
                if(this$event == null) {
                    if(other$event != null) {
                        return false;
                    }
                } else if(!this$event.equals(other$event)) {
                    return false;
                }

                Object this$data = this.getData();
                Object other$data = other.getData();
                if(this$data == null) {
                    if(other$data != null) {
                        return false;
                    }
                } else if(!this$data.equals(other$data)) {
                    return false;
                }

                label42: {
                    Client this$from = this.getFrom();
                    Client other$from = other.getFrom();
                    if(this$from == null) {
                        if(other$from == null) {
                            break label42;
                        }
                    } else if(this$from.equals(other$from)) {
                        break label42;
                    }

                    return false;
                }

                if(!Arrays.equals(this.getPayload(), other.getPayload())) {
                    return false;
                } else {
                    return true;
                }
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Message;
    }

    public int hashCode() {
        boolean PRIME = true;
        byte result = 1;
        Channel $channel = this.getChannel();
        int result1 = result * 59 + ($channel == null?0:$channel.hashCode());
        String $event = this.getEvent();
        result1 = result1 * 59 + ($event == null?0:$event.hashCode());
        Object $data = this.getData();
        result1 = result1 * 59 + ($data == null?0:$data.hashCode());
        Client $from = this.getFrom();
        result1 = result1 * 59 + ($from == null?0:$from.hashCode());
        result1 = result1 * 59 + Arrays.hashCode(this.getPayload());
        return result1;
    }

    Message(Channel channel, String event, Object data, Client from, byte[] payload) {
        if(channel == null) {
            throw new NullPointerException("channel");
        } else if(event == null) {
            throw new NullPointerException("event");
        } else if(from == null) {
            throw new NullPointerException("from");
        } else {
            this.channel = channel;
            this.event = event;
            this.data = data;
            this.from = from;
            this.payload = payload;
        }
    }

    public String toString() {
        return "Message(event=" + this.getEvent() + ", data=" + this.getData() + ", from=" + this.getFrom() + ")";
    }
}
