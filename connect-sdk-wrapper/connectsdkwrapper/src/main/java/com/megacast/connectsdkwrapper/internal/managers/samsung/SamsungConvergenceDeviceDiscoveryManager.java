package com.megacast.connectsdkwrapper.internal.managers.samsung;

import android.content.Context;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungConvergenceDiscoveryProvider;
import com.connectsdk.service.SamsungConvergenceService;
import com.connectsdk.service.capability.MediaPlayer;
import com.mega.cast.utils.log.SmartLog;
import com.megacast.castsdk.model.Device;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.model.SamsungOrsayDevice;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;

import java.util.List;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungConvergenceDeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = SamsungConvergenceDeviceDiscoveryManager.class.getSimpleName();

    private final String appId;
    private final String appType;
    private String version;

    public SamsungConvergenceDeviceDiscoveryManager(Context context, String appId, String version, String appType) {
        super(context, Device.CODE_SAMSUNG_ORSAY);

        this.appId = appId;
        this.version = version;
        this.appType = appType;

        setRemotePlayerId(appId);

        this.setDeviceFilter(new DeviceFilter() {
            @Override
            public boolean acceptDevice(ConnectableDevice device) {
                return isMediaCompatible(device);
            }
        });
    }

    @Override
    public void setVersion(int deviceCode, String version) {
        super.setVersion(deviceCode, version);
        this.version = version;
    }

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "onDeviceAdded ");
        super.onDeviceAdded(manager, device);
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "onDeviceUpdated ");
        super.onDeviceUpdated(manager, device);
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "onDeviceRemoved ");
        super.onDeviceRemoved(manager, device);
    }

    @Override
    protected TvDevice createDevice(ConnectableDevice device) {
        return new SamsungOrsayDevice(device);
    }

    @Override
    protected void updateDeviceService(ConnectableDevice device) {
        super.updateDeviceService(device);
        final MediaPlayer mediaPlayer = device.getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer instanceof SamsungConvergenceService) {
            ((SamsungConvergenceService) mediaPlayer).setAppId(appId);
            ((SamsungConvergenceService) mediaPlayer).setClientVersion(version);
            ((SamsungConvergenceService) mediaPlayer).setClientAppType(appType);
        }
    }

    private void setRemotePlayerId(String remoteAppId) {
        getSamsungConvergenceDiscoveryProvider().setPlayerId(remoteAppId);
    }

    private SamsungConvergenceDiscoveryProvider getSamsungConvergenceDiscoveryProvider() {
        List<DiscoveryProvider> providers = discoveryManager.getDiscoveryProviders();
        for (DiscoveryProvider provider : providers) {
            if (provider instanceof SamsungConvergenceDiscoveryProvider) {
                return ((SamsungConvergenceDiscoveryProvider) provider);
            }
        }
        return null;
    }

    private boolean isSamsungDevice(ConnectableDevice device) {
        return isMediaCompatible(device);
    }

}