package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.device.ConnectableDevice;
import com.megacast.castsdk.model.SubtitleDescription;
import com.megacast.castsdk.model.constants.MediaTypes;

import java.util.List;

/**
 * Created by Dmitry on 27.09.16.
 */
public class ChromecastDevice extends TvDevice {

    public ChromecastDevice(ConnectableDevice device) {
        super(device);

        supportedSubtitles.add(SubtitleDescription.SUBTITLE_WEB_VTT);

        //Chromecast Audio

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_MP4,
                MediaTypes.EXTENSION_M4A, MediaTypes.EXTENSION_AAC,
                MediaTypes.EXTENSION_MP4, MediaTypes.EXTENSION_MP3));


        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_MP3,
                MediaTypes.EXTENSION_MP3));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_MPEG,
                MediaTypes.EXTENSION_MP3, MediaTypes.EXTENSION_MPA));


        //Chromecast Images

        imageFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_JPG, MediaTypes.EXTENSION_GIF,
                MediaTypes.EXTENSION_PNG, MediaTypes.EXTENSION_BMP,
                MediaTypes.EXTENSION_WEBP));

        //Chromecast Video

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_VIDEO_MP4,
                MediaTypes.EXTENSION_MP3,
                MediaTypes.EXTENSION_MP4, MediaTypes.EXTENSION_AAC));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_VIDEO_WEBM,
                MediaTypes.EXTENSION_WEBM));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_PLAYLIST_X_MPEGURL,
                MediaTypes.EXTENSION_M3U8));

    }

    @Override
    public boolean hasSupportedVideoStreams(List<String> videoStreams) {
        return true;
    }

    @Override
    public boolean hasSupportedAudioStreams(List<String> audioStreams) {
        return true;
    }
}
