package com.megacast.castsdk.providers.managers.cast;

import android.support.annotation.Nullable;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.MediaController;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.SubtitleDescription;

import rx.Observable;

/**
 * Created by Dmitry on 14.09.16.
 */
public interface MediaCastManager {

    Observable<MediaController> beamMediaFile(Device device,
                                              MediaDescription mediaDescription,
                                              @Nullable SubtitleDescription subtitleDescription);

}
