package com.megacast.castsdk.model;

import android.graphics.Point;

import java.util.List;

/**
 * Created by Dmitry on 13.09.2016.
 */
public interface Device {

    int UNSUPPORTED_VIDEO_CONVERSION_TYPE_HLS = 1;
    int UNSUPPORTED_VIDEO_CONVERSION_TYPE_FULL = 2;
    int UNSUPPORTED_VIDEO_CONVERSION_TYPE_SEGMENTED = 3;

    int PAIRING_TYPE_UNKNOWN = -1;
    int PAIRING_TYPE_NONE = 0;
    int PAIRING_TYPE_TV_CONFIRMATION = 1;
    int PAIRING_PIN_CODE = 2;

    int CODE_CHROMECAST = 0;
    int CODE_AMAZON_FIRE = 1;
    int CODE_ROKU = 2;
    int CODE_SAMSUNG_SMART_VIEW = 3;
    int CODE_SAMSUNG_ORSAY = 4;
    int CODE_SAMSUNG_LEGACY_2014 = 5;
    int CODE_DLNA = 6;

    String getIpAdress();

    String getName();

    String getID();

    VolumeController getVolumeController();

    boolean isDeviceOnline();

    void addDeviceListener(DeviceListener listener);

    void removeListeners();

    void sendPairingData(String data);

    boolean isConnected();

    boolean isImageResolutionSupported(int width, int height);

    Point getSupportedImageResolution();

    boolean hasSupportedDataType(String metaData, String extension);

    boolean hasSupportedImageType(String mimeType, String extension);

    boolean hasSupportedVideoStreams(List<String> videoStreams);

    boolean hasSupportedAudioStreams(List<String> audioStreams);

    boolean isMediaCompatible();

    void setReceiverInfo(ReceiverInfo receiverInfo);

    ReceiverInfo getReceiverInfo();

    boolean equals(Device device);

    String getUniqueIdentifier();

    void cleanMediaResources();

    List<Integer> getSupportedSubtitles();

    boolean hasSupportedSubtitleFormat(int type);

    int getUnsupportedVideoConversionType();

    boolean isTranscodingForced();

    boolean isAudioPlaylistMode();

    boolean isLiveSubtitleChangesSupported();
}
