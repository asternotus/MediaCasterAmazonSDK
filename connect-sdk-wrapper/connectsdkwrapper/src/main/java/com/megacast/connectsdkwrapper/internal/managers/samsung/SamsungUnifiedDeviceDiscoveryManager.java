package com.megacast.connectsdkwrapper.internal.managers.samsung;

import android.content.Context;

import com.mega.cast.utils.log.SmartLog;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ReceiverInfo;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subjects.PublishSubject;

/**
 * Created by Dmitry on 20.04.17.
 */

public class SamsungUnifiedDeviceDiscoveryManager implements DeviceDiscoveryManager {

    private static final String LOG_TAG = SamsungUnifiedDeviceDiscoveryManager.class.getSimpleName();

    private SamsungConvergenceDeviceDiscoveryManager orsayDiscoveryManager;
    private SamsungSmartViewDeviceDiscoveryManager tizenDiscoveryManager;

    private List<Device> convergenceDevices = new ArrayList<>();
    private List<Device> smartViewDevices = new ArrayList<>();

    private PublishSubject<List<Device>> devicePublisher = PublishSubject.create();

    private Subscription orsaySubscription;
    private Subscription tizenSubscription;

    public SamsungUnifiedDeviceDiscoveryManager(Context context, String orsayAppId, String orsayReceiverVersion,
                                                String tizenChannelId, String tizenAppId, String tizenReceiverVersion, String appType) {
        orsayDiscoveryManager = new SamsungConvergenceDeviceDiscoveryManager(context, orsayAppId, orsayReceiverVersion, appType);
        tizenDiscoveryManager = new SamsungSmartViewDeviceDiscoveryManager(context, tizenChannelId, tizenAppId, orsayAppId, tizenReceiverVersion, appType);
    }

    @Override
    public void discoverAvailableDevices() {
        SmartLog.t(LOG_TAG, "discoverAvailableDevices");

        orsaySubscription = orsayDiscoveryManager.getDevicesObservable()
                .subscribe(new Subscriber<List<Device>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        devicePublisher.onError(e);
                    }

                    @Override
                    public void onNext(List<Device> devices) {
                        SmartLog.t(LOG_TAG, "got orsay devices " + devices.size());
                        convergenceDevices = devices;
                        updateDevices();
                    }
                });

        tizenSubscription = tizenDiscoveryManager.getDevicesObservable()
                .subscribe(new Subscriber<List<Device>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        devicePublisher.onError(e);
                    }

                    @Override
                    public void onNext(List<Device> devices) {
                        SmartLog.t(LOG_TAG, "got tizen devices " + devices.size());
                        smartViewDevices = devices;
                        updateDevices();
                    }
                });

        tizenDiscoveryManager.discoverAvailableDevices();
        orsayDiscoveryManager.discoverAvailableDevices();
    }

    @Override
    public void stopDiscovery() {
        SmartLog.t(LOG_TAG, "stopDiscovery ");

        orsayDiscoveryManager.stopDiscovery();
        tizenDiscoveryManager.stopDiscovery();

        if (orsaySubscription != null && !orsaySubscription.isUnsubscribed()) {
            orsaySubscription.unsubscribe();
        }
        if (tizenSubscription != null && !tizenSubscription.isUnsubscribed()) {
            tizenSubscription.unsubscribe();
        }
    }

    @Override
    public Observable<List<Device>> getDevicesObservable() {
        return devicePublisher.asObservable();
    }

    @Override
    public boolean isDiscoveryInProgress() {
        return orsayDiscoveryManager.isDiscoveryInProgress() && tizenDiscoveryManager.isDiscoveryInProgress();
    }

    @Override
    public boolean isReceiverRequired() {
        return true;
    }

    @Override
    public boolean isReceiverRequired(Device device) {
        return true;
    }

    @Override
    public Observable<Void> installReceiver() {
        return Observable.error(new Throwable("Device has no application management capabilities!"));
    }

    @Override
    public Observable<Void> installReceiver(Device device) {
        return Observable.error(new Throwable("Device has no application management capabilities!"));
    }

    @Override
    public Observable<ReceiverInfo> showReceiver(Device device, String receiverLaunchParams) {
        return Observable.error(new Throwable("Device has no application management capabilities!"));
    }

    @Override
    public Observable<Void> closeReceiver(Device device) {
        return Observable.error(new Throwable("Device has no application management capabilities!"));
    }

    @Override
    public Observable<ReceiverInfo> getReceiverState(Device device) {
        return Observable.error(new Throwable("Device has no application management capabilities!"));
    }

    @Override
    public void disableInstallService() {

    }

    @Override
    public void setVersion(int deviceCode, String version) {
        orsayDiscoveryManager.setVersion(deviceCode, version);
        tizenDiscoveryManager.setVersion(deviceCode, version);
    }

    private void updateDevices() {
        HashSet<Device> devices = new HashSet<>();

        for (Device device : smartViewDevices) {
            String ip = device.getIpAdress();
            if (ip.startsWith("http://")) {
                final int portIndex = ip.indexOf(":", "http://".length());
                ip = ip.substring("http://".length(), portIndex > 0 ? portIndex : ip.length());

                Device deviceDuplicate = null;

                for (Device orsayDevice : convergenceDevices) {
                    if (orsayDevice.getIpAdress().equals(ip)) {
                        deviceDuplicate = orsayDevice;
                    }
                }

                if (deviceDuplicate != null) {
                    SmartLog.t(LOG_TAG, "remove orsay duplicate of tizen device ");
                    convergenceDevices.remove(deviceDuplicate);
                }
            }
        }

        devices.addAll(smartViewDevices);
        devices.addAll(convergenceDevices);

        SmartLog.d(LOG_TAG, "current devices:");

        for (Device smartViewDevice : smartViewDevices) {
            SmartLog.d(LOG_TAG, "Smart View " + smartViewDevice.getIpAdress());
        }

        for (Device smartViewDevice : convergenceDevices) {
            SmartLog.d(LOG_TAG, "Convergence " + smartViewDevice.getIpAdress());
        }

        devicePublisher.onNext(new ArrayList<>(devices));
    }

}
