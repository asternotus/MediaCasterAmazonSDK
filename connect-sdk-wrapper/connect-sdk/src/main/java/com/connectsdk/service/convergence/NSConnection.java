package com.connectsdk.service.convergence;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import com.mega.cast.utils.log.SmartLog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>
 * This class is the main process for library for android. NSConnection maintain the
 * discovery, connection, and communication protocol with N-Service capable devices. Since the N-Service connection should
 * be only one instance in an android application, This class instance should be implemented in a <i>Singleton</i> pattern
 * or inside the android {@link Application} context.
 * </p>
 * <p>
 * NSConnection class instance will notify all registered {@link NSListener} instance when any N-Service events occurred such as
 * device discovery found, connected/disconnected events, and messaging events. {@link NSListener} instance need to implements
 * abstract/unimplemented methods and registering itself to the NSConnection instance to be able to receive notification
 * from NSConnection.
 * </p>
 *
 * @author I Made Krisna Widhiastra (im.krisna@gmail.com)
 * @version 1.0.1
 */
public class NSConnection {

    public static final String LOG_TAG = "NSConnection";
    public static final int MAX_LOST_CONN = 10;

    public static final int CONN_EXCEPTION = -1;
    public static final int CONN_SUCCESS = 200;
    public static final int CONN_FAILED_NOT_RUNNING = 404;
    public static final int CONN_FAILED_CONN_EXIST = 409;
    public static final int CONN_FAILED_TIMEOUT = 408;

    public static final int NSERVICE_CONNECT = 1000;
    public static final int NSERVICE_DISCONNECT = 1001;
    public static final int NSERVICE_MESSAGE_SEND = 2000;
    public static final int NSERVICE_MESSAGE_POLLER = 2001;

    private Context context;
    private WifiManager wifiManager;
    private NSDiscovery discovery;

    private CopyOnWriteArrayList<NSListener> listener;
    private List<NSDevice> devices;

    private String appId;
    private String tvAddress;
    private String tvName;

    private boolean connected;
    private int lostCount;

    /**
     * <p><b><i>public NSConnection(String applicationId, Context context)</i></b></p>
     * <p>
     * Constructor for NSConnection class. Since the N-Service connection should
     * be only one instance in an android application, This class instance should be implemented in a
     * <i>Singleton</i> pattern or inside the android {@link Application} context.
     * </p>
     * <pre>
     * {@code
     * NSConnection connection = new NSConnection("1234567890", getApplicationContext());
     * NSConnection connection = new NSConnection("NServiceTest", getApplicationContext());
     * }
     * </pre>
     *
     * @param applicationId - Smart Application Project Name (development) or Samsung Seller Apps ID (operating)
     * @param context       - android {@link Context}. Suggested to use application context when using single instance pattern
     * @since 1.0.0
     */
    public NSConnection(String applicationId, Context context) {
        this.context = context;
        this.appId = applicationId;
        this.connected = false;
        this.lostCount = 0;

        this.listener = new CopyOnWriteArrayList<NSListener>();
        this.devices = new ArrayList<NSDevice>();

        this.discovery = new NSDiscovery(this);
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * <p><b><i>public void searchDevices()</i></b></p>
     * <p>
     * Commanding the NSConnection instance to perform UPnP SSDP device search for N-Service capable devices. This method
     * require {@link NSDiscovery} class and {@link org.cybergarage.upnp} library.
     * </p>
     * <p>
     * The search process will be asynchronous and when the N-Serivce capable device found, NSConnection will notify
     * all {@link NSListener} on the  <i>onDeviceChanged</i> method.
     * </p>
     *
     * @since 1.0.0
     */
    public void searchDevices() {
        if (!this.isWifiConnected()) {
            SmartLog.w(LOG_TAG, "Aborting Search: Wireless Network Not Connected");
            return;
        }

        Thread explorer = new Thread(new NSRunnable(this) {

            @Override
            public void run() {
                long randomId = Math.round(Math.random() * 1000);
                try {
                    SmartLog.d(LOG_TAG, "Explorer #" + String.valueOf(randomId) + " Started!");

                    this.executor.clearDevices();
                    this.executor.discovery.search();
                } catch (Exception e) {
                    SmartLog.e(LOG_TAG, "Explorer #" + String.valueOf(randomId) + " Error: " + e.getMessage());
                }
            }
        });
        explorer.setPriority(Thread.MIN_PRIORITY);
        explorer.start();
    }

    /**
     * <p><b><i>public void startPoller()</i></b></p>
     * <p>
     * This method will initiate connection polling with paired {@link NSDevice}. Connection polling will maintain
     * connectivity between android and N-Service device so then the connection will kept alive.
     * </p>
     * <p>
     * This method should not be called by external instance to prevent current connection crashed. If it is needed
     * to be called by external instance, the {@link NSConnection} instance must be not connected to any device and
     * the poller process already finished.
     * </p>
     *
     * @since 1.0.0
     */
    public void startPoller() {
        Thread poller = new Thread(new NSRunnable(this) {

            @Override
            public void run() {
                String requestUrl = NSConnection.buildRequestUrl(this.executor, NSConnection.NSERVICE_MESSAGE_POLLER);
                HttpClient client = NSConnection.createHttpClient();
                HttpGet request = new HttpGet(requestUrl);
                request.setHeader("SLDeviceID", this.executor.getAndroidDeviceId());

                while (this.executor.isConnected()) {
                    try {
                        HttpResponse response = client.execute(request);
                        int responseCode = response.getStatusLine().getStatusCode();
                        String responsePhrase = response.getStatusLine().getReasonPhrase();

                        SmartLog.d(NSConnection.LOG_TAG + ".connect", "Poller From: " + this.executor.getTvAddress());
                        SmartLog.d(NSConnection.LOG_TAG + ".connect", "-- Poller URL: " + request.getURI().toString());
                        SmartLog.d(NSConnection.LOG_TAG + ".connect", "-- Response: " + responsePhrase + " (" + String.valueOf(responseCode) + ")");

                        switch (responseCode) {
                            case 200:
                                final String message = EntityUtils.toString(response.getEntity());
                                SmartLog.d(LOG_TAG, "responseString " + message);

                                this.executor.setLostCount(0);
                                this.executor.notifyMessageReceived(message);
                                break;
                            case 408:
                                this.executor.setLostCount(0);
                                break;
                            default:
                                this.executor.setLostCount(this.executor.getLostCount() + 1);
                                if (this.executor.getLostCount() > NSConnection.MAX_LOST_CONN) {
                                    this.executor.stopPoller();
                                    this.executor.notifyDisconnected();
                                }
                                break;
                        }
                        response.getEntity().consumeContent();
                    } catch (ClientProtocolException e) {
                        SmartLog.e(NSConnection.LOG_TAG + ".poller", "ClientProtocolException: " + e.getMessage());
                    } catch (IOException e) {
                        SmartLog.e(NSConnection.LOG_TAG + ".poller", "IOException: " + e.getMessage());
                    }
                }
            }
        });
        poller.setPriority(Thread.MIN_PRIORITY);
        poller.start();
    }

    /**
     * <p><b><i>public void stopPoller()</i></b></p>
     * <p>
     * This method is used to stop current connection poller. Stopping connection poller will causing message from N-Service
     * devices cannot be retrieved by {@link NSConnection} and after several seconds, the N-Service device will close the
     * connection with android device. Closed connection will causing any messaging from and to N-Service device will failed.
     * </p>
     * <p>
     * This method should not be called by external instance, consider using <i>disconnect</i> method to close the connection
     * cleanly. The <i>disconnect</i> method also will stop the connection poller automatically.
     * </p>
     *
     * @since 1.0.0
     */
    public void stopPoller() {
        this.setConnected(false);
    }

    /**
     * <p><b><i>public void connect({@link NSDevice} device)</i></b></p>
     * <p>
     * Attempt connection to N-Service device. The connection is required to perform another {@link NSConnection} method
     * such as messaging and disconnecting.
     * </p>
     *
     * @since 1.0.1
     */
    public void connect(NSDevice device) {
        this.tvName = device.getName();
        this.connect(device, device.getIP());
    }

    /**
     * <p><b><i>public void connect(String tvAddres)</i></b></p>
     * <p>
     * Attempt connection to N-Service device. The connection is required to perform another {@link NSConnection} method
     * such as messaging and disconnecting.
     * </p>
     * <p><b>Note:<br />Connecting using this method did not store TV Device Name,
     * use </b><i>connect({@link NSDevice} device)</i><b> method instead.
     * </b></p>
     *
     * @param device
     * @param tvAddress - IPv4 Address of N-Service Device
     * @since 1.0.0
     */
    public void connect(final NSDevice device, String tvAddress) {
        if (!this.isWifiConnected()) {
            SmartLog.w(NSConnection.LOG_TAG + ".connect", "Aborting Connect: Wireless Network Not Connected");
            return;
        }

        this.setTvAddress(tvAddress);
        Thread connector = new Thread(new NSRunnable(this) {

            @Override
            public void run() {
                String requestUrl = NSConnection.buildRequestUrl(this.executor, NSConnection.NSERVICE_CONNECT);
                HttpClient client = NSConnection.createHttpClient();
                HttpPost request = new HttpPost(requestUrl);
                request.setHeader("SLDeviceID", this.executor.getAndroidDeviceId());
                request.setHeader("DeviceName", this.executor.getAndroidDeviceName());
                request.setHeader("VendorID", "SEINPSTV");
                request.setHeader("ProductID", "SMARTAnd");

                SmartLog.d(LOG_TAG, "Connecting to: " + requestUrl);
                SmartLog.d(LOG_TAG, "with headers " + this.executor.getAndroidDeviceId() + " " + this.executor.getAndroidDeviceName());

                try {
                    HttpResponse response = client.execute(request);
                    int responseCode = response.getStatusLine().getStatusCode();
                    String responsePhrase = response.getStatusLine().getReasonPhrase();

                    SmartLog.d(LOG_TAG, "Connecting to: " + this.executor.getTvAddress());
                    SmartLog.d(LOG_TAG, "-- Connect URL: " + request.getURI().toString());
                    SmartLog.d(LOG_TAG, "-- Response: " + responsePhrase + " (" + String.valueOf(responseCode) + ")");

                    switch (responseCode) {
                        case NSConnection.CONN_SUCCESS:
                            this.executor.setConnected(true);
                            this.executor.setLostCount(0);
                            this.executor.startPoller();
                            this.executor.notifyConnected(device);
                            break;
                        default:
                            this.executor.setConnected(false);
                            this.executor.notifyConnectionFailed(responseCode);
                            break;
                    }
                    response.getEntity().consumeContent();
                } catch (ClientProtocolException e) {
                    SmartLog.e(LOG_TAG, "ClientProtocolException: " + e.getMessage());
                    this.executor.notifyConnectionFailed(NSConnection.CONN_EXCEPTION);
                } catch (IOException e) {
                    SmartLog.e(LOG_TAG, "IOException: " + e.getMessage());
                    this.executor.notifyConnectionFailed(NSConnection.CONN_EXCEPTION);
                }
            }
        });
        connector.setPriority(Thread.NORM_PRIORITY);
        connector.start();
    }

    /**
     * <p><b><i>public void disconnect()</i></b></p>
     * <p>
     * Disconnecting the current connection with N-Service device. This method should be called only when there is a
     * connected N-Service device.
     * </p>
     *
     * @since 1.0.0
     */
    public void disconnect() {
        if (!this.isConnected()) {
            SmartLog.w(NSConnection.LOG_TAG + ".disconnect", "Aborting Disconnect: Wireless Network Not Connected");
            return;
        }

        this.stopPoller();
        Thread disconnector = new Thread(new NSRunnable(this) {

            @Override
            public void run() {
                String requestUrl = NSConnection.buildRequestUrl(this.executor, NSConnection.NSERVICE_DISCONNECT);
                HttpClient client = NSConnection.createHttpClient();
                HttpPost request = new HttpPost(requestUrl);
                request.setHeader("SLDeviceID", this.executor.getAndroidDeviceId());

                try {
                    HttpResponse response = client.execute(request);
                    int responseCode = response.getStatusLine().getStatusCode();
                    String responsePhrase = response.getStatusLine().getReasonPhrase();

                    SmartLog.d(NSConnection.LOG_TAG + ".disconnect", "Disconnecting from: " + this.executor.getTvAddress());
                    SmartLog.d(NSConnection.LOG_TAG + ".disconnect", "-- Disconnect URL: " + request.getURI().toString());
                    SmartLog.d(NSConnection.LOG_TAG + ".disconnect", "-- Response: " + responsePhrase + " (" + String.valueOf(responseCode) + ")");

                    if (responseCode == 200) {
                        this.executor.setLostCount(0);
                        this.executor.setConnected(false);
                        this.executor.notifyDisconnected();
                    }
                    response.getEntity().consumeContent();
                } catch (ClientProtocolException e) {
                    SmartLog.e(NSConnection.LOG_TAG + ".disconnect", "ClientProtocolException: " + e.getMessage());
                } catch (IOException e) {
                    SmartLog.e(NSConnection.LOG_TAG + ".disconnect", "IOException: " + e.getMessage());
                }
            }
        });
        disconnector.setPriority(Thread.MIN_PRIORITY);
        disconnector.start();
    }

    /**
     * <p><b><i>public void sendMessage(Object object)</i></b></p>
     * <p>
     * Serialize the Java object to String using {@link Gson} library and send the message to N-Service device. This
     * method require {@link Gson} library to works. The connection should be established before calling this function.
     * </p>
     *
     * @param object - Object to be serialized to JSON String using {@link Gson} library
     * @since 1.0.0
     */
    public void sendMessage(Object object) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        this.sendMessage(gson.toJson(object));
    }

    /**
     * <p><b><i>public void sendMessage(String message)</i></b></p>
     * <p>
     * Sending String message to N-Service device. The connection should be established before calling this function.
     * </p>
     *
     * @param message - String to be sent to N-Service device
     * @since 1.0.0
     */
    public void sendMessage(final String message) {
        if (!this.isConnected()) {
            SmartLog.w(LOG_TAG, "Aborting Send Message: Wireless Network Not Connected");
            return;
        }

        Thread messager = new Thread(new NSRunnable(this) {

            @Override
            public void run() {
                String requestUrl = NSConnection.buildRequestUrl(this.executor, NSConnection.NSERVICE_MESSAGE_SEND);
                HttpClient client = NSConnection.createHttpClient();
                HttpPost request = new HttpPost(requestUrl);
                request.setHeader("SLDeviceID", this.executor.getAndroidDeviceId());

                try {
                    request.setEntity(new StringEntity(message));

                    HttpResponse response = client.execute(request);
                    int responseCode = response.getStatusLine().getStatusCode();
                    String responsePhrase = response.getStatusLine().getReasonPhrase();

                    SmartLog.d(LOG_TAG, "Sending Message To: " + this.executor.getTvAddress());
                    SmartLog.d(LOG_TAG, "-- Sending Message URL: " + request.getURI().toString());
                    SmartLog.d(LOG_TAG, "-- Response: " + responsePhrase + " (" + String.valueOf(responseCode) + ")");

                    if (responseCode == 200) {
                        this.executor.notifyMessageSent();
                    } else {
                        this.executor.notifyMessageSendFailed(responseCode);
                    }

                    response.getEntity().consumeContent();
                } catch (UnsupportedEncodingException e) {
                    SmartLog.e(LOG_TAG, "UnsupportedEncodingException: " + e.getMessage());
                    this.executor.notifyMessageSendFailed(NSConnection.CONN_EXCEPTION);
                } catch (ClientProtocolException e) {
                    SmartLog.e(LOG_TAG, "ClientProtocolException: " + e.getMessage());
                    this.executor.notifyMessageSendFailed(NSConnection.CONN_EXCEPTION);
                } catch (IOException e) {
                    SmartLog.e(LOG_TAG, "IOException: " + e.getMessage());
                    this.executor.notifyMessageSendFailed(NSConnection.CONN_EXCEPTION);
                }
            }
        });
        messager.setPriority(Thread.MIN_PRIORITY);
        messager.start();
    }

    /**
     * <p><b><i>public String getAppId()</i></b></p>
     *
     * @return application id of N-Service Application
     * @since 1.0.0
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * <p><b><i>public void setTvAddress(String tvAddress)</i></b></p>
     *
     * @param tvAddress - IPv4 Address of N-Service Device
     * @since 1.0.0
     */
    public void setTvAddress(String tvAddress) {
        this.tvAddress = tvAddress;
    }

    /**
     * <p><b><i>public String getTvAddress()</i></b></p>
     *
     * @return N-Service Device IPv4 Address
     * @since 1.0.0
     */
    public String getTvAddress() {
        return this.tvAddress;
    }

    /**
     * <p><b><i>public String getTvName()</i></b></p>
     *
     * @return TV Device Friendly Name
     * @since 1.0.1
     */
    public String getTvName() {
        return this.tvName;
    }

    /**
     * <p><b><i>public boolean isWifiConnected()</i></b></p>
     *
     * @return true - android wifi connected<br />false - android wifi disconnected/invalid
     * @since 1.0.0
     */
    public boolean isWifiConnected() {
        String ssid = this.wifiManager.getConnectionInfo().getSSID();
        if (ssid == null || ssid.contains("unknown") || ssid.isEmpty() || ssid.equals("0x"))
            return false;
        else return true;
    }

    /**
     * <p><b><i>public String getWifiSSID()</i></b></p>
     * <p>
     * Retrieving currently connected Wireless Network SSID, This method may returning:
     * [null, "*unknown*", "0x", etc] when Android is in Invalid Wireless Connection state.
     *
     * @return Connected Wireless Network SSID
     * @since 1.0.1
     */
    public String getWifiSSID() {
        return this.wifiManager.getConnectionInfo().getSSID();
    }

    /**
     * <p><b><i>public String getAndroidDeviceId()</i></b></p>
     *
     * @return Device ID for N-Service framework identifier based on android wireless card MAC Address
     * @since 1.0.0
     */
    public String getAndroidDeviceId() {
        if (this.isWifiConnected()) {
            return this.wifiManager.getConnectionInfo().getMacAddress().replace(":", "");
        } else {
            return null;
        }
    }

    /**
     * <p><b><i>public String getAndroidDeviceName()</i></b></p>
     *
     * @return Android Device Friendly Name / Device Model (ex: GT-I9300)
     * @since 1.0.0
     */
    public String getAndroidDeviceName() {
        return android.os.Build.MODEL;
    }

    /**
     * <p><b><i>public boolean isConnected()</i></b></p>
     *
     * @return true - NSConnection connected with N-Service Device<br />false - Not Connected
     * @since 1.0.0
     */
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getLostCount() {
        return lostCount;
    }

    public void setLostCount(int lostCount) {
        this.lostCount = lostCount;
    }

    /**
     * <p><b><i>public void clearDevices()</i></b></p>
     * <p>
     * Clear all found {@link NSDevice} in the networks.
     * </p>
     *
     * @since 1.0.0
     */
    public void clearDevices() {
        this.devices.clear();
    }

    public void addDevice(NSDevice device) {
        this.devices.add(device);
        this.notifyDeviceChanged();
    }

    /**
     * <p><b><i>public void setSingleListener({@link NSListener} listener)</i></b></p>
     * <p>
     * Remove all other listeners and made specified listener as single events listener for NSConnection.
     * </p>
     *
     * @param listener - Single listener for NSConnection events
     * @since 1.0.0
     */
    public void setSingleListener(NSListener listener) {
        this.listener.clear();
        this.listener.add(listener);
    }

    /**
     * <p><b><i>public void registerListener({@link NSListener} listener)</i></b></p>
     * <p>
     * Registering new listener that subscribe to NSConnection events. All listener registered will receive same events
     * from NSConnection.
     * </p>
     *
     * @param listener - new listener subscribed to NSConnection events.
     * @since 1.0.0
     */
    public void registerListener(NSListener listener) {
        this.listener.add(listener);
    }

    /**
     * <p><b><i>public void deleteListener({@link NSListener} listener)</i></b></p>
     *
     * @param listener - listener to be unsubsribed from events
     * @since 1.0.0
     */
    public void deleteListener(NSListener listener) {
        this.deleteListener(listener.getTag());
    }

    public void deleteListener(String tag) {
        NSListener temp = this.getListener(tag);
        if (temp != null) {
            this.listener.remove(temp);
        }
    }

    public boolean isListening(NSListener listener) {
        return this.isListening(listener.getTag());
    }

    public boolean isListening(String tag) {
        if (this.getListener(tag) == null) return false;
        else return true;
    }

    private NSListener getListener(String tag) {
        for (NSListener i : this.listener) {
            if (i.getTag().equals(tag)) {
                return i;
            }
        }
        return null;
    }

    private void notifyDeviceChanged() {
        for (NSListener listener : this.listener) {
            listener.onDeviceChanged(this.devices);
        }
    }

    private void notifyConnected(NSDevice device) {
        for (NSListener listener : this.listener) {
            listener.onConnected(device);
        }
    }

    private void notifyConnectionFailed(int code) {
        for (NSListener listener : this.listener) {
            listener.onConnectionFailed(code);
        }
    }

    private void notifyDisconnected() {
        for (NSListener listener : this.listener) {
            listener.onDisconnected();
        }
    }

    private void notifyMessageSent() {
        for (NSListener listener : this.listener) {
            listener.onMessageSent();
        }
    }

    private void notifyMessageSendFailed(int code) {
        for (NSListener listener : this.listener) {
            listener.onMessageSendFailed(code);
        }
    }

    private void notifyMessageReceived(String message) {
        for (NSListener listener : this.listener) {
            listener.onMessageReceived(message);
        }
    }

    public void notifyWifiChange() {
        for (NSListener listener : this.listener) {
            listener.onWifiChanged();
        }
    }

    public void dispose() {
        this.discovery.dispose();
    }

    public static String buildRequestUrl(NSConnection connection, int requestType) {
        return NSConnection.buildRequestUrl(connection.getTvAddress(), connection.getAppId(), requestType, connection.getAndroidDeviceId());
    }

    public static String buildRequestUrl(String tvIpAddress, String appId, int requestType, String deviceId) {
        String baseUrl = "http://" + tvIpAddress + "/ws/app/" + appId;

        switch (requestType) {
            case NSConnection.NSERVICE_CONNECT:
                baseUrl += "/connect";
                break;
            case NSConnection.NSERVICE_DISCONNECT:
                baseUrl += "/disconnect";
                break;
            case NSConnection.NSERVICE_MESSAGE_SEND:
                baseUrl += "/queue";
                break;
            case NSConnection.NSERVICE_MESSAGE_POLLER:
                baseUrl += "/queue/device/" + deviceId;
                break;
            default:
                baseUrl = null;
                break;
        }

        return baseUrl;
    }

    public static DefaultHttpClient createHttpClient() {
        return new DefaultHttpClient(null, new DefaultHttpClient().getParams());
    }

    abstract class NSRunnable implements Runnable {

        protected NSConnection executor;

        public NSRunnable(NSConnection executor) {
            this.executor = executor;
        }
    }
}
