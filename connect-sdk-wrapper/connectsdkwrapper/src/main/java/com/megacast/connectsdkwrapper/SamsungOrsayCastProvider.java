package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.managers.samsung.SamsungConvergenceDeviceDiscoveryManager;



/**
 * Created by Dmitry on 28.03.17.
 */

public class SamsungOrsayCastProvider extends ConnectSdkProvider {

    private DeviceDiscoveryManagerImpl deviceDiscoveryManager;

    public SamsungOrsayCastProvider( Context context, String appId, String version, String appType) throws IllegalArgumentException {
        super(context);
        deviceDiscoveryManager = new SamsungConvergenceDeviceDiscoveryManager(context, appId, version, appType);
    }

    @Override
    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return deviceDiscoveryManager;
    }

    @Override
    public int[] getSupportedDevices() {
        return new int[]{Device.CODE_SAMSUNG_ORSAY};
    }

}
