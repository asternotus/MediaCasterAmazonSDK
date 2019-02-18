package com.megacast.castsdk.model;

import rx.Observable;

/**
 * Created by Dmitry on 22.09.16.
 */
public interface RemoteDataReceiverController {

    Observable<Void> pause();

    Observable<Void> resume();

}
