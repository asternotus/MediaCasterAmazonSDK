package com.megacast.localmediasharing.managers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mega.cast.utils.log.SmartLog;

import com.megacast.castsdk.internal.model.TranscodedMediaDescription;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ImageDescription;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.RemoteDataReceiverController;
import com.megacast.castsdk.model.ServerConnection;
import com.megacast.castsdk.model.SubtitleDescription;
import com.megacast.castsdk.providers.managers.sharing.MediaSharingManager;
import com.megacast.localmediasharing.managers.server.SubtitleServer;
import com.megacast.localmediasharing.managers.server.WebServerAbstractFile;
import com.megacast.localmediasharing.managers.server.WebServerFakePlaylist;
import com.megacast.localmediasharing.managers.server.WebServerImage;
import com.megacast.localmediasharing.managers.server.WebServerLocalMedia;
import com.megacast.localmediasharing.managers.server.WebServerLocalPlaylist;


import org.nanohttpd.protocols.http.NanoHTTPD;

import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Created by Dmitry on 16.09.16.
 */
public class MediaSharingManagerImpl implements MediaSharingManager {

    private static final String LOG_TAG = MediaSharingManagerImpl.class.getSimpleName();

    private WebServerImage iconServer;
    private WebServerImage imageServer;
    private SubtitleServer subtitleServer;
    private WebServerLocalMedia videoServer;
    private WebServerLocalPlaylist playlistServer;
    private WebServerFakePlaylist fakePlaylistServer;
    private WebServerAbstractFile abstractFileServer;

    private Context context;

    public MediaSharingManagerImpl(Context context) {
        this.context = context;
        this.imageServer = new WebServerImage(context);
        this.iconServer = new WebServerImage(context);
        this.subtitleServer = new SubtitleServer(context);
        this.videoServer = new WebServerLocalMedia(context);
        this.playlistServer = new WebServerLocalPlaylist(context);
        this.fakePlaylistServer = new WebServerFakePlaylist(context);
        this.abstractFileServer = new WebServerAbstractFile(context);
    }

    //  <editor-fold desc="public interface">

    @Override
    public Observable<ServerConnection> shareMediaFile(final Device device, final MediaDescription description) {
        return prepareMediaIconUrl(device, description)
                .flatMap(new Func1<String, Observable<ServerConnection>>() {
                    @Override
                    public Observable<ServerConnection> call(String url) {
                        if (TextUtils.isEmpty(url)) {
                            SmartLog.e(LOG_TAG, "icon url was not prepared! ");
                        } else {
                            SmartLog.d(LOG_TAG, "icon url: " + url);
                        }
                        return prepareMediaFileUrl(device, description);
                    }
                });
    }

    @Override
    public Observable<ServerConnection> shareMediaFakePlaylist(final Device device, final MediaDescription description) {
        return prepareMediaIconUrl(device, description)
                .flatMap(new Func1<String, Observable<ServerConnection>>() {
                    @Override
                    public Observable<ServerConnection> call(String url) {
                        if (TextUtils.isEmpty(url)) {
                            SmartLog.e(LOG_TAG, "icon url was not prepared! ");
                        } else {
                            SmartLog.d(LOG_TAG, "icon url: " + url);
                        }
                        return prepareMediaFileFakePlaylistUrl(device, description);
                    }
                });
    }

    @Override
    public Observable<ServerConnection> sharePlaylistFile(final Device device, final TranscodedMediaDescription description, final RemoteDataReceiverController remoteDataReceiverController) {
        return Observable.create(
                new Observable.OnSubscribe<ServerConnection>() {
                    @Override
                    public void call(final Subscriber<? super ServerConnection> sub) {
                        try {
                            playlistServer.setBindListener(new NanoHTTPD.BindListener() {
                                @Override
                                public void onBound() {
                                    description.setCastUrl(playlistServer.getRemoteUrl());
                                    sub.onNext(playlistServer.getServerConnection());
                                    sub.onCompleted();
                                }

                                @Override
                                public void onException() {
                                    sub.onError(new Throwable("Could not bind port!"));
                                }
                            });
                            playlistServer.stop();
                            playlistServer.prepare(description.getOriginalFile(), description.getFile(), description.getDuration(), remoteDataReceiverController);
                            playlistServer.setDevice(device);
                            playlistServer.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                            sub.onError(e);
                        }
                    }
                }
        );
    }

    @Override
    public Observable<ServerConnection> shareSubtitleFile(final Device device, final SubtitleDescription description) {
        return Observable.create(
                new Observable.OnSubscribe<ServerConnection>() {
                    @Override
                    public void call(final Subscriber<? super ServerConnection> sub) {
                        try {
                            subtitleServer.setBindListener(new NanoHTTPD.BindListener() {
                                @Override
                                public void onBound() {
                                    description.setCastUrl(subtitleServer.getRemoteUrl());
                                    sub.onNext(subtitleServer.getServerConnection());
                                    sub.onCompleted();
                                }

                                @Override
                                public void onException() {
                                    sub.onError(new Throwable("Could not bind port!"));
                                }
                            });
                            subtitleServer.stop();
                            subtitleServer.setFile(description.getFile(), description.getMimeType());
                            subtitleServer.setDevice(device);
                            subtitleServer.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                            sub.onError(e);
                        }
                    }
                }
        );
    }

    //TODO icon url
    @Override
    public Observable<ServerConnection> shareImageFile(final Device device, final ImageDescription description) {
        return Observable.create(
                new Observable.OnSubscribe<ServerConnection>() {
                    @Override
                    public void call(final Subscriber<? super ServerConnection> sub) {
                        try {
                            if (description.getFile() != null) {
                                imageServer.setBindListener(new NanoHTTPD.BindListener() {
                                    @Override
                                    public void onBound() {
                                        description.setServerConnection(imageServer.getServerConnection());
                                        description.setCastUrl(imageServer.getRemoteUrl());
                                        sub.onNext(imageServer.getServerConnection());
                                        sub.onCompleted();
                                    }

                                    @Override
                                    public void onException() {
                                        sub.onError(new Throwable("Could not bind port!"));
                                    }
                                });
                                imageServer.stop();
                                imageServer.setFile(description.getFile(), description.getMimeType());
                                imageServer.setDevice(device);
                                imageServer.start();
                            } else if (description.getAbstractFile() != null) {
                                abstractFileServer.setBindListener(new NanoHTTPD.BindListener() {
                                    @Override
                                    public void onBound() {
                                        description.setServerConnection(abstractFileServer.getServerConnection());
                                        description.setCastUrl(abstractFileServer.getRemoteUrl());
                                        sub.onNext(abstractFileServer.getServerConnection());
                                        sub.onCompleted();
                                    }

                                    @Override
                                    public void onException() {
                                        sub.onError(new Throwable("Could not bind port!"));
                                    }
                                });
                                abstractFileServer.stop();
                                abstractFileServer.setFile(description.getAbstractFile(), description.getMimeType());
                                abstractFileServer.setDevice(device);
                                abstractFileServer.start();
                            } else {
                                sub.onError(new Throwable("Media description should have a valid file!"));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            sub.onError(e);
                        }
                    }
                }
        );
    }

    @Override
    public void setPlaylistAutoPause(boolean playlistAutoResume) {
        playlistServer.setAutoPause(playlistAutoResume);
    }

    @Override
    public void setAutoDeleteServedSegments(boolean autoDelete) {
        playlistServer.setRemovePlaylistItemsOnServe(autoDelete);
    }

    @Override
    public void setDLNAFixEnabled(boolean enabled) {
        videoServer.setDLNAFixEnabled(enabled);
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private Observable<ServerConnection> prepareMediaFileFakePlaylistUrl(final Device device, final MediaDescription description) {
        return Observable.create(
                new Observable.OnSubscribe<ServerConnection>() {
                    @Override
                    public void call(final Subscriber<? super ServerConnection> sub) {
                        try {
                            fakePlaylistServer.setBindListener(new NanoHTTPD.BindListener() {
                                @Override
                                public void onBound() {
                                    description.setServerConnection(fakePlaylistServer.getServerConnection());
                                    description.setCastUrl(fakePlaylistServer.getRemoteUrl());
                                    sub.onNext(fakePlaylistServer.getServerConnection());
                                    sub.onCompleted();
                                }

                                @Override
                                public void onException() {
                                    sub.onError(new Throwable("Could not bind port!"));
                                }
                            });
                            fakePlaylistServer.stop();
                            fakePlaylistServer.prepare(description.getFile(), description.getDuration());
                            fakePlaylistServer.setDevice(device);
                            fakePlaylistServer.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                            sub.onError(e);
                        }
                    }
                }
        );
    }

    private Observable<ServerConnection> prepareMediaFileUrl(final Device device, final MediaDescription description) {
        return Observable.create(
                new Observable.OnSubscribe<ServerConnection>() {
                    @Override
                    public void call(final Subscriber<? super ServerConnection> sub) {
                        try {
                            if (description.getFile() != null) {
                                videoServer.setBindListener(new NanoHTTPD.BindListener() {
                                    @Override
                                    public void onBound() {
                                        description.setServerConnection(videoServer.getServerConnection());
                                        description.setCastUrl(videoServer.getRemoteUrl());
                                        sub.onNext(videoServer.getServerConnection());
                                        sub.onCompleted();
                                    }

                                    @Override
                                    public void onException() {
                                        sub.onError(new Throwable("Could not bind port!"));
                                    }
                                });
                                videoServer.stop();
                                videoServer.setFile(description.getFile(), description.getMimeType());
                                videoServer.setDevice(device);
                                videoServer.start();
                            } else if (description.getAbstractFile() != null) {
                                abstractFileServer.setBindListener(new NanoHTTPD.BindListener() {
                                    @Override
                                    public void onBound() {
                                        description.setServerConnection(abstractFileServer.getServerConnection());
                                        description.setCastUrl(abstractFileServer.getRemoteUrl());
                                        sub.onNext(abstractFileServer.getServerConnection());
                                        sub.onCompleted();
                                    }

                                    @Override
                                    public void onException() {
                                        sub.onError(new Throwable("Could not bind port!"));
                                    }
                                });
                                abstractFileServer.stop();
                                abstractFileServer.setFile(description.getAbstractFile(), description.getMimeType());
                                abstractFileServer.setDevice(device);
                                abstractFileServer.start();
                            } else {
                                sub.onError(new Throwable("Media description should have a valid file!"));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            sub.onError(e);
                        }
                    }
                }
        );
    }

    @NonNull
    private Observable<String> prepareMediaIconUrl(final Device device, final MediaDescription description) {
        return Observable.create(
                new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> sub) {
                        try {
                            iconServer.setBindListener(new NanoHTTPD.BindListener() {
                                @Override
                                public void onBound() {
                                    description.setIconURL(iconServer.getRemoteUrl());
                                    sub.onNext(iconServer.getRemoteUrl());
                                    sub.onCompleted();
                                }

                                @Override
                                public void onException() {
                                    sub.onError(new Throwable("Could not bind port!"));
                                }
                            });
                            iconServer.stop();
                            if (description.getIconFile() != null && description.getIconFile().exists()) {
                                iconServer.setFile(description.getIconFile(), description.getMimeType());
                                iconServer.setDevice(device);
                                iconServer.start();
                            } else {
                                sub.onNext(null);
                                sub.onCompleted();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            sub.onError(e);
                        }
                    }
                }
        );
    }

    //  </editor-fold>

}
