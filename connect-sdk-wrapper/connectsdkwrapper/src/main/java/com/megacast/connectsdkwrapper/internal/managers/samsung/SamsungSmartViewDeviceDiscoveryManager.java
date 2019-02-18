package com.megacast.connectsdkwrapper.internal.managers.samsung;

import android.content.Context;

import com.connectsdk.discovery.DiscoveryManager;
import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.SamsungSmartViewService;
import com.connectsdk.service.capability.MediaPlayer;
import com.megacast.castsdk.model.Device;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.model.SamsungOrsayDevice;
import com.megacast.connectsdkwrapper.internal.model.SamsungTizenDevice;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungSmartViewDeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = SamsungSmartViewDeviceDiscoveryManager.class.getSimpleName();

    private final String tizenAppId;
    private final String channelId;
    private final String orsayAppId;
    private final String appType;
    private String version;

    public SamsungSmartViewDeviceDiscoveryManager(Context context, String channelId,
                                                  String tizenAppId, String orsayAppId,
                                                  String version, String appType) {
        super(context, Device.CODE_SAMSUNG_SMART_VIEW);

        SmartLog.d(LOG_TAG, "init manager ");

        this.tizenAppId = tizenAppId;
        this.orsayAppId = orsayAppId;
        this.version = version;
        this.channelId = channelId;
        this.appType = appType;

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
        updateDeviceInfo(device);
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "onDeviceUpdated ");
        super.onDeviceUpdated(manager, device);
        updateDeviceInfo(device);
    }

    @Override
    protected TvDevice createDevice(ConnectableDevice device) {
        MediaPlayer mediaPlayer = device.getMediaPlayer();
        boolean legacyDevice = mediaPlayer != null && mediaPlayer instanceof SamsungSmartViewService && ((SamsungSmartViewService) mediaPlayer).isLegacyDevice();
        SmartLog.d(LOG_TAG, "createDevice, is legacy: " + legacyDevice);
        if (legacyDevice) {
            return new SamsungOrsayDevice(device);
        }
        return new SamsungTizenDevice(device);
    }

    @Override
    protected void updateDeviceService(ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "updateDeviceService " + version);
        super.updateDeviceService(device);
        updateDeviceInfo(device);
    }

    private void updateDeviceInfo(ConnectableDevice device) {
        final MediaPlayer mediaPlayer = device.getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer instanceof SamsungSmartViewService) {
            ((SamsungSmartViewService) mediaPlayer).setTizenAppId(tizenAppId);
            ((SamsungSmartViewService) mediaPlayer).setOrsayAppId(orsayAppId);
            ((SamsungSmartViewService) mediaPlayer).setClientVersion(version);
            ((SamsungSmartViewService) mediaPlayer).setClientAppType(appType);
            ((SamsungSmartViewService) mediaPlayer).setChannelId(channelId);
        }
    }

    private boolean isSamsungDevice(ConnectableDevice device) {
        return isMediaCompatible(device);
    }

}