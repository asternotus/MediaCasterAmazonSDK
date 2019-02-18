package com.megacast.connectsdkwrapper.internal.managers.roku;

import android.content.Context;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.RokuService;
import com.connectsdk.service.capability.MediaPlayer;
import com.mega.cast.utils.log.SmartLog;
import com.megacast.castsdk.model.ApplicationSession;
import com.megacast.castsdk.model.ApplicationState;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ReceiverInfo;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.model.RokuDevice;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Dmitry on 05.10.16.
 */

public class RokuDeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = RokuDeviceDiscoveryManager.class.getSimpleName();

    private final String[] keywords = {"roku"};

    private final String receiverAppId;
    private String clientVersion;
    private final String appType;

    private final RemoteApplicationManager remoteApplicationManager;

    public RokuDeviceDiscoveryManager(Context context, String receiverAppId,
                                      String version, String appType,
                                      RemoteApplicationManager remoteApplicationManager) {
        super(context, Device.CODE_ROKU);
        this.receiverAppId = receiverAppId;
        this.clientVersion = version;
        this.appType = appType;
        this.remoteApplicationManager = remoteApplicationManager;

        this.setDeviceFilter(new DeviceFilter() {
            @Override
            public boolean acceptDevice(ConnectableDevice device) {
                return isMediaCompatible(device);
            }
        });
    }

    @Override
    public void discoverAvailableDevices() {
        super.discoverAvailableDevices();
        SmartLog.i(LOG_TAG, "discoverAvailableDevices");
    }

    @Override
    public void stopDiscovery() {
        super.stopDiscovery();
        SmartLog.i(LOG_TAG, "stopDiscovery");
    }

    @Override
    public void setVersion(int deviceCode, String version) {
        super.setVersion(deviceCode, version);
        this.clientVersion = version;
    }

    @Override
    protected TvDevice createDevice(ConnectableDevice device) {
        return new RokuDevice(device);
    }

    @Override
    public boolean isReceiverRequired() {
        return true;
    }

    @Override
    public Observable<Void> installReceiver(final Device device) {
        SmartLog.i(LOG_TAG, "installReceiver ");
        return remoteApplicationManager.installApplication(device, receiverAppId)
                .map(new Func1<Object, Void>() {
                    @Override
                    public Void call(Object o) {
                        return null;
                    }
                });
    }

    @Override
    public Observable<ReceiverInfo> showReceiver(final Device device, String receiverLaunchParams) {
        SmartLog.i(LOG_TAG, "showReceiver ");
        return remoteApplicationManager.launchApplication(device, receiverAppId, receiverLaunchParams)
                .flatMap(new Func1<ApplicationSession, Observable<ReceiverInfo>>() {
                    @Override
                    public Observable<ReceiverInfo> call(ApplicationSession applicationSession) {
                        return getReceiverState(device);
                    }
                });
    }

    @Override
    public Observable<Void> closeReceiver(final Device device) {
        return remoteApplicationManager.closeApplication(device, receiverAppId)
                .map(new Func1<Void, Void>() {
                    @Override
                    public Void call(Void aVoid) {
                        device.setReceiverInfo(new ReceiverInfo(false));
                        return aVoid;
                    }
                });
    }

    @Override
    public Observable<ReceiverInfo> getReceiverState(final Device deviceRoku) {
        SmartLog.i(LOG_TAG, "getReceiverState " + deviceRoku.getIpAdress() + " " + receiverAppId);
        return remoteApplicationManager.getApplicationState(deviceRoku, receiverAppId)
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        SmartLog.e(LOG_TAG, String.format("Could not check receiver state \n " +
                                "ip address: %s \n " +
                                "app id: %s \n ", deviceRoku.getIpAdress(), receiverAppId));
                    }
                })
                .map(new Func1<ApplicationState, ReceiverInfo>() {
                    @Override
                    public ReceiverInfo call(ApplicationState applicationState) {
                        SmartLog.d(LOG_TAG, String.format("Receiver state:  \n " +
                                        "ip address: %s \n " +
                                        "app id: %s \n " +
                                        "running: %b \n" +
                                        "visible: %b",
                                deviceRoku.getIpAdress(), receiverAppId,
                                applicationState.isRunning(), applicationState.isVisible()));

                        final boolean remoteAppRunning = applicationState.isRunning() && applicationState.isVisible();

                        final ReceiverInfo receiverInfo = new ReceiverInfo(remoteAppRunning);
                        deviceRoku.setReceiverInfo(receiverInfo);

                        return receiverInfo;
                    }
                });
    }

    @Override
    public void onDeviceAdded(final DiscoveryManager manager, final ConnectableDevice device) {
        boolean hasDialService = isDialCompatible(device);
        boolean hasMediaService = isMediaCompatible(device);

        SmartLog.d(LOG_TAG, String.format("onDeviceAdded:" +
                "\n name: %s" +
                "\n dial service: %b" +
                "\n media service: %b", device.getFriendlyName(), hasDialService, hasMediaService));

        final Device deviceRoku = getDevice(device);

        SmartLog.d(LOG_TAG, "compatible devices: " + manager.getCompatibleDevices().size());

        getReceiverState(deviceRoku)
                .subscribe(new Subscriber<ReceiverInfo>() {
                    @Override
                    public void onCompleted() {
                        SmartLog.d(LOG_TAG, "checkReceiverState onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        SmartLog.e(LOG_TAG, "checkReceiverState " + e.getMessage());
                        deviceRoku.setReceiverInfo(null);
                        RokuDeviceDiscoveryManager.super.onDeviceAdded(manager, device);
                    }

                    @Override
                    public void onNext(ReceiverInfo receiverInfo) {
                        deviceRoku.setReceiverInfo(receiverInfo);
                        RokuDeviceDiscoveryManager.super.onDeviceAdded(manager, device);
                    }
                });
    }

    @Override
    public void onDeviceUpdated(final DiscoveryManager manager, final ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "onDeviceUpdated");

        SmartLog.d(LOG_TAG, String.format("device update %s :: %s",
                device.getFriendlyName(), device.getIpAddress()));

        final Device deviceRoku = getDevice(device);

        getReceiverState(deviceRoku)
                .subscribe(new Subscriber<ReceiverInfo>() {
                    @Override
                    public void onCompleted() {
                        SmartLog.d(LOG_TAG, "checkReceiverState onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        SmartLog.e(LOG_TAG, "checkReceiverState " + e.getMessage());
                        deviceRoku.setReceiverInfo(null);
                        RokuDeviceDiscoveryManager.super.onDeviceUpdated(manager, device);
                    }

                    @Override
                    public void onNext(ReceiverInfo receiverInfo) {
                        deviceRoku.setReceiverInfo(receiverInfo);
                        RokuDeviceDiscoveryManager.super.onDeviceUpdated(manager, device);
                    }
                });
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        super.onDeviceRemoved(manager, device);
        SmartLog.d(LOG_TAG, "onDeviceRemoved");
    }

    @Override
    protected void updateDeviceService(ConnectableDevice device) {
        super.updateDeviceService(device);
        final MediaPlayer mediaPlayer = device.getMediaPlayer();
        if (mediaPlayer != null
                && mediaPlayer instanceof RokuService) {
            ((RokuService) mediaPlayer).setClientVersion(clientVersion);
            ((RokuService) mediaPlayer).setClientAppType(appType);
        }
    }

    private boolean isRokuDevice(ConnectableDevice device) {
        final String modelName = device.getModelName().toLowerCase();
        final String friendlyName = device.getFriendlyName().toLowerCase();

        boolean rokuDevice = false;

        for (int i = 0; i < keywords.length; i++) {
            if (modelName.contains(keywords[i]) || friendlyName.contains(keywords[i])) {
                rokuDevice = true;
                break;
            }
        }

        return rokuDevice;
    }

}
