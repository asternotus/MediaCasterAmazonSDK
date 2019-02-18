package com.megacast.castsdk.providers.managers.cast;

import com.megacast.castsdk.model.Device;



/**
 * Created by Dmitry on 14.09.16.
 */
public interface DeviceConnectionManager {

    void connect( Device device) throws IllegalArgumentException;

    void disconnect( Device device) throws IllegalArgumentException;

}
