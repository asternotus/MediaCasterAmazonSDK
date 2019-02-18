package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.device.ConnectableDevice;
import com.megacast.castsdk.model.SubtitleDescription;
import com.megacast.castsdk.model.constants.MediaTypes;

/**
 * Created by Dmitry on 19.09.16.
 */
public class AmazonDevice extends TvDevice {

    public AmazonDevice(ConnectableDevice device) {
        super(device);

        supportedSubtitles.add(SubtitleDescription.SUBTITLE_WEB_VTT);

        //Fire TV Images
        imageFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_JPG, MediaTypes.EXTENSION_GIF,
                MediaTypes.EXTENSION_PNG, MediaTypes.EXTENSION_BMP));

        supportedVideoCodecs = new String[]{
                MediaTypes.VIDEO_CODEC_H263, MediaTypes.VIDEO_CODEC_H264, MediaTypes.VIDEO_CODEC_MPEG4
        };

        supportedAudioCodecs = new String[]{MediaTypes.AUDIO_CODEC_MP3,
                MediaTypes.AUDIO_CODEC_AAC, MediaTypes.AUDIO_CODEC_AC3, MediaTypes.AUDIO_CODEC_EAC3,
                MediaTypes.AUDIO_CODEC_FLAC, MediaTypes.AUDIO_CODEC_MIDI, MediaTypes.AUDIO_CODEC_PCM,
                MediaTypes.AUDIO_CODEC_VORBIS, MediaTypes.AUDIO_CODEC_AMR_NB, MediaTypes.AUDIO_CODEC_AMR_WB
        };
        //Fire TV Audio

        addFireTvAudio();

        //Fire TV Images

        addFireTvImages();

        //Fire TV Video

        addFireTvVideo();

        //Fire TV Playlist

        addFireTvPlaylists();

//        setLiveSubtitleChangesSupported(true);
    }

    private void addFireTvPlaylists() {
        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_PLAYLIST_X_MPEGURL,
                MediaTypes.EXTENSION_M3U8));
    }

    private void addFireTvVideo() {
        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_VIDEO_3GP,
                MediaTypes.EXTENSION_3GP, MediaTypes.EXTENSION_MP4));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_VIDEO_3GPP,
                MediaTypes.EXTENSION_3GP, MediaTypes.EXTENSION_MP4));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_VIDEO_AVC,
                MediaTypes.EXTENSION_3GP, MediaTypes.EXTENSION_MP4,
                MediaTypes.EXTENSION_TS));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_VIDEO_MP4,
                MediaTypes.EXTENSION_MP4));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_VIDEO_MP4V_ES,
                MediaTypes.EXTENSION_3GP));
    }

    private void addFireTvImages() {
        imageFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_JPG, MediaTypes.EXTENSION_GIF,
                MediaTypes.EXTENSION_PNG, MediaTypes.EXTENSION_BMP));
    }

    private void addFireTvAudioOfficial() {
        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_MP4_LATM,
                MediaTypes.EXTENSION_3GP, MediaTypes.EXTENSION_MP4,
                MediaTypes.EXTENSION_M4P, MediaTypes.EXTENSION_AAC));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_AC3,
                MediaTypes.EXTENSION_MP4, MediaTypes.EXTENSION_M4A));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_EAC3,
                MediaTypes.EXTENSION_MP4, MediaTypes.EXTENSION_M4A));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_FLAC,
                MediaTypes.EXTENSION_FLAC));

        mediaFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_MID, MediaTypes.EXTENSION_XMF,
                MediaTypes.EXTENSION_MXMF, MediaTypes.EXTENSION_RTTTL,
                MediaTypes.EXTENSION_RTX, MediaTypes.EXTENSION_OTA,
                MediaTypes.EXTENSION_IMY));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_MP3,
                MediaTypes.EXTENSION_MP3));

        mediaFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_WAVE));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_VORBIS,
                MediaTypes.EXTENSION_OGG, MediaTypes.EXTENSION_MKV));
    }

    private void addFireTvAudio() {
        mediaFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_3GP, MediaTypes.EXTENSION_MP4,
                MediaTypes.EXTENSION_M4P, MediaTypes.EXTENSION_AAC,
                MediaTypes.EXTENSION_M4A, MediaTypes.EXTENSION_FLAC,
                MediaTypes.EXTENSION_MP3, MediaTypes.EXTENSION_WAVE,
                MediaTypes.EXTENSION_WAV, MediaTypes.EXTENSION_OGG
        ));

        mediaFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_MID, MediaTypes.EXTENSION_XMF,
                MediaTypes.EXTENSION_MXMF, MediaTypes.EXTENSION_RTTTL,
                MediaTypes.EXTENSION_RTX, MediaTypes.EXTENSION_OTA,
                MediaTypes.EXTENSION_IMY));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_VORBIS,
                MediaTypes.EXTENSION_OGG, MediaTypes.EXTENSION_MKV));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_AUDIO_MP3,
                MediaTypes.EXTENSION_MP3));
    }

}
