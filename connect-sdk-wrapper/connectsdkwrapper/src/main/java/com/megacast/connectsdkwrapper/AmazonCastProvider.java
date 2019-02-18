package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.megacast.connectsdkwrapper.internal.managers.amazon.AmazonDeviceDiscoveryManager;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.amazon.CombinedFireTvDeviceDiscoveryManager;



/**
 * Created by Dmitry on 13.09.2016.
 */
public class AmazonCastProvider extends ConnectSdkProvider {

    private DeviceDiscoveryManager deviceDiscoveryManager;

    public AmazonCastProvider( Context context, String receiverAppId, String asin) throws IllegalArgumentException {
        super(context);
        deviceDiscoveryManager = new CombinedFireTvDeviceDiscoveryManager(context, receiverAppId, asin, getRemoteApplicationManager());
    }

    @Override
    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return deviceDiscoveryManager;
    }

    @Override
    public int[] getSupportedDevices() {
        return new int[]{Device.CODE_AMAZON_FIRE};
    }

}
