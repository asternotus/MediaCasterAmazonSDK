package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.managers.roku.RokuDeviceDiscoveryManager;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;



/**
 * Created by Dmitry on 05.10.16.
 */

public class RokuCastProvider extends ConnectSdkProvider {

    private DeviceDiscoveryManager deviceDiscoveryManager;

    public RokuCastProvider( Context context, String appReceiverId, String clientVersion, String appType) throws IllegalArgumentException {
        super(context);
        deviceDiscoveryManager = new RokuDeviceDiscoveryManager(context, appReceiverId, clientVersion, appType, getRemoteApplicationManager());
    }

    @Override
    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return deviceDiscoveryManager;
    }

    @Override
    public int[] getSupportedDevices() {
        return new int[]{Device.CODE_ROKU};
    }


}