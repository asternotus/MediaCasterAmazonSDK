//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.json;

import com.samsung.multiscreen.net.json.JSONUtil;
import java.util.Map;

public class JSONRPCError {
    private long code = -1L;
    private String message = "error";

    public JSONRPCError() {
    }

    public JSONRPCError(long code, String message) {
        this.code = code;
        this.message = message;
    }

    public String toString() {
        String s = "[JSONRPCError]";
        s = s + " code: " + this.code;
        s = s + ", message: " + this.message;
        return s;
    }

    public long getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public static JSONRPCError createWithJSONData(String data) {
        Map map = JSONUtil.parse(data);
        return createWithMap(map);
    }

    public static JSONRPCError createWithMap(Map<String, Object> data) {
        JSONRPCError error = new JSONRPCError();

        try {
            error.code = ((Long)data.get("code")).longValue();
            error.message = (String)data.get("message");
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        return error;
    }
}
