package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.managers.chromecast.ChromeDeviceDiscoveryManager;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ReceiverApplicationInfo;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;



/**
 * Created by Dmitry on 27.09.16.
 */
public class ChromecastProvider extends ConnectSdkProvider {

    private DeviceDiscoveryManagerImpl deviceDiscoveryManager;


    public ChromecastProvider( Context context, String remoteAppId) throws IllegalArgumentException {
        super(context);
        deviceDiscoveryManager = new ChromeDeviceDiscoveryManager(context, remoteAppId, getRemoteApplicationManager());
    }

    @Override
    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return deviceDiscoveryManager;
    }

    @Override
    public int[] getSupportedDevices() {
        return new int[]{Device.CODE_CHROMECAST};
    }


}
