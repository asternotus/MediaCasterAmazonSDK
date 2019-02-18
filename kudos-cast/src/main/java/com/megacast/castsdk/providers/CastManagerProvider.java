package com.megacast.castsdk.providers;

import com.megacast.castsdk.providers.managers.cast.DeviceConnectionManager;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.castsdk.providers.managers.cast.ImageCastManager;
import com.megacast.castsdk.providers.managers.cast.MediaCastManager;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;

/**
 * Created by Dmitry on 14.09.16.
 */
public interface CastManagerProvider {

    DeviceDiscoveryManager getDeviceDiscoveryManager();

    ImageCastManager getImageCastManager();

    MediaCastManager getMediaCastManager();

    DeviceConnectionManager getDeviceConnectionManager();

    RemoteApplicationManager getRemoteApplicationManager();

    int[] getSupportedDevices();

}
