//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;

import com.mega.cast.utils.log.SmartLog;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;
import com.samsung.multiscreenfix.util.JSONUtil;
import com.samsung.multiscreenfix.util.RunUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.conn.ssl.SSLSocketFactory;

public class Channel {
    private static final String LOG_TAG = "SamsungMultiscreenChannel";

    private static final String TAG = "Channel";
    private static final String ROUTE = "channels";
    private static final String ERROR_EVENT = "ms.error";
    private static final String CONNECT_EVENT = "ms.channel.connect";
    private static final String DISCONNECT_EVENT = "ms.channel.disconnect";
    private static final String CLIENT_CONNECT_EVENT = "ms.channel.clientConnect";
    private static final String CLIENT_DISCONNECT_EVENT = "ms.channel.clientDisconnect";
    private static final String READY_EVENT = "ms.channel.ready";
    private static final String MULTICAST_PORT = "8001";
    private static final String SECURE_PORT = "8002";
    private static final String HTTP_PROTOCOL = "http:";
    private static final String HTTPS_PROTOCOL = "https:";
    private static final String TLS_PROTOCOL = "TLS";
    private Service service;
    private final Uri uri;
    private final String id;
    private Clients clients = new Clients(this);
    protected boolean connected;
    protected boolean securityMode = false;
    private volatile Channel.OnConnectListener onConnectListener;
    private volatile Channel.OnDisconnectListener onDisconnectListener;
    private volatile Channel.OnClientConnectListener onClientConnectListener;
    private volatile Channel.OnClientDisconnectListener onClientDisconnectListener;
    private Channel.OnReadyListener onReadyListener;
    private volatile Channel.OnErrorListener onErrorListener;
    private Map<String, List<Channel.OnMessageListener>> messageListeners = new ConcurrentHashMap();
    private Map<String, Result> callbacks = new ConcurrentHashMap();
    private static SecureRandom random = new SecureRandom();
    private WebSocket webSocket;
    private boolean disconnecting = false;
    private boolean debug = false;
    private final Channel.ChannelConnectionHandler connectionHandler = new Channel.ChannelConnectionHandler();
    private static final String CA_CRT = "-----BEGIN CERTIFICATE-----\nMIIDhjCCAm6gAwIBAgIJAPm7naJvG91yMA0GCSqGSIb3DQEBCwUAMFcxCzAJBgNV\nBAYTAktSMRUwEwYDVQQKEwxTbWFydFZpZXdTREsxMTAvBgNVBAMTKFNtYXJ0Vmll\nd1NESyBSb290IENlcml0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMTYwNzI5MDUzNDEw\nWhcNMzYwNzI5MDUzNDEwWjBXMQswCQYDVQQGEwJLUjEVMBMGA1UEChMMU21hcnRW\naWV3U0RLMTEwLwYDVQQDEyhTbWFydFZpZXdTREsgUm9vdCBDZXJpdGlmaWNhdGUg\nQXV0aG9yaXR5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArtBcclIW\nEuObUkeTn+FW3m6Lm/YpwAOeABCtq6RKnBcq6jzEo3I433cSuVC2DrWGiiYi62Qm\niAzOHEtkvRctj+jEuK7ZKneKkxQ5261os0RsvWG7fONVb4m0ZRBydykgfu/PLwUB\nMWeiF3PB6w7YCzN1MJzb9EISFlhEcqMxDHgwGWHZYo/CTWtIwBVZ07mhdrCQaV2r\nLLJInA+4Wh9nXRO82qRnqYqFZfV7psIOW4MqfjWqNcKAHWWZ1gKrdZc9fPb2YVK4\nOIlaT3Qq9DOCveeU5T8d3MGEoiFnXt4Lp5656nI7MbkAsPEFFRHFkBK3o8CE1HLp\nsELQa6GBRe8WPQIDAQABo1UwUzASBgNVHRMBAf8ECDAGAQH/AgEAMB0GA1UdDgQW\nBBRQyhCp74M+t2GwCiH3g3Aau0AX7DALBgNVHQ8EBAMCAQYwEQYJYIZIAYb4QgEB\nBAQDAgAHMA0GCSqGSIb3DQEBCwUAA4IBAQAVIEeJo4vGsKPZBoY19hCXZnqB6Qcm\nOnWZzAZ0am8OQHQ4/LbSJ+Vnxh7eFiLtPQwuSHJ1a95ODA7RlNgnpC8ymHsL5Wl5\nUKOq5jOs3Jfa0aG99H9TsFKBysXlsBHfaHX+8/AoZUJDOksNeQigj3n4wCdLEPvt\nUpI9qJEjuzXeKxVhwnDkc/AvOuSGUaPiSeCSxy+xpcyWCANc4uVXtOxJluQvy8aC\nm6l0yG3Ucg09yCIkPzKtzG/kAadDRrTOYi/x4ZECtdamHQxncEnb3D881veLc6+s\nztEvDx0F77vRtadpeBxNZKivG2kJrymuf47pGIS0FlC5+/5ieV54+1/d\n-----END CERTIFICATE-----";

    protected Channel(Service service, Uri uri, String id) {
        this.service = service;
        this.uri = uri;
        this.id = id;
    }

    public boolean isConnected() {
        return this.isWebSocketOpen();
    }

    private boolean isWebSocketOpen() {
        return this.webSocket != null && this.webSocket.isOpen();
    }

    protected Uri getChannelUri(Map<String, String> queryParams) {
        Builder builder = this.service.getUri().buildUpon().appendPath("channels").appendPath(this.id);
        if (queryParams != null) {
            Iterator var3 = queryParams.keySet().iterator();

            while (var3.hasNext()) {
                String key = (String) var3.next();
                builder.appendQueryParameter(key, (String) queryParams.get(key));
            }
        }

        return builder.build();
    }

    public void connect() {
        this.connect((Result) null);
    }

    public void connect(Result<Client> result) {
        this.connect((Map) null, result);
    }

    public void setSecurityMode(boolean securityMode, final Result<Boolean> result) {
        if (securityMode) {
            this.service.isSecurityModeSupported(new Result<Boolean>() {
                public void onSuccess(Boolean isSupported) {
                    Channel.this.securityMode = isSupported.booleanValue();
                    result.onSuccess(isSupported);
                }

                public void onError(Error error) {
                    Channel.this.securityMode = false;
                    SmartLog.e("Channel", "set security mode true onError: " + error.getMessage());
                    result.onError(error);
                }
            });
        } else {
            this.securityMode = false;
            result.onSuccess(Boolean.valueOf(true));
        }

    }

    Uri getSecureURL(Uri uri) {
        String httpsUri = uri.toString().replace("http:", "https:").replace("8001", "8002");
        AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return Uri.parse(httpsUri);
    }

    private Uri getWebSocketConnectionURL(Uri uri) {
        if (!this.securityMode) {
            if (AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().getSSLContext() != null) {
                AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().setSSLContext((SSLContext) null);
                AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().setTrustManagers((TrustManager[]) null);
                AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().setHostnameVerifier((HostnameVerifier) null);
            }
        } else {
            uri = this.getSecureURL(uri);
            Certificate testCA = null;

            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
                testCA = certificateFactory.generateCertificate(new ByteArrayInputStream("-----BEGIN CERTIFICATE-----\nMIIDhjCCAm6gAwIBAgIJAPm7naJvG91yMA0GCSqGSIb3DQEBCwUAMFcxCzAJBgNV\nBAYTAktSMRUwEwYDVQQKEwxTbWFydFZpZXdTREsxMTAvBgNVBAMTKFNtYXJ0Vmll\nd1NESyBSb290IENlcml0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMTYwNzI5MDUzNDEw\nWhcNMzYwNzI5MDUzNDEwWjBXMQswCQYDVQQGEwJLUjEVMBMGA1UEChMMU21hcnRW\naWV3U0RLMTEwLwYDVQQDEyhTbWFydFZpZXdTREsgUm9vdCBDZXJpdGlmaWNhdGUg\nQXV0aG9yaXR5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArtBcclIW\nEuObUkeTn+FW3m6Lm/YpwAOeABCtq6RKnBcq6jzEo3I433cSuVC2DrWGiiYi62Qm\niAzOHEtkvRctj+jEuK7ZKneKkxQ5261os0RsvWG7fONVb4m0ZRBydykgfu/PLwUB\nMWeiF3PB6w7YCzN1MJzb9EISFlhEcqMxDHgwGWHZYo/CTWtIwBVZ07mhdrCQaV2r\nLLJInA+4Wh9nXRO82qRnqYqFZfV7psIOW4MqfjWqNcKAHWWZ1gKrdZc9fPb2YVK4\nOIlaT3Qq9DOCveeU5T8d3MGEoiFnXt4Lp5656nI7MbkAsPEFFRHFkBK3o8CE1HLp\nsELQa6GBRe8WPQIDAQABo1UwUzASBgNVHRMBAf8ECDAGAQH/AgEAMB0GA1UdDgQW\nBBRQyhCp74M+t2GwCiH3g3Aau0AX7DALBgNVHQ8EBAMCAQYwEQYJYIZIAYb4QgEB\nBAQDAgAHMA0GCSqGSIb3DQEBCwUAA4IBAQAVIEeJo4vGsKPZBoY19hCXZnqB6Qcm\nOnWZzAZ0am8OQHQ4/LbSJ+Vnxh7eFiLtPQwuSHJ1a95ODA7RlNgnpC8ymHsL5Wl5\nUKOq5jOs3Jfa0aG99H9TsFKBysXlsBHfaHX+8/AoZUJDOksNeQigj3n4wCdLEPvt\nUpI9qJEjuzXeKxVhwnDkc/AvOuSGUaPiSeCSxy+xpcyWCANc4uVXtOxJluQvy8aC\nm6l0yG3Ucg09yCIkPzKtzG/kAadDRrTOYi/x4ZECtdamHQxncEnb3D881veLc6+s\nztEvDx0F77vRtadpeBxNZKivG2kJrymuf47pGIS0FlC5+/5ieV54+1/d\n-----END CERTIFICATE-----".getBytes()));
            } catch (CertificateException var18) {
                var18.printStackTrace();
            }

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = null;

            try {
                keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load((InputStream) null, (char[]) null);
                keyStore.setCertificateEntry("ca", testCA);
            } catch (KeyStoreException var14) {
                var14.printStackTrace();
            } catch (CertificateException var15) {
                var15.printStackTrace();
            } catch (NoSuchAlgorithmException var16) {
                var16.printStackTrace();
            } catch (IOException var17) {
                var17.printStackTrace();
            }

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = null;

            try {
                tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);
            } catch (NoSuchAlgorithmException var12) {
                var12.printStackTrace();
            } catch (KeyStoreException var13) {
                var13.printStackTrace();
            }

            if (tmf != null) {
                SSLContext sslContext = null;

                try {
                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init((KeyManager[]) null, tmf.getTrustManagers(), (SecureRandom) null);
                } catch (NoSuchAlgorithmException var10) {
                    var10.printStackTrace();
                } catch (KeyManagementException var11) {
                    var11.printStackTrace();
                }

                AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().setSSLContext(sslContext);
                AsyncHttpClient.getDefaultInstance().getSSLSocketMiddleware().setTrustManagers(tmf.getTrustManagers());
            }
        }

        return uri;
    }

    public void connect(final Map<String, String> attributes, final Result<Client> result) {
        if (this.service.isStandbyService.booleanValue()) {
            if (StandbyDeviceList.getInstance() == null) {
                return;
            }

            String mac = StandbyDeviceList.getInstance().getMac(this.service);
            if (mac == null) {
                return;
            }

            Service.WakeOnWirelessAndConnect(mac, this.service.getUri(), new Result<Service>() {
                public void onSuccess(final Service newService) {
                    Channel.this.connect(Channel.this.getWebSocketConnectionURL(Channel.this.getChannelUri(attributes)), attributes, new Result<Client>() {
                        public void onSuccess(Client client) {
                            Channel.this.service = newService;
                            result.onSuccess(client);
                        }

                        public void onError(Error err) {
                            ErrorCode error = new ErrorCode("ERROR_CONNECT_FAILED");
                            result.onError(Error.create((long) error.value(), error.name(), err.toString()));
                        }
                    });
                }

                public void onError(Error error) {
                    Service.getByURI(StandbyDeviceList.getInstance().get(Channel.this.service.getId()).getUri(), new Result<Service>() {
                        public void onSuccess(Service newService) {
                            Channel.this.service = newService;
                            Channel.this.connect(Channel.this.getWebSocketConnectionURL(Channel.this.getChannelUri(attributes)), attributes, new Result<Client>() {
                                public void onSuccess(Client client) {
                                    result.onSuccess(client);
                                }

                                public void onError(Error err) {
                                    ErrorCode error = new ErrorCode("ERROR_CONNECT_FAILED");
                                    result.onError(Error.create((long) error.value(), error.name(), err.toString()));
                                }
                            });
                        }

                        public void onError(Error err) {
                            ErrorCode error = new ErrorCode("ERROR_HOST_UNREACHABLE");
                            result.onError(Error.create((long) error.value(), error.name(), err.toString()));
                        }
                    });
                }
            });
        } else {
            if (StandbyDeviceList.getInstance() != null) {
                StandbyDeviceList.getInstance().update(this.service, Boolean.valueOf(false));
            }

            this.connect(this.getWebSocketConnectionURL(this.getChannelUri(attributes)), attributes, result);
        }

    }

    public void connect(Uri url, Map<String, String> attributes, final Result<Client> result) {
        SmartLog.d(LOG_TAG, "connect " + url);
        final String id = this.getUID();
        this.registerCallback(id, result);
        if (this.isWebSocketOpen()) {
            ErrorCode error = new ErrorCode("ERROR_ALREADY_CONNECTED");
            this.handleError(id, Error.create((long) error.value(), error.name(), "Already Connected"));
        } else {
            AsyncHttpClient.getDefaultInstance().websocket(url.toString(), (String) null, new WebSocketConnectCallback() {
                public void onCompleted(Exception exception, WebSocket socket) {
                    if (Channel.this.isDebug()) {
                        if (exception != null) {
                            exception.printStackTrace();
                            SmartLog.d(LOG_TAG, "Socket connection exception: " + exception.getMessage());
                        }
                        SmartLog.d(LOG_TAG, "Connect completed socket " + socket);
                    }

                    if (socket == null) {
                        ErrorCode error = new ErrorCode("ERROR_CONNECT_FAILED");
                        Channel.this.handleError(id, Error.create((long) error.value(), error.name(), "Connect failed"));
                    } else {
                        Channel.this.webSocket = socket;
                        if (exception != null && result != null) {
                            Channel.this.handleError(id, Error.create(exception));
                        } else {
                            socket.setClosedCallback(new CompletedCallback() {
                                public void onCompleted(Exception ex) {
                                    Channel.this.handleSocketClosedAndNotify();
                                }
                            });
                            socket.setStringCallback(new StringCallback() {
                                public void onStringAvailable(String s) {
                                    SmartLog.d(LOG_TAG, "setStringCallback " + s);
                                    Channel.this.connectionHandler.resetLastPingReceived();

                                    try {
                                        Map e = JSONUtil.parse(s);
                                        String event = (String) e.get("event");
                                        if ("ms.channel.connect".equals(event)) {
                                            Channel.this.handleConnectMessage(e, id);
                                        } else {
                                            Channel.this.handleMessage(id, e, (byte[]) null);
                                        }
                                    } catch (Exception var4) {
                                        SmartLog.e("Channel", "connect error: " + var4.getMessage());
                                    }

                                }
                            });
                            socket.setDataCallback(new DataCallback() {
                                public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
                                    Channel.this.connectionHandler.resetLastPingReceived();
                                    Channel.this.handleBinaryMessage(dataEmitter, byteBufferList);
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    public void disconnect() {
        this.disconnect((Result) null);
    }

    public void disconnect(Result<Client> result) {
        String id = this.getUID();
        this.registerCallback(id, result);
        String message = null;
        if (!this.isWebSocketOpen()) {
            message = "Already Disconnected";
        }

        if (this.disconnecting) {
            message = "Already Disconnecting";
        }

        if (message != null) {
            this.handleError(id, Error.create(message));
        } else {
            this.disconnecting = true;
            this.webSocket.close();
            this.webSocket = null;
            this.getCallback(id);
            if (result != null) {
                result.onSuccess(this.clients.me());
            }
        }

    }

    private void handleSocketClosedAndNotify() {
        final Client client = this.clients.me();
        this.handleSocketClosed();
        if (this.onDisconnectListener != null) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    if (Channel.this.onDisconnectListener != null) {
                        Channel.this.onDisconnectListener.onDisconnect(client);
                    }

                }
            });
        }

    }

    protected void handleSocketClosed() {
        this.connectionHandler.stopPing();
        this.webSocket = null;
        this.connected = false;
        this.clients.reset();
        if (this.disconnecting) {
            this.disconnecting = false;
        }

    }

    public void publish(String event, Object data) {
        this.publishMessage(event, data, "host", (byte[]) null);
    }

    public void publish(String event, Object data, byte[] payload) {
        this.publishMessage(event, data, "host", payload);
    }

    public void publish(String event, Object data, String target) {
        this.publishMessage(event, data, target, (byte[]) null);
    }

    public void publish(String event, Object data, String target, byte[] payload) {
        this.publishMessage(event, data, target, payload);
    }

    public void publish(String event, Object data, Client client) {
        this.publishMessage(event, data, client.getId(), (byte[]) null);
    }

    public void publish(String event, Object data, Client client, byte[] payload) {
        this.publishMessage(event, data, client.getId(), payload);
    }

    public void publish(String event, Object data, List<Client> clients) {
        this.publish(event, data, (List) clients, (byte[]) null);
    }

    public void publish(String event, Object data, List<Client> clients, byte[] payload) {
        ArrayList ids = new ArrayList();
        Iterator var6 = clients.iterator();

        while (var6.hasNext()) {
            Client client = (Client) var6.next();
            ids.add(client.getId());
        }

        this.publishMessage(event, data, ids, payload);
    }

    private void publishMessage(String event, Object data, Object to, byte[] payload) {
        this.publishMessage("ms.channel.emit", event, data, to, payload);
    }

    private void publishMessage(String method, String event, Object data, Object to, byte[] payload) {
        if (this.isDebug()) {
            SmartLog.d(LOG_TAG, "method: " + method + ", event: " + event + ", data: " + data + ", to: " + to + ", payload size: " + (payload != null ? payload.length : 0));
        }

        if (!this.isWebSocketOpen()) {
            if (this.isDebug()) {
                SmartLog.d(LOG_TAG, "Not Connected");
            }

            ErrorCode params1 = new ErrorCode("ERROR_WEBSOCKET_DISCONNECTED");
            this.handleError((String) null, Error.create((long) params1.value(), params1.name(), "Not Connected"));
        } else {
            HashMap params = new HashMap();
            if (!TextUtils.isEmpty(event)) {
                params.put("event", event);
            }

            if (data != null) {
                params.put("data", data);
            }

            if (to != null) {
                params.put("to", to);
            }

            HashMap message = new HashMap();
            message.put("method", method);
            message.put("params", params);
            String json = JSONUtil.toJSONString(message);
            if (payload != null) {
                this.webSocket.send(this.createBinaryMessage(json, payload));
            } else {
                this.webSocket.send(json);
            }

        }
    }

    static Channel create(Service service, Uri uri) {
        if (service != null && uri != null) {
            return new Channel(service, uri, uri.toString());
        } else {
            throw new NullPointerException();
        }
    }

    private byte[] createBinaryMessage(String json, byte[] payload) {
        int headerLength = json.getBytes().length;
        ByteBuffer buffer = ByteBuffer.allocate(2 + headerLength + payload.length);
        buffer.putShort((short) headerLength);
        buffer.put(json.getBytes());
        buffer.put(payload);
        return buffer.array();
    }

    private void handleBinaryMessage(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
        short headerLength = byteBufferList.getShort();
        ByteBufferList headerBufferList = byteBufferList.get(headerLength);
        String json = headerBufferList.readString();
        byte[] payload = new byte[byteBufferList.remaining()];
        byteBufferList.get(payload);

        try {
            Map e = JSONUtil.parse(json);
            this.handleMessage(e, payload);
        } catch (Exception var8) {
            SmartLog.e("Channel", "handleBinaryMessage error: " + var8.getMessage());
        }

    }

    private void handleMessage(Map<String, Object> message, byte[] payload) {
        this.handleMessage((String) null, message, payload);
    }

    protected void handleMessage(String callbackId, Map<String, Object> message, byte[] payload) {
        String event = (String) message.get("event");
        if (this.isDebug()) {
            SmartLog.d(LOG_TAG, "event: " + event + ", message: " + message.toString() + ", payload size: " + (payload != null ? payload.length : 0));
        }

        if (event != null) {
            if ("ms.error".equalsIgnoreCase(event)) {
                this.handleErrorMessage(callbackId, message);
            } else if ("ms.channel.clientConnect".equalsIgnoreCase(event)) {
                this.handleClientConnectMessage(message);
            } else if ("ms.channel.clientDisconnect".equalsIgnoreCase(event)) {
                this.handleClientDisconnectMessage(message);
            } else if ("ms.channel.ready".equalsIgnoreCase(event)) {
                this.handleReadyMessage(message);
            } else if ("ms.channel.disconnect".equalsIgnoreCase(event)) {
                this.disconnect();
            } else {
                this.handleClientMessage(message, payload);
            }
        }

    }

    private void handleErrorMessage(String callbackId, Map<String, Object> message) {
        Map data = (Map) message.get("data");
        String errorMessage = (String) data.get("message");
        this.handleError(callbackId, Error.create(errorMessage));
    }

    protected void handleError(String callbackId, final Error error) {
        final Result result = this.getCallback(callbackId);
        RunUtil.runOnUI(new Runnable() {
            public void run() {
                if (result != null) {
                    result.onError(error);
                }

            }
        });
        if (this.onErrorListener != null) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    if (Channel.this.onErrorListener != null) {
                        Channel.this.onErrorListener.onError(error);
                    }

                }
            });
        }

    }

    private void handleConnectMessage(Map<String, Object> message, String callbackId) {
        Map data = (Map) message.get("data");
        String myClientId = (String) data.get("id");
        ArrayList clientList = new ArrayList();
        List mapClients = (List) data.get("clients");

        Client client;
        for (Iterator var7 = mapClients.iterator(); var7.hasNext(); this.connected = this.connected || client.isHost()) {
            Map mapClient = (Map) var7.next();
            client = Client.create(this, mapClient);
            clientList.add(client);
        }

        this.clients.reset();
        this.clients.add(clientList);
        this.clients.setMyClientId(myClientId);
        if (this.isWebSocketOpen()) {
            this.connectionHandler.startPing();
        }

        this.handleConnect(callbackId);
    }

    private void handleConnect(String callbackId) {
        SmartLog.d(LOG_TAG, "handleConnect " + callbackId);
        final Result result = this.getCallback(callbackId);
        RunUtil.runOnUI(new Runnable() {
            public void run() {
                if (result != null) {
                    result.onSuccess(Channel.this.clients.me());
                }

            }
        });
        if (this.onConnectListener != null) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    if (Channel.this.onConnectListener != null) {
                        Channel.this.onConnectListener.onConnect(Channel.this.clients.me());
                    }

                }
            });
        }

    }

    protected void handleReadyMessage(Map<String, Object> message) {
    }

    private void handleClientConnectMessage(Map<String, Object> message) {
        Map data = (Map) message.get("data");
        final Client client = Client.create(this, data);
        this.connected = true;
        this.clients.add(client);
        if (this.onClientConnectListener != null) {
            RunUtil.runOnUI(new Runnable() {
                public void run() {
                    if (Channel.this.onClientConnectListener != null) {
                        Channel.this.onClientConnectListener.onClientConnect(client);
                    }

                }
            });
        }

    }

    protected void handleClientDisconnectMessage(Map<String, Object> message) {
        Map data = (Map) message.get("data");
        if (data != null) {
            String clientId = (String) data.get("id");
            final Client client = this.clients.get(clientId);
            if (client == null) {
                return;
            }

            if (client.isHost()) {
                this.connected = false;
            }

            this.clients.remove(client);
            if (this.onClientDisconnectListener != null) {
                RunUtil.runOnUI(new Runnable() {
                    public void run() {
                        if (Channel.this.onClientDisconnectListener != null) {
                            Channel.this.onClientDisconnectListener.onClientDisconnect(client);
                        }

                    }
                });
            }
        }

    }

    protected void handleClientMessage(Map<String, Object> message, byte[] payload) {
        String event = (String) message.get("event");
        Object data = message.get("data");
        String fromId = (String) message.get("from");
        Client from = this.clients.get(fromId);
        Message msg = new Message(this, event, data, from, payload);
        this.emit(msg);
    }

    public void addOnMessageListener(String event, Channel.OnMessageListener onMessageListener) {
        if (event != null && onMessageListener != null) {
            List<Channel.OnMessageListener> onMessageListeners = this.messageListeners.get(event);
            if (onMessageListeners == null) {
                onMessageListeners = new CopyOnWriteArrayList();
                this.messageListeners.put(event, onMessageListeners);
            }

            ((List) onMessageListeners).add(onMessageListener);
        } else {
            throw new NullPointerException();
        }
    }

    public void removeOnMessageListeners(String event) {
        if (event == null) {
            throw new NullPointerException();
        } else {
            List onMessageListeners = (List) this.messageListeners.get(event);
            onMessageListeners.clear();
        }
    }

    public void removeOnMessageListener(String event, Channel.OnMessageListener onMessageListener) {
        if (event != null && onMessageListener != null) {
            List onMessageListeners = (List) this.messageListeners.get(event);
            if (onMessageListeners != null) {
                onMessageListeners.remove(onMessageListener);
            }

        } else {
            throw new NullPointerException();
        }
    }

    public void removeOnMessageListeners() {
        this.messageListeners.clear();
    }

    public void removeAllListeners() {
        this.setOnConnectListener((Channel.OnConnectListener) null);
        this.setOnDisconnectListener((Channel.OnDisconnectListener) null);
        this.setOnClientConnectListener((Channel.OnClientConnectListener) null);
        this.setOnClientDisconnectListener((Channel.OnClientDisconnectListener) null);
        this.setOnReadyListener((Channel.OnReadyListener) null);
        this.setOnErrorListener((Channel.OnErrorListener) null);
        this.removeOnMessageListeners();
    }

    public void setConnectionTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        } else {
            if (timeout == 0) {
                this.connectionHandler.stopPing();
            } else {
                this.connectionHandler.setPingTimeout(timeout);
                if (this.isWebSocketOpen()) {
                    this.connectionHandler.startPing();
                }
            }

        }
    }

    private void emit(final Message message) {
        if (message == null) {
            throw new NullPointerException();
        } else {
            List onMessageListeners = (List) this.messageListeners.get(message.getEvent());
            if (onMessageListeners != null) {
                Iterator var3 = onMessageListeners.iterator();

                while (var3.hasNext()) {
                    final Channel.OnMessageListener listener = (Channel.OnMessageListener) var3.next();
                    RunUtil.runOnUiDelayed(new Runnable() {
                        public void run() {
                            listener.onMessage(message);
                        }
                    }, 5L);
                }
            }

        }
    }

    protected void registerCallback(String uid, Result callback) {
        if (uid != null && callback != null) {
            this.callbacks.put(uid, callback);
        }

    }

    protected Result getCallback(String uid) {
        return uid != null ? (Result) this.callbacks.remove(uid) : null;
    }

    protected String getUID() {
        return String.valueOf(random.nextInt(2147483647));
    }

    public String toString() {
        return "Channel(service=" + this.service + ", uri=" + this.uri + ", id=" + this.id + ", clients=" + this.clients + ", connected=" + this.connected + ", securityMode=" + this.securityMode + ", onConnectListener=" + this.onConnectListener + ", onDisconnectListener=" + this.onDisconnectListener + ", onClientConnectListener=" + this.onClientConnectListener + ", onClientDisconnectListener=" + this.onClientDisconnectListener + ", onReadyListener=" + this.onReadyListener + ", onErrorListener=" + this.onErrorListener + ")";
    }

    protected Service getService() {
        return this.service;
    }

    public Uri getUri() {
        return this.uri;
    }

    public String getId() {
        return this.id;
    }

    public Clients getClients() {
        return this.clients;
    }

    public boolean isSecurityMode() {
        return this.securityMode;
    }

    public void setOnConnectListener(Channel.OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }

    public void setOnDisconnectListener(Channel.OnDisconnectListener onDisconnectListener) {
        this.onDisconnectListener = onDisconnectListener;
    }

    public void setOnClientConnectListener(Channel.OnClientConnectListener onClientConnectListener) {
        this.onClientConnectListener = onClientConnectListener;
    }

    public void setOnClientDisconnectListener(Channel.OnClientDisconnectListener onClientDisconnectListener) {
        this.onClientDisconnectListener = onClientDisconnectListener;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setOnReadyListener(Channel.OnReadyListener onReadyListener) {
        this.onReadyListener = onReadyListener;
    }

    protected Channel.OnReadyListener getOnReadyListener() {
        return this.onReadyListener;
    }

    public void setOnErrorListener(Channel.OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    protected WebSocket getWebSocket() {
        return this.webSocket;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return this.debug;
    }

    private class ChannelConnectionHandler {
        static final String PING = "channel.ping";
        private static final String PONG = "pong";
        static final String LIBVERSION_CHECK = "msfVersion2";
        private int pingTimeout = 15000;
        private long lastPingReceived = 0L;
        private ScheduledExecutorService executor = null;
        private final Runnable pingCheckRunnable = new Runnable() {
            public void run() {
                ChannelConnectionHandler.this.pingCheck();
            }
        };
        private int numPings;
        private long startTime;
        private long pingSent;
        private double average;
        private long longestRT;
        private boolean running = false;

        private void pingCheck() {
            long now = (new Date()).getTime();
            if (now > this.lastPingReceived + (long) this.pingTimeout) {
                SmartLog.w("Channel", "Ping not received in " + this.pingTimeout + " ms");
                Channel.this.webSocket.close();
            } else {
                Channel.this.publish("channel.ping", "pong", (Client) Channel.this.clients.me());
                this.pingSent = (new Date()).getTime();
            }

        }

        void resetLastPingReceived() {
            this.lastPingReceived = (new Date()).getTime();
        }

        void calculateAverageRT() {
            long lastRT = this.lastPingReceived - this.pingSent;
            if (lastRT > this.longestRT) {
                this.longestRT = lastRT;
            }

            this.average = ((double) (this.numPings++) * this.average + (double) lastRT) / (double) this.numPings;
        }

        void stopPing() {
            if (this.executor != null) {
                this.executor.shutdown();
                this.executor = null;
            }

            this.running = false;
        }

        void startPing() {
            short ping_timeout = 5000;
            if (!this.running) {
                this.stopPing();
                this.running = true;
                this.numPings = 0;
                this.average = 0.0D;
                this.longestRT = 0L;
                Channel.this.publish("msfVersion2", "msfVersion2", (Client) Channel.this.clients.me());
                Channel.this.publish("channel.ping", "pong", (Client) Channel.this.clients.me());
                this.startTime = (new Date()).getTime();
                this.pingSent = this.startTime;
                this.executor = Executors.newSingleThreadScheduledExecutor();
                this.executor.scheduleAtFixedRate(this.pingCheckRunnable, (long) ping_timeout, (long) ping_timeout, TimeUnit.MILLISECONDS);
            }
        }

        public ChannelConnectionHandler() {
        }

        public int getPingTimeout() {
            return this.pingTimeout;
        }

        public void setPingTimeout(int pingTimeout) {
            this.pingTimeout = pingTimeout;
        }

        public boolean isRunning() {
            return this.running;
        }
    }

    public interface OnErrorListener {
        void onError(Error var1);
    }

    public interface OnMessageListener {
        void onMessage(Message var1);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public interface OnReadyListener {
        void onReady();
    }

    public interface OnClientDisconnectListener {
        void onClientDisconnect(Client var1);
    }

    public interface OnClientConnectListener {
        void onClientConnect(Client var1);
    }

    public interface OnDisconnectListener {
        void onDisconnect(Client var1);
    }

    public interface OnConnectListener {
        void onConnect(Client var1);
    }
}
