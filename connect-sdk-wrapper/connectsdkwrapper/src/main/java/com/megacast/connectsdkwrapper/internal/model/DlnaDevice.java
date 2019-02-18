package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.device.ConnectableDevice;
import com.megacast.castsdk.model.SubtitleDescription;

/**
 * Created by Дима on 06.03.2018.
 */

public class DlnaDevice extends TvDevice {

    private boolean samsungTVFlag = false;

    public DlnaDevice(ConnectableDevice device) {
        super(device);

        supportedSubtitles.add(SubtitleDescription.SUBTITLE_SRT);

        setAllowAllMediaFormats(true);
        setAllowAllImageFormats(true);

        setUnsupportedVideoConversionType(UNSUPPORTED_VIDEO_CONVERSION_TYPE_FULL);
    }

    @Override
    public boolean isImageResolutionSupported(int width, int height) {
        return true;
    }

    public void setSamsungTVFlag(boolean samsungTV) {
        this.samsungTVFlag = samsungTV;
    }

    public boolean isSamsungTVFlag() {
        return samsungTVFlag;
    }
}