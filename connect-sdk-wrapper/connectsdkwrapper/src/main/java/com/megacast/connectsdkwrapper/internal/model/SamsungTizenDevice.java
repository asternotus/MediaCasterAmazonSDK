package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.device.ConnectableDevice;
import com.megacast.castsdk.model.SubtitleDescription;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungTizenDevice extends TvDevice {

    public SamsungTizenDevice(ConnectableDevice device) {
        super(device);

        supportedSubtitles.add(SubtitleDescription.SUBTITLE_SAMI);

        setAllowAllMediaFormats(true);
        setAllowAllImageFormats(true);
    }

    @Override
    public boolean isImageResolutionSupported(int width, int height) {
        return true;
    }

}

