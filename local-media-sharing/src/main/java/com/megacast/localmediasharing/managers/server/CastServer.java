package com.megacast.localmediasharing.managers.server;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ServerConnection;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;

import java.util.Map;

/**
 * Created by Dmitry on 21.09.16.
 */
public abstract class CastServer extends NanoHTTPD {

    private final WifiManager wifiManager;
    private final Context context;

    protected ServerConnection serverConnection;
    protected Device device;

    public CastServer(int port, Context context) {
        super(port);
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public CastServer(String hostname, int port, Context context) {
        super(hostname, port);
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected final void onBound() {
        setup();
        serverConnection = new ServerConnection(getRemoteUrl());
        super.onBound();
    }

    public String getRemoteUrl() {
        WifiInfo wifiInf = wifiManager.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();

        return String.format("http://%d.%d.%d.%d:%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff), myPort);
    }

    protected String getValidLink(String link) {
        return removeExtension(link).replace('.', '_').replace(' ', '_')
                .replace("[", "").replace("]", "").replace("'", "")
                .replace("(", "_").replace(")", "_").replace("-", "_").replace("+", "_");
    }

    protected String removeExtension(String loadedFileName) {
        return loadedFileName.replaceFirst("[.][^.]+$", "");
    }

    @Override
    protected Response serve(IHTTPSession session) {
        return serve(session.getUri(), session.getMethod(), session.getHeaders(), session.getParms(), session.getParms());
    }

    public Context getContext() {
        return context;
    }

    public abstract Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> params, Map<String, String> files);

    protected abstract void setup();

    public ServerConnection getServerConnection() {
        return serverConnection;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }
}
