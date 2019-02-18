//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel.info;

import com.samsung.multiscreen.net.json.JSONUtil;
import java.util.Map;

public class ChannelInfo {
    private static final String KEY_ID = "id";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_HOSTCONNECTED = "hostConnected";
    private Map<String, Object> params;

    protected ChannelInfo(Map<String, Object> params) {
        this.params = params;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[ChannelInfo]").append(" ").append("id").append(": ").append(this.getId()).append(", ").append("endpoint").append(": ").append(this.getEndPoint());
        return builder.toString();
    }

    public String getId() {
        return (String)this.params.get("id");
    }

    public String getEndPoint() {
        return (String)this.params.get("endpoint");
    }

    public Boolean getHostConnected() {
        Boolean hostConnected = (Boolean)this.params.get("hostConnected");
        return hostConnected != null?hostConnected:Boolean.FALSE;
    }

    protected static ChannelInfo createWithJSONData(String jsonData) {
        Map map = JSONUtil.parse(jsonData);
        return createWithMap(map);
    }

    public static ChannelInfo createWithMap(Map<String, Object> map) {
        return new ChannelInfo(map);
    }
}
