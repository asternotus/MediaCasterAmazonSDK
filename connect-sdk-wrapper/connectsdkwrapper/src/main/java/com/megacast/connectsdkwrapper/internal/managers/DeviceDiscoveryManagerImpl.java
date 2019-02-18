package com.megacast.connectsdkwrapper.internal.managers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.connectsdk.service.DLNAService;
import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.discovery.provider.CastDiscoveryProvider;
import com.connectsdk.discovery.provider.FireTVDiscoveryProvider;
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungConvergenceDiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungSmartViewDiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungLegacySmartViewDiscoveryProvider;
import com.connectsdk.service.CastService;
import com.connectsdk.service.DIALService;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.FireTVService;
import com.connectsdk.service.RokuService;
import com.connectsdk.service.SamsungConvergenceService;
import com.connectsdk.service.SamsungLegacySmartViewService;
import com.connectsdk.service.SamsungSmartViewService;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.command.ServiceCommandError;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;
import com.megacast.castsdk.model.ReceiverInfo;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.castsdk.model.Device;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by Dmitry on 13.09.2016.
 */
public class DeviceDiscoveryManagerImpl implements DeviceDiscoveryManager, DiscoveryManagerListener {

    private static final String LOG_TAG = DeviceDiscoveryManagerImpl.class.getSimpleName();

    private boolean discoveryInProgress;

    private DeviceListDiscoveryListener listener;

    private Map<String, TvDevice> devicesMap;

    private final Context context;

    private final int[] filters;

    private DeviceFilter deviceFilter;

    protected DiscoveryManager discoveryManager;

    private PublishSubject<List<Device>> devicePublisher = PublishSubject.create();

    public DeviceDiscoveryManagerImpl(Context context, int... filters) {
        this.context = context;
        this.filters = filters;

        initDiscoveryManager();

        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                addFilter(filters[i]);
            }
        }

        discoveryInProgress = false;

        devicesMap = new HashMap<>();

        setListener(new DeviceListDiscoveryListener() {
            @Override
            public void onDeviceListUpdated(List<Device> compatibleDevices) {
                devicePublisher.onNext(compatibleDevices);
            }

            @Override
            public void onFail(Throwable cause) {
//                devicePublisher.onError(cause);
            }

        });
    }

    //  <editor-fold desc="public interface">


    @Override
    public void discoverAvailableDevices() {
        SmartLog.d(LOG_TAG, "discoverAvailableDevices ");
        startDiscovery();
    }

    @Override
    public void stopDiscovery() {
        SmartLog.d(LOG_TAG, "stopDiscovery ");
        discoveryManager.stop();
        discoveryInProgress = false;
    }

    @Override
    public Observable<List<Device>> getDevicesObservable() {
        return devicePublisher.asObservable();
    }

    @Override
    public boolean isDiscoveryInProgress() {
        return discoveryInProgress;
    }

    @Override
    public boolean isReceiverRequired() {
        return false;
    }

    @Override
    public boolean isReceiverRequired(Device device) {
        return false;
    }

    @Override
    public Observable<Void> installReceiver() {
        return Observable.error(new Throwable("No receiver required!"));
    }

    @Override
    public Observable<Void> installReceiver(Device device) {
        return Observable.error(new Throwable("No receiver required!"));
    }

    @Override
    public Observable<ReceiverInfo> showReceiver(Device device, String receiverLaunchParams) {
        return Observable.error(new Throwable("No receiver required!"));
    }

    @Override
    public Observable<Void> closeReceiver(Device device) {
        return Observable.error(new Throwable("No receiver required!"));
    }

    @Override
    public Observable<ReceiverInfo> getReceiverState(Device device) {
        return Observable.error(new Throwable("No receiver required!"));
    }

    @Override
    public void disableInstallService() {
        //do nothing
    }

    @Override
    public void setVersion(int deviceCode, String version) {
        //do nothing
    }

    //  </editor-fold>

    //  <editor-fold desc="DiscoveryManagerListener">

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.i(LOG_TAG, String.format("Device %s \nModel %s\n Model Number %s\nIp %s \nID %s",
                device.getFriendlyName(),
                device.getModelName(),
                device.getModelNumber(),
                device.getIpAddress(),
                device.getId()));

        Collection<DeviceService> services = device.getServices();
        for (DeviceService service : services) {
            SmartLog.d(LOG_TAG, "DeviceService  " + service);
        }

        SmartLog.d(LOG_TAG, "Device capabilities: ");
        List<String> capabilities = device.getCapabilities();
        for (String capability : capabilities) {
            SmartLog.d(LOG_TAG, " " + capability);
        }

        if (listener != null) {
            listener.onDeviceListUpdated(getMediaCompatibleDevices());
        }
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.i(LOG_TAG, "onDeviceUpdated ");

        SmartLog.d(LOG_TAG, String.format("Device %s \nModel %s\n Model Number %s\nIp %s \nID %s",
                device.getFriendlyName(),
                device.getModelName(),
                device.getModelNumber(),
                device.getIpAddress(),
                device.getId()));

        Collection<DeviceService> services = device.getServices();
        for (DeviceService service : services) {
            SmartLog.d(LOG_TAG, "DeviceService  " + service);
        }

        SmartLog.d(LOG_TAG, "Device capabilities: ");
        List<String> capabilities = device.getCapabilities();
        for (String capability : capabilities) {
            SmartLog.d(LOG_TAG, " " + capability);
        }

        if (listener != null) {
            listener.onDeviceListUpdated(getMediaCompatibleDevices());
        }
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.i(LOG_TAG, "onDeviceRemoved ");
        if (listener != null) {
            listener.onDeviceListUpdated(getMediaCompatibleDevices());
        }
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        SmartLog.i(LOG_TAG, "onDiscoveryFailed ");
        if (listener != null) {
            listener.onFail(new Throwable(error.getMessage()));
        }
        discoveryInProgress = false;
    }

    //  </editor-fold>

    //  <editor-fold desc="protected">

    protected void setDeviceFilter(DeviceFilter filter) {
        this.deviceFilter = filter;
    }

    @NonNull
    protected final Device getDevice(ConnectableDevice device) {
        SmartLog.i(LOG_TAG, "getDevice ");
        TvDevice deviceWrapper = devicesMap.get(device.getIpAddress());
        if (deviceWrapper == null) {
            deviceWrapper = createDevice(device);
            devicesMap.put(device.getIpAddress(), deviceWrapper);
            SmartLog.d(LOG_TAG, "create new device!");
        } else {
            deviceWrapper.setDevice(device);
            if (deviceWrapper.getConnectableDevice() != device) {
                SmartLog.d(LOG_TAG, "reset device object! " + deviceWrapper.getName());
            }
        }
        updateDeviceService(device);
        return deviceWrapper;
    }

    protected void updateDeviceService(ConnectableDevice device) {
        //override if needed
    }

    protected TvDevice createDevice(ConnectableDevice device) {
        return new TvDevice(device);
    }

    protected boolean isDialCompatible(ConnectableDevice device) {
        for (DeviceService service : device.getServices()) {
            SmartLog.d(LOG_TAG, "DeviceService  " + service);
            if (service instanceof DIALService) {
                return true;
            }
        }
        return false;
    }

    protected boolean isMediaCompatible(ConnectableDevice device) {
        for (DeviceService service : device.getServices()) {
            SmartLog.d(LOG_TAG, "DeviceService  " + service);
            if (service instanceof MediaPlayer) {
                return true;
            }
        }
        return false;
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private void initDiscoveryManager() {
        DiscoveryManager.init(context);
        discoveryManager = DiscoveryManager.getInstance();
        discoveryManager.setPairingLevel(DiscoveryManager.PairingLevel.ON);
        discoveryManager.addListener(this);
    }

    private void startDiscovery() {
        if (discoveryInProgress) {
            SmartLog.d(LOG_TAG, "discovery manager rescan");
            discoveryManager.rescan();
        } else {
            SmartLog.d(LOG_TAG, "discoveryManager started!");
            discoveryManager.start();
        }
        discoveryInProgress = true;
    }

    private void setListener(DeviceListDiscoveryListener listener) {
        this.listener = listener;
    }

    private List<Device> getMediaCompatibleDevices() {
        List<Device> devicesList = new ArrayList<>();

        Map<String, ConnectableDevice> compatibleDevices = discoveryManager.getCompatibleDevices();

        for (String ipAdress : compatibleDevices.keySet()) {
            ConnectableDevice device = compatibleDevices.get(ipAdress);

            if (deviceFilter != null && !deviceFilter.acceptDevice(device)) {
                continue;
            }

            Device deviceWrapper = getDevice(device);
            devicesList.add(deviceWrapper);
        }
        return devicesList;
    }

    private void addFilter(int deviceCode) {
        switch (deviceCode) {
            case Device.CODE_CHROMECAST:
                SmartLog.d(LOG_TAG, "add filter CHROMECAST");
                discoveryManager.registerDeviceService(CastService.class, CastDiscoveryProvider.class);
                discoveryManager.registerDeviceService(DIALService.class, SSDPDiscoveryProvider.class);
                break;
            case Device.CODE_AMAZON_FIRE:
                SmartLog.d(LOG_TAG, "add filter CODE_AMAZON_FIRE");
                discoveryManager.registerDeviceService(FireTVService.class, FireTVDiscoveryProvider.class);
                discoveryManager.registerDeviceService(DIALService.class, SSDPDiscoveryProvider.class);
                break;
            case Device.CODE_ROKU:
                SmartLog.d(LOG_TAG, "add filter CODE_ROKU");
                discoveryManager.registerDeviceService(RokuService.class, SSDPDiscoveryProvider.class);
                break;
            case Device.CODE_SAMSUNG_SMART_VIEW:
                SmartLog.d(LOG_TAG, "add filter CODE_SAMSUNG_SMART_VIEW");
                discoveryManager.registerDeviceService(SamsungSmartViewService.class, SamsungSmartViewDiscoveryProvider.class);
                break;
            case Device.CODE_SAMSUNG_ORSAY:
                SmartLog.d(LOG_TAG, "add filter CODE_SAMSUNG_ORSAY");
                discoveryManager.registerDeviceService(SamsungConvergenceService.class, SamsungConvergenceDiscoveryProvider.class);
                break;
            case Device.CODE_SAMSUNG_LEGACY_2014:
                SmartLog.d(LOG_TAG, "add filter CODE_SAMSUNG_LEGACY_2014");
                discoveryManager.registerDeviceService(SamsungLegacySmartViewService.class, SamsungLegacySmartViewDiscoveryProvider.class);
                break;
            case Device.CODE_DLNA:
                SmartLog.d(LOG_TAG, "add filter CODE_DLNA");
                discoveryManager.registerDeviceService(DLNAService.class, SSDPDiscoveryProvider.class);
                break;
        }
    }

    //  </editor-fold>

    //  <editor-fold desc="DeviceListDiscoveryListener">

    private interface DeviceListDiscoveryListener {

        void onDeviceListUpdated(List<Device> compatibleDevices);

        void onFail(Throwable cause);

    }

    protected interface DeviceFilter {

        boolean acceptDevice(ConnectableDevice device);

    }

    //  </editor-fold>


}
