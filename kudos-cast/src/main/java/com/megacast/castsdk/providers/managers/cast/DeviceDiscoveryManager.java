package com.megacast.castsdk.providers.managers.cast;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ReceiverInfo;

import java.util.List;

import rx.Observable;

/**
 * Created by Dmitry on 14.09.16.
 */
public interface DeviceDiscoveryManager {

    void discoverAvailableDevices();

    void stopDiscovery();

    Observable<List<Device>> getDevicesObservable();

    boolean isDiscoveryInProgress();

    boolean isReceiverRequired();

    boolean isReceiverRequired(Device device);

    Observable<Void> installReceiver();

    Observable<Void> installReceiver(Device device);

    Observable<ReceiverInfo> showReceiver(Device device, String receiverLaunchParams);

    Observable<Void> closeReceiver(Device device);

    Observable<ReceiverInfo> getReceiverState(Device device);

    void disableInstallService();

    void setVersion(int deviceCode, String version);
}
