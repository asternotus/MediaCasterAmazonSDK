//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build;
import android.os.Build.VERSION;
import android.text.TextUtils;
import com.mega.cast.utils.log.SmartLog;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.samsung.multiscreenfix.util.HttpUtil;
import com.samsung.multiscreenfix.util.JSONUtil;
import com.samsung.multiscreenfix.util.RunUtil;
import com.samsung.multiscreenfix.util.HttpUtil.ResultCreator;

import java.util.HashMap;
import java.util.Map;

public class Application extends Channel {
    private static final String LOG_TAG = "SamsungMultiscreen";

    private static final String TAG = "Application";
    public static final String ROUTE_APPLICATION = "applications";
    public static final String ROUTE_WEBAPPLICATION = "webapplication";
    public static final String PROPERTY_VALUE_LIBRARY = "Android SDK";
    private OnConnectListener onConnectListener;
    private boolean isStopping = false;
    private Boolean isHostDisconnected = Boolean.valueOf(false);
    private final boolean webapp;
    private final Map<String, Object> startArgs;

    private Application(Service service, Uri uri, String id, Map<String, Object> startArgs) {
        super(service, uri, id);
        boolean webapp = false;
        if (!TextUtils.isEmpty(uri.getScheme())) {
            webapp = true;
        }

        if (id.equals("samsung.default.media.player")) {
            webapp = true;
        }

        this.webapp = webapp;
        this.startArgs = startArgs;
    }

    Map<String, Object> getStartArgs() {
        return this.startArgs;
    }

    public boolean isConnected() {
        return super.isConnected() && this.connected && !this.isHostDisconnected.booleanValue();
    }

    public void getInfo(Result<ApplicationInfo> result) {
        Builder builder = this.getService().getUri().buildUpon();
        if (this.webapp) {
            builder.appendPath("webapplication");
        } else {
            builder.appendPath("applications").appendPath(this.getUri().toString());
        }

        builder.appendPath("");
        Uri uri = builder.build();
        if (this.securityMode) {
            uri = super.getSecureURL(uri);
        }

        StringCallback httpStringCallback = HttpHelper.createHttpCallback(new ResultCreator<ApplicationInfo>() {
            public ApplicationInfo createResult(Map<String, Object> data) {
                return ApplicationInfo.create(data);
            }
        }, result);
        HttpUtil.executeJSONRequest(uri, "GET", httpStringCallback);
    }

    public void start(Result<Boolean> result) {
        Map params = this.getParams();
        params.put("os", VERSION.RELEASE);
        params.put("library", "Android SDK");
        params.put("version", "2.4.1");
        params.put("modelNumber", Build.MODEL);
        if (this.startArgs != null) {
            params.put("data", this.startArgs);
        }

        this.invokeMethod(this.webapp ? "ms.webapplication.start" : "ms.application.start", params, result);
    }

    public void stop(Result<Boolean> result) {
        Map params = this.getParams();
        this.invokeMethod(this.webapp ? "ms.webapplication.stop" : "ms.application.stop", params, result);
    }

    public void install(Result<Boolean> result) {
        if (this.webapp) {
            String builder = this.getUID();
            this.registerCallback(builder, result);
            this.handleError(builder, Error.create("Unsupported method"));
        } else {
            Builder builder1 = this.getService().getUri().buildUpon().appendPath("applications").appendPath(this.getUri().toString()).appendPath("");
            Uri uri = builder1.build();
            if (this.securityMode) {
                uri = super.getSecureURL(uri);
            }

            StringCallback httpStringCallback = HttpHelper.createHttpCallback(new ResultCreator<Boolean>() {
                public Boolean createResult(Map<String, Object> data) {
                    return Boolean.TRUE;
                }
            }, result);
            SmartLog.d(LOG_TAG, "executeJSONRequest " + uri);
            HttpUtil.executeJSONRequest(uri, "PUT", httpStringCallback);
        }

    }

    Map<String, Object> getParams() {
        Uri uri = this.getUri();
        String messageKey = "id";
        String id = uri.toString();
        if (this.webapp) {
            messageKey = "url";
        }

        HashMap params = new HashMap();
        params.put(messageKey, id);
        return params;
    }

    void connectToPlay(Map<String, String> attributes, Result<Client> result) {
        super.connect(attributes, result);
    }

    public void connect(Map<String, String> attributes, final Result<Client> result) {
        Result connectCallback = new Result<Client>() {
            public void onSuccess(final Client client) {
                Application.this.start(new Result<Boolean>() {
                    public void onSuccess(Boolean success) {
                        if (result != null) {
                            result.onSuccess(client);
                        }

                        Application.this.isHostDisconnected = Boolean.valueOf(false);
                    }

                    public void onError(Error error) {
                        Application.this.closeConnection();
                        if (result != null) {
                            result.onError(error);
                        }

                    }
                });
            }

            public void onError(Error error) {
                if (result != null) {
                    result.onError(error);
                }

            }
        };
        super.connect(attributes, connectCallback);
    }

    void closeConnection() {
        WebSocket websocket = this.getWebSocket();
        if (websocket != null && websocket.isOpen()) {
            websocket.setClosedCallback(new CompletedCallback() {
                public void onCompleted(Exception ex) {
                    Application.this.handleSocketClosed();
                }
            });
            websocket.close();
        }

    }

    public void disconnect() {
        this.disconnect(true, (Result) null);
    }

    public void disconnect(Result<Client> result) {
        this.disconnect(true, result);
    }

    public void disconnect(boolean stopOnDisconnect, final Result<Client> result) {
        if (stopOnDisconnect) {
            Clients clients = this.getClients();
            int numClients = clients.size();
            final Client me = clients.me();
            if (numClients == 2 && clients.getHost() != null && me != null || numClients == 1 && me != null || numClients == 0) {
                this.stop(new Result<Boolean>() {
                    public void onSuccess(Boolean success) {
                        Application.this.isStopping = false;
                        synchronized (Application.this.isHostDisconnected) {
                            if (Application.this.isHostDisconnected.booleanValue()) {
                                Application.this.realDisconnect(result);
                            } else if (result != null) {
                                result.onSuccess(me);
                            }

                        }
                    }

                    public void onError(Error error) {
                        Application.this.isStopping = false;
                        if (result != null) {
                            result.onError(error);
                        }

                    }
                });
                this.isStopping = true;
                return;
            }
        }

        this.realDisconnect(result);
    }

    private void realDisconnect(Result<Client> result) {
        super.disconnect(result);
    }

    void invokeMethod(String method, Map<String, Object> params, Result callback) {
        String messageId = this.getUID();
        this.registerCallback(messageId, callback);
        this.invokeMethod(method, params, messageId, callback);
    }

    private void invokeMethod(String method, Map<String, Object> params, String messageId, Result callback) {
        if (this.isDebug()) {
            SmartLog.d("Application", "method: " + method + ", params: " + params);
        }

        if (!super.isConnected()) {
            this.handleError(messageId, Error.create("Not connected"));
        } else {
            HashMap message = new HashMap();
            message.put("method", method);
            message.put("id", messageId);
            message.put("params", params);
            String json = JSONUtil.toJSONString(message);
            this.getWebSocket().send(json);
        }
    }

    protected void handleMessage(String callbackId, Map<String, Object> message, byte[] payload) {
        String event = (String) message.get("event");
        if (event != null) {
            super.handleMessage(callbackId, message, payload);
        } else {
            this.handleApplicationMessage(message);
        }

    }

    private void handleApplicationMessage(Map<String, Object> message) {
        if (this.isDebug()) {
            SmartLog.d("Application", "message: " + message.toString());
        }

        String messageId = (String) message.get("id");
        Map errorMap = (Map) message.get("error");

        try {
            Result result = this.getCallback(messageId);
            if (result != null) {
                this.doApplicationCallback(result, message);
            }
        } catch (Exception var5) {
            ;
        }

    }

    private void doApplicationCallback(final Result<Object> result, Map<String, Object> message) {
        final Object obj = message.get("result");
        final Map errorMap = (Map) message.get("error");
        RunUtil.runOnUI(new Runnable() {
            public void run() {
                if (errorMap != null) {
                    long info = -1L;

                    try {
                        Object codeObj = errorMap.get("code");
                        if (codeObj instanceof String) {
                            info = Long.parseLong((String) codeObj);
                        } else if (codeObj instanceof Integer) {
                            info = (long) ((Integer) codeObj).intValue();
                        } else if (codeObj instanceof Long) {
                            info = ((Long) codeObj).longValue();
                        }
                    } catch (Exception var5) {
                        ;
                    }

                    result.onError(Error.create(info, errorMap));
                } else {
                    ApplicationInfo info1 = null;
                    if (obj instanceof Map) {
                        try {
                            info1 = ApplicationInfo.create((Map) obj);
                            result.onSuccess(info1);
                        } catch (NullPointerException var4) {
                            result.onError(Error.create("Unexpected response: " + obj.toString()));
                        }
                    } else {
                        result.onSuccess(obj);
                    }
                }

            }
        });
    }

    protected void handleClientDisconnectMessage(Map<String, Object> message) {
        Map data = (Map) message.get("data");
        Client client = null;
        if (data != null) {
            String clientId = (String) data.get("id");
            Clients clients = this.getClients();
            client = clients.get(clientId);
        }

        super.handleClientDisconnectMessage(message);
        if (client != null && client.isHost()) {
            Boolean clientId1 = this.isHostDisconnected;
            synchronized (this.isHostDisconnected) {
                this.isHostDisconnected = Boolean.valueOf(true);
            }
        }

        if (!this.isStopping && client != null && client.isHost()) {
            this.realDisconnect((Result) null);
        }

    }

    protected void handleReadyMessage(Map<String, Object> message) {
        SmartLog.d(LOG_TAG, "handleReadyMessage ");
        if (this.onConnectListener != null) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    Application.this.onConnectListener.onConnect(Application.this.getClients().me());
                }
            });
        }

        final OnReadyListener onReadyListener = this.getOnReadyListener();
        if (onReadyListener != null) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    onReadyListener.onReady();
                }
            });
        }

    }

    static Application create(Service service, Uri uri) {
        if (service != null && uri != null) {
            String id = uri.toString();
            if (!TextUtils.isEmpty(uri.getScheme())) {
                id = uri.getHost();
            }

            Application application = new Application(service, uri, id, (Map) null);
            return application;
        } else {
            throw new NullPointerException();
        }
    }

    static Application create(Service service, Uri uri, String id, Map<String, Object> startArgs) {
        if (service != null && uri != null && id != null) {
            Application application = new Application(service, uri, id, startArgs);
            return application;
        } else {
            throw new NullPointerException();
        }
    }

    public String toString() {
        return "Application(super=" + super.toString() + ", onConnectListener=" + this.onConnectListener + ", isStopping=" + this.isStopping + ", isHostDisconnected=" + this.isHostDisconnected + ", webapp=" + this.isWebapp() + ", startArgs=" + this.getStartArgs() + ")";
    }

    public void setOnConnectListener(OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }

    public boolean isWebapp() {
        return this.webapp;
    }
}
