package com.megacast.castsdk.model;

import com.mega.cast.utils.log.SmartLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Дима on 05.02.2018.
 */

public class ServerConnection {

    private static final String LOG_TAG = ServerConnection.class.getSimpleName();

    private String url;

    private long bytesPushed = 0;
    private long bytesTotal = 0;

    private List<ServerConnectionListener> listeners = new ArrayList<>();

    private boolean available = true;
    private long monitoringStartedMillisec = 0;
    private long monitoringBytes = 0;
    private double kbsec = 0;

    public ServerConnection(String url) {
        this.url = url;
    }

    public void update(long bytesPushed, long bytesTotal) {
        this.bytesPushed = bytesPushed;
        this.bytesTotal = bytesTotal;

        long timeDiff = System.currentTimeMillis() - monitoringStartedMillisec;
        long bytesDiff = this.bytesPushed - monitoringBytes;
        long kbBytesDiff = bytesDiff / 1000;

        double latestTimeSec = ((double) timeDiff / 1000);
        this.kbsec = kbBytesDiff / latestTimeSec;

        for (ServerConnectionListener listener : listeners) {
            listener.onUpdate();
        }
    }

    public void onMonitoringStarted() {
        monitoringBytes = this.bytesPushed;
        monitoringStartedMillisec = System.currentTimeMillis();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getBytesPushed() {
        return bytesPushed;
    }

    public void setBytesPushed(long bytesPushed) {
        this.bytesPushed = bytesPushed;
    }

    public long getBytesTotal() {
        return bytesTotal;
    }

    public void setBytesTotal(long bytesTotal) {
        this.bytesTotal = bytesTotal;
    }

    public void addListener(ServerConnectionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ServerConnectionListener listener) {
        listeners.remove(listener);
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public double getKbsec() {
        return kbsec;
    }

    public interface ServerConnectionListener {
        void onUpdate();
    }
}
