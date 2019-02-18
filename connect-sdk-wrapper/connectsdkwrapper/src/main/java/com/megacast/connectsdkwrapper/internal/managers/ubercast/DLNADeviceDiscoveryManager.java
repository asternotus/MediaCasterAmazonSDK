package com.megacast.connectsdkwrapper.internal.managers.ubercast;

import android.content.Context;
import android.text.TextUtils;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.config.ServiceDescription;
import com.mega.cast.utils.log.SmartLog;
import com.megacast.castsdk.model.Device;
import com.megacast.connectsdkwrapper.internal.managers.DeviceDiscoveryManagerImpl;
import com.megacast.connectsdkwrapper.internal.model.DlnaDevice;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;

import java.util.Collection;

/**
 * Created by Дима on 06.03.2018.
 */

public class DLNADeviceDiscoveryManager extends DeviceDiscoveryManagerImpl {

    private static final String LOG_TAG = DLNADeviceDiscoveryManager.class.getSimpleName();

    public DLNADeviceDiscoveryManager(Context context) {
        super(context, Device.CODE_DLNA);
    }

    @Override
    protected TvDevice createDevice(ConnectableDevice device) {
        SmartLog.d(LOG_TAG, "createDevice");
        DlnaDevice dlnaDevice = new DlnaDevice(device);
        Collection<DeviceService> services = device.getServices();
        boolean isSamsung = false;
        if (services != null) {
            for (DeviceService service : services) {
                ServiceDescription serviceDescription = service.getServiceDescription();
                if (serviceDescription != null) {
                    if (checkForSamsung(serviceDescription.getFriendlyName())
                            || checkForSamsung(serviceDescription.getModelDescription())
                            || checkForSamsung(serviceDescription.getManufacturer())
                            || checkForSamsung(serviceDescription.getModelName())
                            || checkForSamsung(serviceDescription.getModelNumber())) {
                        isSamsung = true;
                    }
                }
            }
        }
        SmartLog.d(LOG_TAG, "Is samsung device? " + isSamsung);
        dlnaDevice.setSamsungTVFlag(isSamsung);
        return dlnaDevice;
    }

    private boolean checkForSamsung(String str) {
        return !TextUtils.isEmpty(str) && str.contains("Samsung");
    }

}
