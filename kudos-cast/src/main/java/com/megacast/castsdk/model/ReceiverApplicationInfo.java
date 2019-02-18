package com.megacast.castsdk.model;

/**
 * Created by Dmitry on 16.09.16.
 */
public class ReceiverApplicationInfo {

    private String remoteAppId;

    public ReceiverApplicationInfo(String remoteAppId, String asin) {
        this.remoteAppId = remoteAppId;
    }

    public String getRemoteAppId() {
        return remoteAppId;
    }

}
