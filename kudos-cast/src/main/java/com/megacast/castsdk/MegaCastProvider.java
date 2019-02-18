package com.megacast.castsdk;

import android.content.Context;

import com.megacast.castsdk.internal.managers.MegacastStreamingManager;
import com.megacast.castsdk.managers.StreamingManager;
import com.megacast.castsdk.providers.CastManagerProvider;
import com.megacast.castsdk.providers.managers.cast.DeviceConnectionManager;
import com.megacast.castsdk.providers.managers.cast.DeviceDiscoveryManager;
import com.megacast.castsdk.providers.managers.cast.RemoteApplicationManager;
import com.megacast.castsdk.providers.managers.sharing.MediaSharingManager;
import com.megacast.castsdk.providers.managers.subtitle.SubtitleConversionManager;
import com.megacast.castsdk.providers.managers.transcoding.TranscodingManager;



import rx.Observable;

/**
 * Created by Dmitry on 14.09.16.
 */
public class MegaCastProvider {

    private CastManagerProvider provider;
    private StreamingManager streamingManager;
    private TranscodingManager transcodingManager;

    public MegaCastProvider( Context context,  CastManagerProvider provider,
                             MediaSharingManager mediaSharingManager,  TranscodingManager transcodingManager,
                             SubtitleConversionManager subtitleConversionManager) {
        this.provider = provider;
        this.transcodingManager = transcodingManager;
        this.streamingManager = new MegacastStreamingManager(context, provider, mediaSharingManager, transcodingManager, subtitleConversionManager);
    }

    public Observable<Void> prepare() {
        //we are calling only transcoding manager prepare since other managers don't need preparing listeners
        return transcodingManager.prepare();
    }

    public StreamingManager getStreamingManager() {
        return streamingManager;
    }

    public DeviceDiscoveryManager getDeviceDiscoveryManager() {
        return provider.getDeviceDiscoveryManager();
    }

    public DeviceConnectionManager getDeviceConnectionManager() {
        return provider.getDeviceConnectionManager();
    }

    public RemoteApplicationManager getRemoteApplicationManager() {
        return provider.getRemoteApplicationManager();
    }

    public TranscodingManager getTranscodingManager() {
        return transcodingManager;
    }
}
