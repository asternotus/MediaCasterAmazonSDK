//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.http.client;

import java.util.List;
import java.util.Map;

public class Response {
    public int status;
    public Map<String, List<String>> headers;
    public String message;
    public byte[] body;

    public Response(int status, Map<String, List<String>> headers, String message, byte[] bodyBytes) {
        this.status = status;
        this.headers = headers;
        this.message = message;
        this.body = (byte[])bodyBytes.clone();
    }
}
