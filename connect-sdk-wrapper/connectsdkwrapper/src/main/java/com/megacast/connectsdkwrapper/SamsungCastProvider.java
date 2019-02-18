package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.samsung.SamsungUnifiedDeviceDiscoveryManager;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungCastProvider extends ConnectSdkProvider {

    private DeviceDiscoveryManager deviceDiscoveryManager;

    public SamsungCastProvider(Context context, String orsayAppId, String orsayReceiverVersion,
                               String tizenChannelId, String tizenAppId, String tizenReceiverVersion, String appType) throws IllegalArgumentException {
        super(context);
        deviceDiscoveryManager = new SamsungUnifiedDeviceDiscoveryManager(context, orsayAppId, orsayReceiverVersion,
                tizenChannelId, tizenAppId, tizenReceiverVersion, appType);
    }

    @Override
    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return deviceDiscoveryManager;
    }

    @Override
    public int[] getSupportedDevices() {
        return new int[]{Device.CODE_SAMSUNG_SMART_VIEW, Device.CODE_SAMSUNG_ORSAY};
    }
}