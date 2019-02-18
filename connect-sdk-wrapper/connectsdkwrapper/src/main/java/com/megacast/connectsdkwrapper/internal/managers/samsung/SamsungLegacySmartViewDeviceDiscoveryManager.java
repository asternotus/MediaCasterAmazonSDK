package com.megacast.connectsdkwrapper.internal.managers.samsung;

import android.content.Context;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungConvergenceDiscoveryProvider;
import com.connectsdk.discovery.provider.SamsungLegacySmartViewDiscoveryProvider;
import com.connectsdk.service.SamsungConvergenceService;
import com.connectsdk.service.SamsungLegacySmartViewService;
import com.connectsdk.service.capability.MediaPlayer;
import com.megacast.castsdk.model.Device;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.model.SamsungOrsayDevice;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;

import java.util.List;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungLegacySmartViewDeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = SamsungLegacySmartViewDeviceDiscoveryManager.class.getSimpleName();

    private final String appId;
    private final String version;
    private final String channelID;

    public SamsungLegacySmartViewDeviceDiscoveryManager(Context context, String appId, String version, String channelID) {
        super(context, Device.CODE_SAMSUNG_LEGACY_2014);

        this.appId = appId;
        this.version = version;
        this.channelID = channelID;

        this.setDeviceFilter(new DeviceFilter() {
            @Override
            public boolean acceptDevice(ConnectableDevice device) {
                return isMediaCompatible(device);
            }
        });
    }

    @Override
    protected TvDevice createDevice(ConnectableDevice device) {
        return new SamsungOrsayDevice(device);
    }

    @Override
    protected void updateDeviceService(ConnectableDevice device) {
        super.updateDeviceService(device);
        final MediaPlayer mediaPlayer = device.getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer instanceof SamsungLegacySmartViewService) {
            ((SamsungLegacySmartViewService) mediaPlayer).setAppId(appId);
            ((SamsungLegacySmartViewService) mediaPlayer).setChannelId(channelID);
            ((SamsungLegacySmartViewService) mediaPlayer).setClientVersion(version);
        }
    }

    private boolean isSamsungDevice(ConnectableDevice device) {
        return isMediaCompatible(device);
    }

}