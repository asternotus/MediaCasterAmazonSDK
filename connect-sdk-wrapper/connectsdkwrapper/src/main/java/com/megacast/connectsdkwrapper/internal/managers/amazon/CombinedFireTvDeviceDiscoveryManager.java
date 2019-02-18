package com.megacast.connectsdkwrapper.internal.managers.amazon;

import android.content.Context;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.FireTVService;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.mega.cast.utils.log.SmartLog;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ReceiverInfo;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * Created by Дима on 09.01.2018.
 */

public class CombinedFireTvDeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = CombinedFireTvDeviceDiscoveryManager.class.getSimpleName();

    private static final String DEFAULT_PLAYER_ID = "amzn.thin.pl";

    private final AmazonDeviceDiscoveryManager customReceiverDiscoveryManager;
    private final AmazonDeviceDiscoveryManager defaultDiscoveryManager;
    private final String receiverAppId;

    private List<Device> customDevices = new ArrayList<>();
    private List<Device> defaultDevices = new ArrayList<>();

    private PublishSubject<List<Device>> devicePublisher = PublishSubject.create();

    private Subscription orsaySubscription;
    private Subscription tizenSubscription;

    public CombinedFireTvDeviceDiscoveryManager(Context context, String receiverAppId, String asin, RemoteApplicationManager remoteApplicationManager) {
        super(context, Device.CODE_AMAZON_FIRE);

        this.receiverAppId = receiverAppId;
        customReceiverDiscoveryManager = new AmazonDeviceDiscoveryManager(context, receiverAppId, asin, remoteApplicationManager);

        defaultDiscoveryManager = new AmazonDeviceDiscoveryManager(context, DEFAULT_PLAYER_ID, asin, remoteApplicationManager);
    }

    @Override
    public void discoverAvailableDevices() {
        SmartLog.t(LOG_TAG, "discoverAvailableDevices");

        orsaySubscription = customReceiverDiscoveryManager.getDevicesObservable()
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
                        SmartLog.t(LOG_TAG, "got custom devices " + devices.size());
                        customDevices = devices;
                        updateDevices();
                    }
                });

        tizenSubscription = defaultDiscoveryManager.getDevicesObservable()
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
                        SmartLog.t(LOG_TAG, "got default devices " + devices.size());
                        defaultDevices = devices;
                        updateDevices();
                    }
                });

        customReceiverDiscoveryManager.discoverAvailableDevices();
        defaultDiscoveryManager.discoverAvailableDevices();
    }

    @Override
    public void stopDiscovery() {
        SmartLog.t(LOG_TAG, "stopDiscovery ");

        customReceiverDiscoveryManager.stopDiscovery();
        defaultDiscoveryManager.stopDiscovery();

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
        return customReceiverDiscoveryManager.isDiscoveryInProgress() && defaultDiscoveryManager.isDiscoveryInProgress();
    }

    @Override
    public boolean isReceiverRequired() {
        return true;
    }

    @Override
    public Observable<Void> installReceiver() {
        return customReceiverDiscoveryManager.installReceiver();
    }

    @Override
    public Observable<ReceiverInfo> showReceiver(final Device device, String receiverLaunchParams) {
        return customReceiverDiscoveryManager.showReceiver(device, receiverLaunchParams);
    }

    @Override
    public Observable<Void> closeReceiver(final Device device) {
        return customReceiverDiscoveryManager.closeReceiver(device);
    }

    @Override
    public Observable<ReceiverInfo> getReceiverState(Device device) {
        return customReceiverDiscoveryManager.getReceiverState(device);
    }

    /*
    @Override
    public Observable<ReceiverInfo> getReceiverState(final Device deviceAmazon) {
        return Observable.create(
                new Observable.OnSubscribe<ReceiverInfo>() {
                    public ReceiverInfo customReceiverInfo = null;
                    public ReceiverInfo defaultReceiverInfo = null;

                    public void updateInfo(Subscriber<? super ReceiverInfo> sub) {
                        if (customReceiverInfo != null && defaultReceiverInfo != null) {
                            customReceiverInfo.setRunning(customReceiverInfo.isRunning() || defaultReceiverInfo.isRunning());
                            sub.onNext(customReceiverInfo);
                            sub.onCompleted();
                        }
                    }

                    @Override
                    public void call(final Subscriber<? super ReceiverInfo> sub) {
                        customReceiverDiscoveryManager
                                .getReceiverState(deviceAmazon)
                                .subscribe(new Subscriber<ReceiverInfo>() {
                                    @Override
                                    public void onCompleted() {

                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        sub.onError(e);
                                    }

                                    @Override
                                    public void onNext(ReceiverInfo receiverInfo) {
                                        customReceiverInfo = receiverInfo;
                                        updateInfo(sub);
                                    }
                                });

                        defaultDiscoveryManager
                                .getReceiverState(deviceAmazon)
                                .subscribe(new Subscriber<ReceiverInfo>() {
                                    @Override
                                    public void onCompleted() {

                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        sub.onError(e);
                                    }

                                    @Override
                                    public void onNext(ReceiverInfo receiverInfo) {
                                        defaultReceiverInfo = receiverInfo;
                                        updateInfo(sub);
                                    }
                                });
                    }
                }
        );
    }
*/

    @Override
    public void disableInstallService() {
        super.disableInstallService();
        customReceiverDiscoveryManager.disableInstallService();
    }

    private void updateDevices() {
        HashSet<Device> devices = new HashSet<>();

        for (Device defaultDevice : defaultDevices) {
            SmartLog.d(LOG_TAG, "device info " + (defaultDevice.getReceiverInfo() != null));
            for (Device customDevice : customDevices) {
                SmartLog.d(LOG_TAG, "custom device info " + (defaultDevice.getReceiverInfo() != null));
                if (defaultDevice.equals(customDevice)) {
                    ConnectableDevice connectableDevice = ((TvDevice) defaultDevice).getConnectableDevice();

                    MediaControl mediaControl = connectableDevice.getMediaControl();
                    Launcher launcher = connectableDevice.getLauncher();
                    if (mediaControl instanceof FireTVService) {
                        ((FireTVService) mediaControl).setLauncher(launcher, receiverAppId);
                    }

                    defaultDevice.setReceiverInfo(customDevice.getReceiverInfo());

                    if (isMediaCompatible(connectableDevice)) {
                        devices.add(defaultDevice);
                    }
                    break;
                }
            }
        }

        devicePublisher.onNext(new ArrayList<>(devices));
    }
}
