package com.megacast.connectsdkwrapper.internal.managers.amazon;

import android.content.Context;
import android.text.TextUtils;

import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.provider.FireTVDiscoveryProvider;
import com.connectsdk.discovery.provider.firetv.FireTVApplicationProvider;
import com.connectsdk.service.command.ServiceCommandError;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.model.AmazonDevice;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;
import com.megacast.castsdk.model.ApplicationSession;
import com.megacast.castsdk.model.ApplicationState;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ReceiverInfo;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Dmitry on 16.09.16.
 */
public class AmazonDeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = AmazonDeviceDiscoveryManager.class.getSimpleName();

    private final String[] keywords = {"fire", "amazon"};

    private final RemoteApplicationManager remoteApplicationManager;
    private final String asin;
    private final String receiverAppId;

    private Map<String, StoredDeviceInfo> storedInfoMap;

    public AmazonDeviceDiscoveryManager(Context context, String receiverAppId, String asin, RemoteApplicationManager remoteApplicationManager) {
        super(context, Device.CODE_AMAZON_FIRE);
        this.remoteApplicationManager = remoteApplicationManager;

        this.storedInfoMap = new HashMap<>();

        this.receiverAppId = receiverAppId;
        this.asin = asin;

        setRemotePlayerId(receiverAppId);

        this.setDeviceFilter(new DeviceFilter() {
            @Override
            public boolean acceptDevice(ConnectableDevice device) {
                SmartLog.d(LOG_TAG, "device.getModelName " + device.getModelName());
                return isDialCompatible(device);
            }
        });
    }

    @Override
    public boolean isReceiverRequired() {
        return true;
    }

    @Override
    public Observable<Void> installReceiver() {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        getFireTvDiscoveryProvider()
                                .attemptToInstallApplication(asin, new FireTVApplicationProvider.InstallApplicationListener() {
                                    @Override
                                    public void onInstall(String deviceName) {
                                        sub.onNext(null);
                                        sub.onCompleted();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        sub.onError(e);
                                    }
                                });
                    }
                }
        );
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
    public Observable<ReceiverInfo> getReceiverState(final Device deviceAmazon) {
        SmartLog.i(LOG_TAG, "getReceiverState " + deviceAmazon.getIpAdress() + " " + receiverAppId);
        return remoteApplicationManager.getApplicationState(deviceAmazon, receiverAppId)
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        SmartLog.e(LOG_TAG, String.format("Could not check receiver state \n " +
                                "ip address: %s \n " +
                                "app id: %s \n ", deviceAmazon.getIpAdress(), receiverAppId));
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
                                deviceAmazon.getIpAdress(), receiverAppId,
                                applicationState.isRunning(), applicationState.isVisible()));

                        final boolean remoteAppRunning = applicationState.isRunning() && applicationState.isVisible();

                        final ReceiverInfo receiverInfo = new ReceiverInfo(remoteAppRunning);
                        deviceAmazon.setReceiverInfo(receiverInfo);

                        return receiverInfo;
                    }
                });
    }

    @Override
    public void disableInstallService() {
        super.disableInstallService();
        getFireTvDiscoveryProvider().setInstallServiceEnabled(false);
    }

    //  <editor-fold desc="device listener">

    @Override
    public void onDeviceAdded(final DiscoveryManager manager, final ConnectableDevice device) {

        boolean hasDialService = isDialCompatible(device);
        boolean hasMediaService = isMediaCompatible(device);

        SmartLog.d(LOG_TAG, String.format("onDeviceAdded:" +
                "\n name: %s" +
                "\n dial service: %b" +
                "\n media service: %b", device.getFriendlyName(), hasDialService, hasMediaService));

        if (device.getFriendlyName().contains(" ")) {
            SmartLog.d(LOG_TAG, "put info into store " + device.getFriendlyName());
            storedInfoMap.put(device.getIpAddress(), new StoredDeviceInfo(device.getFriendlyName(),
                    device.getModelName(), device.getModelNumber()));
        }

        final Device deviceAmazon = getDevice(device);

        SmartLog.d(LOG_TAG, "compatible devices: " + manager.getCompatibleDevices().size());

        if (hasDialService) {
            getReceiverState(deviceAmazon)
                    .subscribe(new Subscriber<ReceiverInfo>() {
                        @Override
                        public void onCompleted() {
                            SmartLog.d(LOG_TAG, "checkReceiverState onCompleted");
                        }

                        @Override
                        public void onError(Throwable e) {
                            SmartLog.e(LOG_TAG, "checkReceiverState " + e.getMessage());
                            deviceAmazon.setReceiverInfo(null);
                            AmazonDeviceDiscoveryManager.super.onDeviceAdded(manager, device);
                        }

                        @Override
                        public void onNext(ReceiverInfo receiverInfo) {
                            deviceAmazon.setReceiverInfo(receiverInfo);
                            AmazonDeviceDiscoveryManager.super.onDeviceAdded(manager, device);
                        }
                    });
        }
    }

    @Override
    public void onDeviceUpdated(final DiscoveryManager manager, final ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "onDeviceUpdated");

        SmartLog.d(LOG_TAG, String.format("device update %s :: %s",
                device.getFriendlyName(), device.getIpAddress()));

        StoredDeviceInfo deviceInfo = storedInfoMap.get(device.getIpAddress());
        if (deviceInfo != null) {
            SmartLog.d(LOG_TAG, "found device info " + deviceInfo.getFriendlyName());
            device.setFriendlyName(deviceInfo.getFriendlyName());
            if (TextUtils.isEmpty(device.getModelName())) {
                device.setModelName(deviceInfo.getModelName());
            }
            if (TextUtils.isEmpty(device.getModelNumber())) {
                device.setModelNumber(deviceInfo.getModelNumber());
            }
        } else {
            SmartLog.e(LOG_TAG, "Device info not found ");
        }

        boolean hasDialService = isDialCompatible(device);
        boolean hasMediaService = isMediaCompatible(device);

        final Device deviceAmazon = getDevice(device);

        if (hasDialService) {
            getReceiverState(deviceAmazon)
                    .subscribe(new Subscriber<ReceiverInfo>() {
                        @Override
                        public void onCompleted() {
                            SmartLog.d(LOG_TAG, "checkReceiverState onCompleted");
                        }

                        @Override
                        public void onError(Throwable e) {
                            SmartLog.e(LOG_TAG, "checkReceiverState " + e.getMessage());
                            deviceAmazon.setReceiverInfo(null);
                            AmazonDeviceDiscoveryManager.super.onDeviceUpdated(manager, device);
                        }

                        @Override
                        public void onNext(ReceiverInfo receiverInfo) {
                            deviceAmazon.setReceiverInfo(receiverInfo);
                            AmazonDeviceDiscoveryManager.super.onDeviceUpdated(manager, device);
                        }
                    });
        }
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        super.onDeviceRemoved(manager, device);
        SmartLog.d(LOG_TAG, "onDeviceRemoved");
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        super.onDiscoveryFailed(manager, error);
        SmartLog.d(LOG_TAG, "onDiscoveryFailed");
    }

    //  </editor-fold>

    //  <editor-fold desc="protected">

    @Override
    protected TvDevice createDevice(ConnectableDevice device) {
        return new AmazonDevice(device);
    }


    //  </editor-fold>

    //  <editor-fold desc="private">

    private boolean isAmazonDevice(ConnectableDevice device) {
        final String modelName = device.getModelName();
        final String friendlyName = device.getFriendlyName();

        boolean amazonDevice = false;

        for (int i = 0; i < keywords.length; i++) {
            if ((modelName != null && modelName.toLowerCase().contains(keywords[i])) || (friendlyName != null && friendlyName.toLowerCase().contains(keywords[i]))) {
                amazonDevice = true;
                break;
            }
        }

//        return amazonDevice && isDialCompatible(device);

        return isDialCompatible(device);
    }

    private void setRemotePlayerId(String remoteAppId) {
        final FireTVDiscoveryProvider fireTvDiscoveryProvider = getFireTvDiscoveryProvider();
        if (fireTvDiscoveryProvider != null) {
            fireTvDiscoveryProvider.setPlayerId(remoteAppId);
        }
    }

    private FireTVDiscoveryProvider getFireTvDiscoveryProvider() {
        List<DiscoveryProvider> providers = discoveryManager.getDiscoveryProviders();
        for (DiscoveryProvider provider : providers) {
            if (provider instanceof FireTVDiscoveryProvider) {
                return ((FireTVDiscoveryProvider) provider);
            }
        }
        return null;
    }

    //  </editor-fold>

    //  <editor-fold desc="StoredDeviceInfo">

    static class StoredDeviceInfo {
        public String friendlyName;
        public String modelName;
        public String modelNumber;

        public StoredDeviceInfo(String friendlyName, String modelName, String modelNumber) {
            this.friendlyName = friendlyName;
            this.modelName = modelName;
            this.modelNumber = modelNumber;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public String getModelName() {
            return modelName;
        }

        public String getModelNumber() {
            return modelNumber;
        }
    }

    //  </editor-fold>

}
