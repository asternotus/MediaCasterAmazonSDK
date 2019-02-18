package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.connectsdk.discovery.provider.CastDiscoveryProvider;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.chromecast.ChromeDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.ubercast.DLNADeviceDiscoveryManager;



/**
 * Created by Дима on 06.03.2018.
 */

public class DlnaProvider extends ConnectSdkProvider {

    private DeviceDiscoveryManager deviceDiscoveryManager;

    public DlnaProvider( Context context) throws IllegalArgumentException {
        super(context);
        deviceDiscoveryManager = new DLNADeviceDiscoveryManager(context);
//        deviceDiscoveryManager = new ChromeDeviceDiscoveryManager(context,
//                CastDiscoveryProvider.CATEGORY_REMOTE_PLAYBACK,
//                getRemoteApplicationManager());
    }

    @Override
    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return deviceDiscoveryManager;
    }

    @Override
    public int[] getSupportedDevices() {
        return new int[]{Device.CODE_DLNA};
    }

}