package com.megacast.connectsdkwrapper.internal.managers;

import com.megacast.connectsdkwrapper.internal.model.TvDevice;
import com.megacast.castsdk.providers.managers.cast.DeviceConnectionManager;
import com.megacast.castsdk.model.Device;



/**
 * Created by Dmitry on 14.09.16.
 */
public class DeviceConnectionManagerImpl implements DeviceConnectionManager {

    //  <editor-fold desc="public interface">

    @Override
    public void connect( Device device) throws IllegalArgumentException {
        if (!(device instanceof TvDevice)) {
            throw new IllegalArgumentException("Wrong device type!");
        }

        TvDevice deviceWrapper = (TvDevice) device;

        deviceWrapper.connect();
    }

    @Override
    public void disconnect( Device device) throws IllegalArgumentException {
        if (!(device instanceof TvDevice)) {
            throw new IllegalArgumentException("Wrong device type!");
        }

        TvDevice deviceWrapper = (TvDevice) device;

        deviceWrapper.disconnect();
    }

    //  </editor-fold>
}
