//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel;

import com.samsung.multiscreen.net.json.JSONUtil;
import java.util.Map;

public class ChannelError {
    private int code = -1;
    private String message = "error";

    protected ChannelError() {
    }

    protected ChannelError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ChannelError(String message) {
        this.message = message;
    }

    public String toString() {
        String s = "[ChannelError]";
        s = s + " code: " + this.code;
        s = s + ", message: " + this.message;
        return s;
    }

    public int getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public static ChannelError createWithJSONData(String data) {
        Map params = JSONUtil.parse(data);
        ChannelError error = new ChannelError();
        error.code = ((Integer)params.get("code")).intValue();
        error.message = (String)params.get("message");
        return error;
    }
}
