package com.megacast.castsdk.model;


import rx.Observable;

/**
 * Created by Dmitry on 04.10.16.
 */

public interface VolumeController {

    Observable<Void> setVolume(Integer volume);

    Observable<Void> volumeUp();

    Observable<Void> volumeDown();

    Observable<Integer> subscribeVolumeChanges();

}
