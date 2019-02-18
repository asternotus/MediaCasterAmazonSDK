package com.megacast.connectsdkwrapper;

import android.content.Context;

import com.connectsdk.core.Util;
import com.megacast.connectsdkwrapper.internal.managers.DeviceConnectionManagerImpl;
import com.megacast.connectsdkwrapper.internal.managers.ImageCastManagerImpl;
import com.megacast.connectsdkwrapper.internal.managers.MediaCastManagerImpl;
import com.megacast.connectsdkwrapper.internal.managers.RemoteApplicationManagerImpl;
import com.megacast.castsdk.providers.CastManagerProvider;
import com.megacast.castsdk.providers.managers.cast.DeviceConnectionManager;
import com.megacast.castsdk.providers.managers.cast.ImageCastManager;
import com.megacast.castsdk.providers.managers.cast.MediaCastManager;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;



/**
 * Created by Dmitry on 27.09.16.
 */
public abstract class ConnectSdkProvider implements CastManagerProvider {

    private ImageCastManager imageCastManager;
    private MediaCastManager mediaCastManager;
    private DeviceConnectionManager deviceConnectionManager;
    private RemoteApplicationManager remoteApplicationManager;

    public ConnectSdkProvider( Context context) throws IllegalArgumentException {
        Util.init(context);

        imageCastManager = new ImageCastManagerImpl();
        mediaCastManager = new MediaCastManagerImpl();
        deviceConnectionManager = new DeviceConnectionManagerImpl();
        remoteApplicationManager = new RemoteApplicationManagerImpl();
    }

    @Override
    public ImageCastManager getImageCastManager() {
        return imageCastManager;
    }

    @Override
    public MediaCastManager getMediaCastManager() {
        return mediaCastManager;
    }

    @Override
    public DeviceConnectionManager getDeviceConnectionManager() {
        return deviceConnectionManager;
    }

    @Override
    public RemoteApplicationManager getRemoteApplicationManager() {
        return remoteApplicationManager;
    }
}
