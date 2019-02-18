package com.megacast.connectsdkwrapper.internal.managers.ubercast;

import android.content.Context;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.provider.CastDiscoveryProvider;
import com.connectsdk.discovery.provider.FireTVDiscoveryProvider;
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungConvergenceDiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungLegacySmartViewDiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungSmartViewDiscoveryProvider;
import com.connectsdk.service.CastService;
import com.connectsdk.service.DIALService;
import com.connectsdk.service.DLNAService;
import com.connectsdk.service.FireTVService;
import com.connectsdk.service.RokuService;
import com.connectsdk.service.SamsungConvergenceService;
import com.connectsdk.service.SamsungLegacySmartViewService;
import com.connectsdk.service.SamsungSmartViewService;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.mega.cast.utils.log.SmartLog;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ReceiverInfo;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.managers.amazon.CombinedFireTvDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.chromecast.ChromeDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.roku.RokuDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.samsung.SamsungUnifiedDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.model.AmazonDevice;
import com.megacast.connectsdkwrapper.internal.model.ChromecastDevice;
import com.megacast.connectsdkwrapper.internal.model.DlnaDevice;
import com.megacast.connectsdkwrapper.internal.model.RokuDevice;
import com.megacast.connectsdkwrapper.internal.model.SamsungOrsayDevice;
import com.megacast.connectsdkwrapper.internal.model.SamsungTizenDevice;
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

public class UbercastDeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = UbercastDeviceDiscoveryManager.class.getSimpleName();

    private final ChromeDeviceDiscoveryManager chromeDeviceDiscoveryManager;
    private final SamsungUnifiedDeviceDiscoveryManager samsungUnifiedDeviceDiscoveryManager;
    private final RokuDeviceDiscoveryManager rokuDeviceDiscoveryManager;
    private final CombinedFireTvDeviceDiscoveryManager combinedFireTvDiscoveryManager;
    private final DLNADeviceDiscoveryManager dlnaDeviceDiscoveryManager;

    private List<Device> chromeDevices = new ArrayList<>();
    private List<Device> samsungDevices = new ArrayList<>();
    private List<Device> rokuDevices = new ArrayList<>();
    private List<Device> fireTvDevices = new ArrayList<>();
    private List<Device> dlnaDevices = new ArrayList<>();

    private PublishSubject<List<Device>> devicePublisher = PublishSubject.create();

    private Subscription chromeSubscription;
    private Subscription samsungSubscription;
    private Subscription rokuSubscription;
    private Subscription fireTvSubscription;
    private Subscription dlnaSubscription;

    public UbercastDeviceDiscoveryManager(Context context,
                                          ChromeDeviceDiscoveryManager chromeDeviceDiscoveryManager,
                                          SamsungUnifiedDeviceDiscoveryManager samsungUnifiedDeviceDiscoveryManager,
                                          RokuDeviceDiscoveryManager rokuDeviceDiscoveryManager,
                                          CombinedFireTvDeviceDiscoveryManager combinedFireTvDiscoveryManager,
                                          DLNADeviceDiscoveryManager dlnaDeviceDiscoveryManager) {
        super(context, null);

        this.chromeDeviceDiscoveryManager = chromeDeviceDiscoveryManager;
        this.samsungUnifiedDeviceDiscoveryManager = samsungUnifiedDeviceDiscoveryManager;
        this.rokuDeviceDiscoveryManager = rokuDeviceDiscoveryManager;
        this.combinedFireTvDiscoveryManager = combinedFireTvDiscoveryManager;
        this.dlnaDeviceDiscoveryManager = dlnaDeviceDiscoveryManager;
    }

    @Override
    public void discoverAvailableDevices() {
        SmartLog.t(LOG_TAG, "discoverAvailableDevices");

        chromeSubscription = chromeDeviceDiscoveryManager.getDevicesObservable()
                .subscribe(new Subscriber<List<Device>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
//                        devicePublisher.onError(e);
                    }

                    @Override
                    public void onNext(List<Device> devices) {
                        SmartLog.t(LOG_TAG, "got chrome devices " + devices.size());
                        chromeDevices = devices;
                        updateDevices();
                    }
                });

        samsungSubscription = samsungUnifiedDeviceDiscoveryManager.getDevicesObservable()
                .subscribe(new Subscriber<List<Device>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
//                        devicePublisher.onError(e);
                    }

                    @Override
                    public void onNext(List<Device> devices) {
                        SmartLog.t(LOG_TAG, "got samsung devices " + devices.size());
                        samsungDevices = devices;
                        updateDevices();
                    }
                });

        rokuSubscription = rokuDeviceDiscoveryManager.getDevicesObservable()
                .subscribe(new Subscriber<List<Device>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
//                        devicePublisher.onError(e);
                    }

                    @Override
                    public void onNext(List<Device> devices) {
                        SmartLog.t(LOG_TAG, "got roku devices " + devices.size());
                        rokuDevices = devices;
                        updateDevices();
                    }
                });


        fireTvSubscription = combinedFireTvDiscoveryManager.getDevicesObservable()
                .subscribe(new Subscriber<List<Device>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
//                        devicePublisher.onError(e);
                    }

                    @Override
                    public void onNext(List<Device> devices) {
                        SmartLog.t(LOG_TAG, "got fire tv devices " + devices.size());
                        fireTvDevices = devices;
                        updateDevices();
                    }
                });

        dlnaSubscription = dlnaDeviceDiscoveryManager.getDevicesObservable()
                .subscribe(new Subscriber<List<Device>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
//                        devicePublisher.onError(e);
                    }

                    @Override
                    public void onNext(List<Device> devices) {
                        SmartLog.t(LOG_TAG, "got dlna devices " + devices.size());
                        dlnaDevices = devices;
                        updateDevices();
                    }
                });


        chromeDeviceDiscoveryManager.discoverAvailableDevices();
        samsungUnifiedDeviceDiscoveryManager.discoverAvailableDevices();
        rokuDeviceDiscoveryManager.discoverAvailableDevices();
        combinedFireTvDiscoveryManager.discoverAvailableDevices();
        dlnaDeviceDiscoveryManager.discoverAvailableDevices();
    }

    @Override
    public void stopDiscovery() {
        SmartLog.t(LOG_TAG, "stopDiscovery ");

        chromeDeviceDiscoveryManager.stopDiscovery();
        samsungUnifiedDeviceDiscoveryManager.stopDiscovery();
        rokuDeviceDiscoveryManager.stopDiscovery();
        combinedFireTvDiscoveryManager.stopDiscovery();
        dlnaDeviceDiscoveryManager.stopDiscovery();

        if (chromeSubscription != null && !chromeSubscription.isUnsubscribed()) {
            chromeSubscription.unsubscribe();
        }
        if (samsungSubscription != null && !samsungSubscription.isUnsubscribed()) {
            samsungSubscription.unsubscribe();
        }
        if (rokuSubscription != null && !rokuSubscription.isUnsubscribed()) {
            rokuSubscription.unsubscribe();
        }
        if (fireTvSubscription != null && !fireTvSubscription.isUnsubscribed()) {
            fireTvSubscription.unsubscribe();
        }
        if (dlnaSubscription != null && !dlnaSubscription.isUnsubscribed()) {
            dlnaSubscription.unsubscribe();
        }

    }

    @Override
    public Observable<List<Device>> getDevicesObservable() {
        return devicePublisher.asObservable();
    }

    @Override
    public boolean isDiscoveryInProgress() {
        return chromeDeviceDiscoveryManager.isDiscoveryInProgress()
                && samsungUnifiedDeviceDiscoveryManager.isDiscoveryInProgress()
                && rokuDeviceDiscoveryManager.isDiscoveryInProgress()
                && combinedFireTvDiscoveryManager.isDiscoveryInProgress()
                && dlnaDeviceDiscoveryManager.isDiscoveryInProgress();
    }

    @Override
    public boolean isReceiverRequired(Device device) {
        if (device instanceof AmazonDevice) {
            return combinedFireTvDiscoveryManager.isReceiverRequired();
        } else if (device instanceof SamsungTizenDevice || device instanceof SamsungOrsayDevice) {
            return samsungUnifiedDeviceDiscoveryManager.isReceiverRequired();
        } else if (device instanceof ChromecastDevice) {
            return chromeDeviceDiscoveryManager.isReceiverRequired();
        } else if (device instanceof RokuDevice) {
            return rokuDeviceDiscoveryManager.isReceiverRequired();
        } else if (device instanceof DlnaDevice) {
            return dlnaDeviceDiscoveryManager.isReceiverRequired();
        }
        return false;
    }

    @Override
    public Observable<Void> installReceiver(Device device) {
        if (device instanceof AmazonDevice) {
            return combinedFireTvDiscoveryManager.installReceiver(device);
        } else if (device instanceof SamsungTizenDevice || device instanceof SamsungOrsayDevice) {
            return samsungUnifiedDeviceDiscoveryManager.installReceiver(device);
        } else if (device instanceof ChromecastDevice) {
            return chromeDeviceDiscoveryManager.installReceiver(device);
        } else if (device instanceof RokuDevice) {
            return rokuDeviceDiscoveryManager.installReceiver(device);
        } else if (device instanceof DlnaDevice) {
            return dlnaDeviceDiscoveryManager.installReceiver(device);
        }
        return Observable.error(new Throwable("Unknown device error"));
    }

    @Override
    public Observable<ReceiverInfo> showReceiver(final Device device, String receiverLaunchParams) {
        if (device instanceof AmazonDevice) {
            return combinedFireTvDiscoveryManager.showReceiver(device, receiverLaunchParams);
        } else if (device instanceof SamsungTizenDevice || device instanceof SamsungOrsayDevice) {
            return samsungUnifiedDeviceDiscoveryManager.showReceiver(device, receiverLaunchParams);
        } else if (device instanceof ChromecastDevice) {
            return chromeDeviceDiscoveryManager.showReceiver(device, receiverLaunchParams);
        } else if (device instanceof RokuDevice) {
            return rokuDeviceDiscoveryManager.showReceiver(device, receiverLaunchParams);
        } else if (device instanceof DlnaDevice) {
            return dlnaDeviceDiscoveryManager.showReceiver(device, receiverLaunchParams);
        }
        return Observable.error(new Throwable("Unknown device error"));
    }

    @Override
    public Observable<Void> closeReceiver(final Device device) {
        if (device instanceof AmazonDevice) {
            return combinedFireTvDiscoveryManager.closeReceiver(device);
        } else if (device instanceof SamsungTizenDevice || device instanceof SamsungOrsayDevice) {
            return samsungUnifiedDeviceDiscoveryManager.closeReceiver(device);
        } else if (device instanceof ChromecastDevice) {
            return chromeDeviceDiscoveryManager.closeReceiver(device);
        } else if (device instanceof RokuDevice) {
            return rokuDeviceDiscoveryManager.closeReceiver(device);
        } else if (device instanceof DlnaDevice) {
            return dlnaDeviceDiscoveryManager.closeReceiver(device);
        }
        return Observable.error(new Throwable("Unknown device error"));
    }

    @Override
    public Observable<ReceiverInfo> getReceiverState(Device device) {
        if (device instanceof AmazonDevice) {
            return combinedFireTvDiscoveryManager.getReceiverState(device);
        } else if (device instanceof SamsungTizenDevice || device instanceof SamsungOrsayDevice) {
            return samsungUnifiedDeviceDiscoveryManager.getReceiverState(device);
        } else if (device instanceof ChromecastDevice) {
            return chromeDeviceDiscoveryManager.getReceiverState(device);
        } else if (device instanceof RokuDevice) {
            return rokuDeviceDiscoveryManager.getReceiverState(device);
        } else if (device instanceof DlnaDevice) {
            return dlnaDeviceDiscoveryManager.getReceiverState(device);
        }
        return Observable.error(new Throwable("Unknown device error"));
    }

    @Override
    public void disableInstallService() {
        super.disableInstallService();
        combinedFireTvDiscoveryManager.disableInstallService();
        samsungUnifiedDeviceDiscoveryManager.disableInstallService();
        chromeDeviceDiscoveryManager.disableInstallService();
        rokuDeviceDiscoveryManager.disableInstallService();
        dlnaDeviceDiscoveryManager.disableInstallService();
    }

    @Override
    public void setVersion(int deviceCode, String version) {
        switch (deviceCode) {
            case Device.CODE_CHROMECAST:
                chromeDeviceDiscoveryManager.setVersion(Device.CODE_CHROMECAST, version);
                break;
            case Device.CODE_AMAZON_FIRE:
                combinedFireTvDiscoveryManager.setVersion(Device.CODE_AMAZON_FIRE, version);
                break;
            case Device.CODE_ROKU:
                rokuDeviceDiscoveryManager.setVersion(Device.CODE_ROKU, version);
                break;
            case Device.CODE_SAMSUNG_SMART_VIEW:
                samsungUnifiedDeviceDiscoveryManager.setVersion(Device.CODE_SAMSUNG_SMART_VIEW, version);
                break;
            case Device.CODE_SAMSUNG_ORSAY:
                samsungUnifiedDeviceDiscoveryManager.setVersion(Device.CODE_SAMSUNG_ORSAY, version);
                break;
            case Device.CODE_SAMSUNG_LEGACY_2014:
                samsungUnifiedDeviceDiscoveryManager.setVersion(Device.CODE_SAMSUNG_LEGACY_2014, version);
                break;
            case Device.CODE_DLNA:
                dlnaDeviceDiscoveryManager.setVersion(Device.CODE_DLNA, version);
                break;
        }
    }

    private void updateDevices() {
        HashSet<Device> devices = new HashSet<>();

        for (Device device : fireTvDevices) {
            devices.add(device);
        }

        for (Device device : rokuDevices) {
            devices.add(device);
        }

        for (Device device : chromeDevices) {
            devices.add(device);
        }

        for (Device device : samsungDevices) {
            devices.add(device);
        }

        for (Device device : dlnaDevices) {
            SmartLog.d(LOG_TAG, "dlna device " + device.getUniqueIdentifier());

            boolean has_device = false;
            for (Device other_device : devices) {
                SmartLog.d(LOG_TAG, "device " + other_device.getUniqueIdentifier());
                if (other_device.getName().equals(device.getName())
                        && other_device.getIpAdress().contains(device.getIpAdress())) {
                    has_device = true;
                    break;
                }
            }

            if (!has_device) {
                devices.add(device);
            }
        }

        SmartLog.d(LOG_TAG, "publish devices " + devices.size());

        devicePublisher.onNext(new ArrayList<>(devices));
    }
}
