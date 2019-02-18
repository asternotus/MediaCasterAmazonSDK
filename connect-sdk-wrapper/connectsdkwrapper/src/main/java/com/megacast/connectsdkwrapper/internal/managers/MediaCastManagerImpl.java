package com.megacast.connectsdkwrapper.internal.managers;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.connectsdk.service.capability.Launcher;
import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.command.ServiceCommandError;
import com.megacast.castsdk.exceptions.MediaUnsupportedException;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;
import com.megacast.connectsdkwrapper.internal.model.MediaControllerImpl;
import com.megacast.castsdk.providers.managers.cast.MediaCastManager;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.MediaController;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.SubtitleDescription;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by Dmitry on 13.09.2016.
 */
public class MediaCastManagerImpl implements MediaCastManager {

    private static final String LOG_TAG = MediaCastManagerImpl.class.getSimpleName();

    private String[] requiredCapabilities = {
            MediaPlayer.Play_Video, MediaPlayer.Play_Audio,
    };

    //  <editor-fold desc="public interface">

    @Override
    public Observable<MediaController> beamMediaFile(final Device device,
                                                     final MediaDescription mediaDescription,
                                                     @Nullable final SubtitleDescription subtitleDescription) {
        return Observable.create(
                new Observable.OnSubscribe<MediaController>() {
                    @Override
                    public void call(final Subscriber<? super MediaController> sub) {
                        beamMediaFile(device, mediaDescription, subtitleDescription, new MediaLaunchListener() {
                            @Override
                            public void onSuccess(MediaController controller) {
                                controller.setMediaDescription(mediaDescription);
                                controller.setSubtitleDescription(subtitleDescription);
                                sub.onNext(controller);
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(Throwable cause) {
                                sub.onError(cause);
                            }
                        });
                    }
                }
        );
    }

    //  </editor-fold>


    //  <editor-fold desc="private">

    private void beamMediaFile(final Device device, final MediaDescription mediaDescription,
                               final SubtitleDescription subtitleDescription,
                               final MediaLaunchListener imageLaunchListener) throws IllegalArgumentException {

        if (!(device instanceof TvDevice)) {
            if (imageLaunchListener != null) {
                imageLaunchListener.onError(new Throwable("Wrong device type!"));
            }
            return;
        }

        final ConnectableDevice connectableDevice = ((TvDevice) device).getConnectableDevice();

        if (connectableDevice.isConnectable() && !connectableDevice.isConnected()) {
            if (imageLaunchListener != null) {
                imageLaunchListener.onError(new Throwable("Device must be connected!"));
            }
            return;
        }

        MediaPlayer mediaPlayer = connectableDevice.getCapability(MediaPlayer.class);

        if (!connectableDevice.hasCapabilities(requiredCapabilities) || mediaPlayer == null) {
            if (imageLaunchListener != null) {
                imageLaunchListener.onError(new Throwable("Device hasn't Media Player capabilities!"));
            }
            return;
        }

        MediaInfo.Builder builder = new MediaInfo.Builder(mediaDescription.getCastUrl(), mediaDescription.getMimeType())
                .setTitle(mediaDescription.getTitle())
                .setDescription(mediaDescription.getDescription());

        //for custom receivers
        if (mediaDescription.getExtras() != null) {
            builder.setExtra(mediaDescription.getExtras());
        }

        if (!TextUtils.isEmpty(mediaDescription.getIconURL())) {
            SmartLog.d(LOG_TAG, "set icon " + mediaDescription.getIconURL());
            builder.setIcon(mediaDescription.getIconURL());
        }

        if (subtitleDescription != null) {

            SubtitleInfo subtitles = new SubtitleInfo.Builder(subtitleDescription.getCastUrl())
                    .setMimeType(subtitleDescription.getMimeType())
                    .setLanguage(subtitleDescription.getLanguage())
                    .setLabel(subtitleDescription.getLabel())
                    .build();

            builder.setSubtitleInfo(subtitles);
        }

        MediaInfo mediaInfo = builder.build();

        MediaPlayer.LaunchListener listener = new MediaPlayer.LaunchListener() {
            @Override
            public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                SmartLog.d(LOG_TAG, "beam media  onSuccess");
                if (imageLaunchListener != null) {
                    MediaControllerImpl mediaController = new MediaControllerImpl(object.launchSession, object.mediaControl, object.playlistControl);
                    imageLaunchListener.onSuccess(mediaController);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.d(LOG_TAG, "beam media  onError " + error.getMessage());
                final Throwable cause;
                if (error.getCode() == ServiceCommandError.MEDIA_UNSUPPORTED) {
                    cause = new MediaUnsupportedException(error.getMessage());
                } else {
                    cause = new Throwable(error.getMessage());
                }
                if (imageLaunchListener != null) {
                    imageLaunchListener.onError(cause);
                }
            }
        };

        SmartLog.d(LOG_TAG, String.format("beam media \n url: %s \n mime type: %s",
                mediaInfo.getUrl(), mediaInfo.getMimeType()));

        if (mediaInfo.getImages() != null) {
            for (ImageInfo imageInfo : mediaInfo.getImages()) {
                SmartLog.d(LOG_TAG, "image info " + imageInfo.getUrl());
            }
        }

        mediaPlayer.playMedia(mediaInfo, false, listener);
    }

    //  </editor-fold>

    //  <editor-fold desc="MediaLaunchListener">

    private interface MediaLaunchListener {

        void onSuccess(MediaController controller);

        void onError(Throwable cause);
    }

    //  </editor-fold>


}
