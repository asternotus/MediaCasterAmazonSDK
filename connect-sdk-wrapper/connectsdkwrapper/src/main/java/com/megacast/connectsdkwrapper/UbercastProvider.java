package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.amazon.CombinedFireTvDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.chromecast.ChromeDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.roku.RokuDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.samsung.SamsungUnifiedDeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.ubercast.DLNADeviceDiscoveryManager;
import com.megacast.connectsdkwrapper.internal.managers.ubercast.UbercastDeviceDiscoveryManager;

/**
 * Created by Дима on 26.02.2018.
 */

public class UbercastProvider extends ConnectSdkProvider {

    private final ChromeDeviceDiscoveryManager chromeDeviceDiscoveryManager;
    private final SamsungUnifiedDeviceDiscoveryManager samsungUnifiedDeviceDiscoveryManager;
    private final RokuDeviceDiscoveryManager rokuDeviceDiscoveryManager;
    private final CombinedFireTvDeviceDiscoveryManager combinedFireTvDiscoveryManager;
    private final DLNADeviceDiscoveryManager dlnaDeviceDiscoveryManager;

    private DeviceDiscoveryManager deviceDiscoveryManager;

    public UbercastProvider(Context context,
                            String samsungReceiverVersion,
                            String rokuReceiverVersion,
                            String chromecastRemoteAppId,
                            String samsungOrsayAppId, String samsungTizenAppId, String samsungChannelId,
                            String rokuAppId,
                            String fireTvAppId, String fireTvAppAsin,
                            String appType) {
        super(context);

        dlnaDeviceDiscoveryManager = new DLNADeviceDiscoveryManager(context);

        chromeDeviceDiscoveryManager
                = new ChromeDeviceDiscoveryManager(context, chromecastRemoteAppId, getRemoteApplicationManager());

        samsungUnifiedDeviceDiscoveryManager
                = new SamsungUnifiedDeviceDiscoveryManager(context, samsungOrsayAppId, samsungReceiverVersion,
                samsungChannelId, samsungTizenAppId, samsungReceiverVersion, appType);

        rokuDeviceDiscoveryManager =
                new RokuDeviceDiscoveryManager(context, rokuAppId, rokuReceiverVersion, appType, getRemoteApplicationManager());

        combinedFireTvDiscoveryManager
                = new CombinedFireTvDeviceDiscoveryManager(context, fireTvAppId, fireTvAppAsin, getRemoteApplicationManager());

        deviceDiscoveryManager = new UbercastDeviceDiscoveryManager(context,
                chromeDeviceDiscoveryManager,
                samsungUnifiedDeviceDiscoveryManager,
                rokuDeviceDiscoveryManager,
                combinedFireTvDiscoveryManager,
                dlnaDeviceDiscoveryManager);
    }

    @Override
    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return deviceDiscoveryManager;
    }

    @Override
    public int[] getSupportedDevices() {
        return new int[]{Device.CODE_AMAZON_FIRE, Device.CODE_CHROMECAST, Device.CODE_ROKU,
                Device.CODE_SAMSUNG_LEGACY_2014, Device.CODE_SAMSUNG_ORSAY, Device.CODE_SAMSUNG_SMART_VIEW
        };
    }
}
