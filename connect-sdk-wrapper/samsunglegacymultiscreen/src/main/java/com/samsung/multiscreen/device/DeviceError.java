//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device;

import com.samsung.multiscreen.net.json.JSONRPCError;
import com.samsung.multiscreen.net.json.JSONUtil;
import java.util.Map;

public class DeviceError {
    private long code;
    private String message;

    public DeviceError() {
        this.code = -1L;
        this.message = "error";
    }

    public DeviceError(String message) {
        this(-1L, message);
    }

    public DeviceError(long code, String message) {
        this.code = -1L;
        this.message = "error";
        this.code = code;
        this.message = message;
    }

    public String toString() {
        String s = "[DeviceError]";
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

    public static DeviceError createWithJSONData(String data) {
        Map params = JSONUtil.parse(data);
        DeviceError error = new DeviceError();
        error.code = (long)((Integer)params.get("code")).intValue();
        error.message = (String)params.get("message");
        return error;
    }

    public static DeviceError createWithJSONRPCError(JSONRPCError rpcError) {
        return new DeviceError(rpcError.getCode(), rpcError.getMessage());
    }

    public static DeviceError createWithException(Exception e) {
        return new DeviceError(e.getLocalizedMessage());
    }
}
