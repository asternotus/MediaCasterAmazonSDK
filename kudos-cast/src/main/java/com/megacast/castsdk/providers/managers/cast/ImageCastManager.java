package com.megacast.castsdk.providers.managers.cast;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ImageDescription;
import com.megacast.castsdk.model.MediaController;

import rx.Observable;

/**
 * Created by Dmitry on 14.09.16.
 */
public interface ImageCastManager {

    Observable<MediaController> beamImageFile( Device device,
                                               ImageDescription imageDescription);

}
