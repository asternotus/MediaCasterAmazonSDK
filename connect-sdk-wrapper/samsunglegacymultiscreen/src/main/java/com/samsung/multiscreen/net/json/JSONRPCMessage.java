//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.json;

import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JSONRPCMessage {
    private static final String JSON_RPC_VERSION = "2.0";
    private static final String KEY_JSON_RPC = "jsonrpc";
    private static final String KEY_METHOD = "method";
    private static final String KEY_PARAMS = "params";
    private static final String KEY_RESULT = "result";
    private static final String KEY_ERROR = "error";
    public static String KEY_ID = "id";
    public static String KEY_TO = "to";
    public static String KEY_FROM = "from";
    public static String KEY_MESSAGE = "message";
    public static String KEY_ENCRYPTED = "encrypted";
    public static String KEY_CLIENT_ID = "clientId";
    public static String KEY_CLIENTS = "clients";
    public static String KEY_TIMEOUT = "timeout";
    public static String KEY_ALL = "all";
    public static String KEY_BROADCAST = "broadcast";
    public static String KEY_HOST = "host";
    private JSONRPCMessage.MessageType type;
    private Map<String, Object> map;

    public JSONRPCMessage(Map<String, Object> map) {
        this.map = map;
        if(map.get("result") != null) {
            this.type = JSONRPCMessage.MessageType.RESULT;
        } else if(map.get("error") != null) {
            this.type = JSONRPCMessage.MessageType.ERROR;
        } else if(map.get(KEY_ID) != null) {
            this.type = JSONRPCMessage.MessageType.MESSAGE;
        }

    }

    public JSONRPCMessage(String method) {
        this(JSONRPCMessage.MessageType.NOTIFICATION, method);
    }

    public JSONRPCMessage(JSONRPCMessage.MessageType type, String method) {
        this.type = type;
        this.map = new HashMap();
        this.map.put("jsonrpc", "2.0");
        if(type == JSONRPCMessage.MessageType.MESSAGE) {
            this.map.put(KEY_ID, UUID.randomUUID().toString());
        }

        this.map.put("method", method);
        this.map.put("params", new HashMap());
    }

    public String toString() {
        return this.toJSONString();
    }

    public boolean isResult() {
        return this.type == JSONRPCMessage.MessageType.RESULT;
    }

    public boolean isError() {
        return this.type == JSONRPCMessage.MessageType.ERROR;
    }

    public JSONRPCMessage.MessageType getType() {
        return this.type;
    }

    public String getId() {
        return (String)this.map.get(KEY_ID);
    }

    public String getMethod() {
        return (String)this.map.get("method");
    }

    public Map<String, Object> getParams() {
        return (Map)this.map.get("params");
    }

    public Map<String, Object> getResult() {
        Object result = this.map.get("result");
        if(result instanceof Map) {
            return (Map)result;
        } else if(result instanceof Boolean) {
            Boolean booleanResult = (Boolean)result;
            HashMap returnValue = new HashMap();
            returnValue.put("success", booleanResult);
            return returnValue;
        } else {
            return null;
        }
    }

    public JSONRPCError getError() {
        Map data = (Map)this.map.get("error");
        return JSONRPCError.createWithMap(data);
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.putAll(this.map);
        return json;
    }

    public String toJSONString() {
        return this.toJSON().toJSONString();
    }

    public static JSONRPCMessage createWithJSONData(String jsonData) {
        Map map = JSONUtil.parse(jsonData);
        return map == null?null:new JSONRPCMessage(map);
    }

    public static JSONRPCMessage createSendMessage(Object to, String message) {
        JSONRPCMessage rpcMessage = new JSONRPCMessage("ms.channel.sendMessage");
        rpcMessage.getParams().put(KEY_TO, to);
        rpcMessage.getParams().put(KEY_MESSAGE, message);
        return rpcMessage;
    }

    public static JSONRPCMessage createPingMessage(int timeout) {
        JSONRPCMessage rpcMessage = new JSONRPCMessage("ms.channel.ping");
        rpcMessage.getParams().put(KEY_TIMEOUT, Integer.valueOf(timeout));
        return rpcMessage;
    }

    public static enum MessageType {
        MESSAGE,
        NOTIFICATION,
        RESULT,
        ERROR;

        private MessageType() {
        }
    }
}
