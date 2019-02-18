package com.megacast.castsdk.providers.managers.sharing;

import com.megacast.castsdk.internal.model.TranscodedMediaDescription;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ImageDescription;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.RemoteDataReceiverController;
import com.megacast.castsdk.model.ServerConnection;
import com.megacast.castsdk.model.SubtitleDescription;


import rx.Observable;

/**
 * Created by Dmitry on 16.09.16.
 */
public interface MediaSharingManager {

    Observable<ServerConnection> shareMediaFile(Device device, MediaDescription description);

    Observable<ServerConnection> shareMediaFakePlaylist(Device device, MediaDescription description);

    Observable<ServerConnection> sharePlaylistFile(Device device, TranscodedMediaDescription description,
                                                   RemoteDataReceiverController remoteDataReceiverController);

    Observable<ServerConnection> shareSubtitleFile(Device device, SubtitleDescription description);

    Observable<ServerConnection> shareImageFile(Device device, ImageDescription description);

    void setPlaylistAutoPause(boolean playlistAutoResume);

    void setAutoDeleteServedSegments(boolean autoDelete);

    void setDLNAFixEnabled(boolean enabled);
}
