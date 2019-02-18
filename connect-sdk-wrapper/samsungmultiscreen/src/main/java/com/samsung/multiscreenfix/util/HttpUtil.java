//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.util;

import android.net.Uri;
import com.mega.cast.utils.log.SmartLog;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.koushikdutta.async.http.body.JSONObjectBody;
import java.util.Map;
import org.json.JSONObject;

public class HttpUtil {
    public static final String METHOD_GET = "GET";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_DELETE = "DELETE";
    private static boolean logging = false;
    private static final String TAG = "HttpUtil";

    public HttpUtil() {
    }

    public static void enableLogging(boolean logging) {
        HttpUtil.logging = logging;
    }

    public static void executeJSONRequest(Uri uri, String method, StringCallback httpStringCallback) {
        executeJSONRequest(uri, method, (Map)null, httpStringCallback);
    }

    public static void executeJSONRequest(Uri uri, String method, int timeout, StringCallback httpStringCallback) {
        executeJSONRequest(uri, method, timeout, (Map)null, httpStringCallback);
    }

    public static void executeJSONRequest(Uri uri, String method, Map<String, Object> params, StringCallback httpStringCallback) {
        executeJSONRequest(uri, method, 30000, params, httpStringCallback);
    }

    public static void executeJSONRequest(Uri uri, String method, int timeout, Map<String, Object> params, StringCallback httpStringCallback) {
        AsyncHttpRequest request = new AsyncHttpRequest(uri, method);
        request.setTimeout(timeout <= 0?30000:timeout);
        request.setHeader("Content-Type", "application/json");
        if(params != null) {
            JSONObject jsonParams = new JSONObject(params);
            JSONObjectBody requestBody = new JSONObjectBody(jsonParams);
            request.setBody(requestBody);
        }

        if(logging) {
            SmartLog.d("HttpUtil", "executeJSONRequest() method: " + request.getMethod() + ", uri: " + uri);
            SmartLog.d("HttpUtil", "executeJSONRequest() request.getHeaders() " + request.getHeaders().toString());
        }

        AsyncHttpClient.getDefaultInstance().executeString(request, httpStringCallback);
    }

    public interface ResultCreator<T> {
        T createResult(Map<String, Object> var1);
    }
}
