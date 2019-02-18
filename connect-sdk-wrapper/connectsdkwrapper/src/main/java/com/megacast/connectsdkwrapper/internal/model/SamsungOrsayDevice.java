package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.device.ConnectableDevice;
import com.megacast.castsdk.model.SubtitleDescription;

/**
 * Created by Dmitry on 20.04.17.
 */

public class SamsungOrsayDevice extends TvDevice {

    public SamsungOrsayDevice(ConnectableDevice device) {
        super(device);

        supportedSubtitles.add(SubtitleDescription.SUBTITLE_SRT);

        setAllowAllMediaFormats(true);
        setAllowAllImageFormats(true);

        setUnsupportedVideoConversionType(UNSUPPORTED_VIDEO_CONVERSION_TYPE_FULL);
    }

    @Override
    public boolean isImageResolutionSupported(int width, int height) {
        return width <= DEFAULT_SUPPORTED_IMAGE_WIDTH && height <= DEFAULT_SUPPORTED_IMAGE_HEIGHT;
    }

}
