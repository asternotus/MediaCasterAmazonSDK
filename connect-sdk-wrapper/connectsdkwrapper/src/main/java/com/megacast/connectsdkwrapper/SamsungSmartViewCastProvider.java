package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.samsung.SamsungSmartViewDeviceDiscoveryManager;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungSmartViewCastProvider extends ConnectSdkProvider {

    private DeviceDiscoveryManager deviceDiscoveryManager;

    public SamsungSmartViewCastProvider(Context context, String tizenChannelId, String tizenAppId, String orsayAppId, String receiverVersion, String appType) throws IllegalArgumentException {
        super(context);
        deviceDiscoveryManager = new SamsungSmartViewDeviceDiscoveryManager(context, tizenChannelId, tizenAppId, orsayAppId, receiverVersion, appType);
    }

    @Override
    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return deviceDiscoveryManager;
    }

    @Override
    public int[] getSupportedDevices() {
        return new int[]{Device.CODE_SAMSUNG_SMART_VIEW};
    }
}