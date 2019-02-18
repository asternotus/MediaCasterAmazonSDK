package com.megacast.connectsdkwrapper.internal.managers.chromecast;

import android.content.Context;

import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.provider.CastDiscoveryProvider;
import com.connectsdk.service.CastService;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.command.ServiceCommandError;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.model.ChromecastDevice;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;
import com.megacast.castsdk.model.ApplicationSession;
import com.megacast.castsdk.model.ApplicationState;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ReceiverApplicationInfo;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;

import java.util.List;

import rx.Subscriber;

/**
 * Created by Dmitry on 16.09.16.
 */
public class ChromeDeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = ChromeDeviceDiscoveryManager.class.getSimpleName();
    private final RemoteApplicationManager remoteApplicationManager;
    private String remoteAppId;

    public ChromeDeviceDiscoveryManager(Context context, String remoteAppId,
                                        RemoteApplicationManager remoteApplicationManager) {
        super(context, Device.CODE_CHROMECAST);
        this.remoteApplicationManager = remoteApplicationManager;

        setDeviceFilter(new DeviceFilter() {
            @Override
            public boolean acceptDevice(ConnectableDevice device) {
                return isMediaCompatible(device);
            }
        });

        setRemotePlayerId(remoteAppId);
    }

    @Override
    public void setVersion(int deviceCode, String version) {
        super.setVersion(deviceCode, version);
        setRemotePlayerId(remoteAppId);
    }

    private void setRemotePlayerId(String remoteAppId) {
        this.remoteAppId = remoteAppId;
        List<DiscoveryProvider> providers = discoveryManager.getDiscoveryProviders();
        for (DiscoveryProvider provider : providers) {
            if (provider instanceof CastDiscoveryProvider) {
                ((CastDiscoveryProvider) provider).setApplicationId(remoteAppId);
            }
        }
    }

    //  <editor-fold desc="device listener">

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.d(LOG_TAG, String.format("onDeviceAdded\n" +
                        "name: %s\n" +
                        "model: %s",
                device.getFriendlyName(), device.getModelName()));
        updateCastServiceAppInfo(device);
        super.onDeviceAdded(manager, device);
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "onDeviceUpdated");
        updateCastServiceAppInfo(device);
        super.onDeviceUpdated(manager, device);
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "onDeviceRemoved");
        super.onDeviceRemoved(manager, device);
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        SmartLog.d(LOG_TAG, "onDiscoveryFailed ");
        super.onDiscoveryFailed(manager, error);
    }

    //  </editor-fold>

    //  <editor-fold desc="protected">

    @Override
    protected TvDevice createDevice(ConnectableDevice device) {
        return new ChromecastDevice(device);
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private void updateCastServiceAppInfo(ConnectableDevice device) {
        for (DeviceService service : device.getServices()) {
            if (service instanceof CastService) {
                SmartLog.d(LOG_TAG, "found Cast Service! ");
                CastService castService = (CastService) service;
                castService.setApplicationID(remoteAppId);
            }
        }
    }

    //  </editor-fold>

}
