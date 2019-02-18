package com.megacast.castsdk.model;

/**
 * Created by Dmitry on 14.09.16.
 */
public interface DeviceListener {

    void onDeviceReady();

    void onDeviceDisconnected();

    void onPairingRequired(int pairingType);

    void onConnectionFailed(Throwable error);

}
