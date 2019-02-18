package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.command.ServiceCommandError;
import com.mega.cast.utils.log.SmartLog;
import com.megacast.castsdk.model.DeviceListener;
import com.megacast.castsdk.model.ReceiverInfo;
import com.megacast.castsdk.model.SubtitleDescription;
import com.megacast.castsdk.model.constants.MediaTypes;

import java.util.List;

/**
 * Created by Dmitry on 05.10.16.
 */

public class RokuDevice extends TvDevice {

    private ConnectableDeviceListener internalDeviceListener;

    public RokuDevice(ConnectableDevice device) {
        super(device);

        supportedSubtitles.add(SubtitleDescription.SUBTITLE_SRT);

        supportedVideoCodecs = new String[]{MediaTypes.VIDEO_CODEC_H264, MediaTypes.VIDEO_CODEC_H265};
        supportedAudioCodecs = new String[]{MediaTypes.AUDIO_CODEC_AAC, MediaTypes.AUDIO_CODEC_MP3};

        //Roku Audio

        mediaFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_MKV,
                MediaTypes.EXTENSION_MP4, MediaTypes.EXTENSION_MOV));

        mediaFormats.add(new SupportedFormat(null, MediaTypes.EXTENSION_MP3,
                MediaTypes.EXTENSION_WMA, MediaTypes.EXTENSION_FLAC,
                MediaTypes.EXTENSION_WAV, MediaTypes.EXTENSION_MP4,
                MediaTypes.EXTENSION_AC3, MediaTypes.EXTENSION_ASF,
                MediaTypes.EXTENSION_AAC));

        mediaFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_M4A));

        //Roku Video

        mediaFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_MKV,
                MediaTypes.EXTENSION_MP4, MediaTypes.EXTENSION_MOV));

        mediaFormats.add(new SupportedFormat(MediaTypes.MIME_TYPE_PLAYLIST_X_MPEGURL,
                MediaTypes.EXTENSION_M3U8));

        //Roku Images

        imageFormats.add(new SupportedFormat(null,
                MediaTypes.EXTENSION_JPG, MediaTypes.EXTENSION_GIF,
                MediaTypes.EXTENSION_PNG));

        // The connection towards the Roku Device is useless if the receiver is not up and running
        // The device can get forcefully disconnected if the user uses his device remote and so we update the device receiver state here
        internalDeviceListener = new ConnectableDeviceListener() {
            @Override
            public void onDeviceReady(ConnectableDevice device) {

            }

            @Override
            public void onDeviceDisconnected(ConnectableDevice device) {
                SmartLog.d("RokuDevice onDeviceDisconnected. Removing ReceiverInfo!");
                setReceiverInfo(new ReceiverInfo(false));
            }

            @Override
            public void onPairingRequired(ConnectableDevice device, DeviceService service, DeviceService.PairingType pairingType) {

            }

            @Override
            public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {

            }

            @Override
            public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {

            }
        };

        device.addListener(internalDeviceListener);

    }


    //TODO research
    @Override
    public boolean isImageResolutionSupported(int width, int height) {
        return width <= DEFAULT_SUPPORTED_IMAGE_WIDTH && height <= DEFAULT_SUPPORTED_IMAGE_HEIGHT;
    }
}