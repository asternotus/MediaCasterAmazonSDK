package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.managers.samsung.SamsungLegacySmartViewDeviceDiscoveryManager;



/**
 * Created by Dmitry on 27.04.17.
 */

public class SamsungLegacySmartViewProvider extends ConnectSdkProvider {

    private DeviceDiscoveryManagerImpl deviceDiscoveryManager;

    public SamsungLegacySmartViewProvider( Context context, String appId, String version, String channelId) throws IllegalArgumentException {
        super(context);
        deviceDiscoveryManager = new SamsungLegacySmartViewDeviceDiscoveryManager(context, appId, version, channelId);
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
