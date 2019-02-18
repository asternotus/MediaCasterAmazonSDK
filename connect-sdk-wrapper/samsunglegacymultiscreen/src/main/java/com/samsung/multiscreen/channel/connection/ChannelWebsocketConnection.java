//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel.connection;

import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.ChannelError;
import com.samsung.multiscreen.channel.connection.ChannelConnection;
import com.samsung.multiscreen.channel.connection.IChannelConnectionListener;
import com.samsung.multiscreen.channel.connection.PKI;
import com.samsung.multiscreen.channel.info.ChannelInfo;
import com.samsung.multiscreen.net.json.JSONRPCMessage;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class ChannelWebsocketConnection extends ChannelConnection {
    public static final Logger LOG = Logger.getLogger(ChannelWebsocketConnection.class.getName());
    private static String TV_PUB_KEY;
    private Key tvPublicKey;
    private KeyPair keyPair;
    private String encryptedPublicKey = null;
    private ChannelWebsocketConnection.WSClient wsClient;
    private boolean isConnecting = false;
    private boolean isDisconnecting = false;

    public ChannelWebsocketConnection(Channel channel, ChannelInfo channelInfo, Map<String, String> attributes) {
        super(channel);
        this.initializePKI();
        if(this.encryptedPublicKey != null) {
            if(attributes == null) {
                attributes = new HashMap();
            }

            ((Map)attributes).put("__pem", this.encryptedPublicKey);
        }

        URI uri = this.createURI(channelInfo.getEndPoint(), (Map)attributes);
        this.wsClient = new ChannelWebsocketConnection.WSClient(uri);
    }

    private void initializePKI() {
        this.keyPair = PKI.generateKeyPair();
        String pemKey = PKI.keyAsPEM(this.keyPair.getPublic(), "PUBLIC KEY");
        this.tvPublicKey = PKI.pemAsPublicKey(TV_PUB_KEY);
        if(this.tvPublicKey != null) {
            this.encryptedPublicKey = PKI.encryptStringAsHex(pemKey, this.tvPublicKey);
        }

    }

    private URI createURI(String endPoint, Map<String, String> attributes) {
        try {
            URI e = URI.create(endPoint);
            StringBuilder query = new StringBuilder();
            if(attributes != null && attributes.size() > 0) {
                Iterator iter = attributes.entrySet().iterator();

                while(iter.hasNext()) {
                    Entry entry = (Entry)iter.next();
                    query.append(URLEncoder.encode((String)entry.getKey(), "UTF-8"));
                    query.append("=");
                    query.append(URLEncoder.encode((String)entry.getValue(), "UTF-8"));
                    if(iter.hasNext()) {
                        query.append("&");
                    }
                }
            }

            return new URI(e.getScheme(), e.getUserInfo(), e.getHost(), e.getPort(), e.getPath(), query.toString(), (String)null);
        } catch (UnsupportedEncodingException var7) {
            return null;
        } catch (URISyntaxException var8) {
            return null;
        }
    }

    public boolean isConnected() {
        return this.wsClient.getConnection().isOpen();
    }

    public void connect() {
        if(this.isConnected()) {
            LOG.info("connect() ALREADY CONNECTED");
            IChannelConnectionListener listener = super.getListener();
            if(listener != null) {
                listener.onConnectError(new ChannelError("Already Connected"));
            }
        } else {
            this.isConnecting = true;
            this.wsClient.connect();
        }

    }

    public void disconnect() {
        if(!this.isConnected()) {
            IChannelConnectionListener listener = super.getListener();
            if(listener != null) {
                listener.onDisconnectError(new ChannelError("Not Connected"));
            }
        }

        this.wsClient.close();
    }

    public void send(JSONRPCMessage message, boolean encryptMessage) {
        if(encryptMessage) {
            LOG.info("send() Encrypting outgoing message");
            String payload = (String)message.getParams().get(JSONRPCMessage.KEY_MESSAGE);
            payload = PKI.encryptStringAsHex(payload, this.tvPublicKey);
            message.getParams().put(JSONRPCMessage.KEY_MESSAGE, payload);
            message.getParams().put(JSONRPCMessage.KEY_ENCRYPTED, Boolean.valueOf(true));
        }

        LOG.warning("send() - Sending message: " + message.toJSONString());
        this.wsClient.send(message.toJSONString());
    }

    public void send(String message) {
        this.wsClient.send(message);
    }

    static {
        LOG.setLevel(Level.OFF);
        TV_PUB_KEY = "-----BEGIN PUBLIC KEY-----\r\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDfaldZKOKdkfvfYiFgX/ZRHdQw\r\nNrb8U8imZ9gNOBXtrDu/hGxHEgyrZ9iMqoIcIhgxBzcKwKBAp4xu6yB3AOZiBwLI\r\n73ajox/CpIzXE9yPevd5wQ+XHctIQazp0qrE9Py5Q5Ox7HB9rmKjSISKQ3A1JtEV\r\nbl0bI0iMf4QCtl/FdQIDAQAB\r\n-----END PUBLIC KEY-----\r\n";
    }

    private class WSClient extends WebSocketClient {
        public WSClient(URI serverURI) {
            super(serverURI);
        }

        public void onOpen(ServerHandshake serverHandshake) {
            ChannelWebsocketConnection.this.isConnecting = false;
            IChannelConnectionListener listener = ChannelWebsocketConnection.super.getListener();
            if(listener != null) {
                listener.onConnect();
            }

        }

        public void onMessage(String s) {
            JSONRPCMessage message = JSONRPCMessage.createWithJSONData(s);
            Boolean isEncrypted = (Boolean)message.getParams().get(JSONRPCMessage.KEY_ENCRYPTED);
            if(isEncrypted == Boolean.TRUE) {
                String listener = (String)message.getParams().get(JSONRPCMessage.KEY_MESSAGE);
                listener = PKI.decryptHexString(listener, ChannelWebsocketConnection.this.keyPair.getPrivate());
                message.getParams().put(JSONRPCMessage.KEY_MESSAGE, listener);
            }

            IChannelConnectionListener listener1 = ChannelWebsocketConnection.super.getListener();
            if(listener1 != null) {
                listener1.onMessage(message);
            }

        }

        public void onClose(int i, String s, boolean b) {
            ChannelWebsocketConnection.this.isDisconnecting = true;
            IChannelConnectionListener listener = ChannelWebsocketConnection.super.getListener();
            if(listener != null) {
                listener.onDisconnect();
            }

        }

        public void onCloseInitiated(int code, String reason) {
            ChannelWebsocketConnection.this.isDisconnecting = true;
        }

        public void onError(Exception e) {
            IChannelConnectionListener listener;
            if(ChannelWebsocketConnection.this.isConnecting) {
                ChannelWebsocketConnection.this.isConnecting = false;
                listener = ChannelWebsocketConnection.super.getListener();
                if(listener != null) {
                    listener.onConnectError(new ChannelError("Connection Failed"));
                }
            } else if(ChannelWebsocketConnection.this.isDisconnecting) {
                ChannelWebsocketConnection.this.isDisconnecting = false;
                listener = ChannelWebsocketConnection.super.getListener();
                if(listener != null) {
                    listener.onDisconnectError(new ChannelError("Error closing: " + e.getLocalizedMessage()));
                }
            }

        }
    }
}
