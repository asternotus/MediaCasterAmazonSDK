package com.megacast.castsdk.internal.model;

import com.megacast.castsdk.model.MediaController;
import com.megacast.castsdk.model.RemoteDataReceiverController;

import rx.Observable;

/**
 * Created by Dmitry on 22.09.16.
 */
public class RemoteMediaReceiverController implements RemoteDataReceiverController {

    private MediaController controller;

    public RemoteMediaReceiverController() {
    }

    public RemoteMediaReceiverController(MediaController controller) {
        this.controller = controller;
    }

    public void setController(MediaController controller) {
        this.controller = controller;
    }

    @Override
    public Observable<Void> pause() {
        if (controller != null) {
            return controller.pause();
        }
        return Observable.error(new Throwable("MediaController is not set!"));
    }

    @Override
    public Observable<Void> resume() {
        if (controller != null) {
            return controller.play();
        }
        return Observable.error(new Throwable("MediaController is not set!"));
    }
}
