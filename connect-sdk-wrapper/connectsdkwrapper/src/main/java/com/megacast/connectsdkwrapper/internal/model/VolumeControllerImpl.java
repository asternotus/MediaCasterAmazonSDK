package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.megacast.castsdk.model.VolumeController;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by Dmitry on 04.10.16.
 */
public class VolumeControllerImpl implements VolumeController {
    private final VolumeControl volumeControl;

    public VolumeControllerImpl(VolumeControl volumeControl) {
        this.volumeControl = volumeControl;
    }

    @Override
    public Observable<Void> setVolume(final Integer volume) {
        if (volumeControl == null) {
            return Observable.error(new Throwable("Device has no volume controls!"));
        }
        return rx.Observable.create(
                new rx.Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        volumeControl.setVolume(volume, new ResponseListener<Object>() {
                            @Override
                            public void onSuccess(Object object) {
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                sub.onError(new Throwable(error.getMessage()));
                            }
                        });
                    }
                }
        );
    }

    @Override
    public Observable<Void> volumeUp() {
        if (volumeControl == null) {
            return Observable.error(new Throwable("Device has no volume controls!"));
        }
        return rx.Observable.create(
                new rx.Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        volumeControl.volumeUp(new ResponseListener<Object>() {
                            @Override
                            public void onSuccess(Object object) {
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                sub.onError(new Throwable(error.getMessage()));
                            }
                        });
                    }
                }
        );
    }

    @Override
    public Observable<Void> volumeDown() {
        if (volumeControl == null) {
            return Observable.error(new Throwable("Device has no volume controls!"));
        }
        return rx.Observable.create(
                new rx.Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        volumeControl.volumeDown(new ResponseListener<Object>() {
                            @Override
                            public void onSuccess(Object object) {
                                sub.onCompleted();
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                sub.onError(new Throwable(error.getMessage()));
                            }
                        });
                    }
                }
        );
    }

    @Override
    public Observable<Integer> subscribeVolumeChanges() {
        if (volumeControl == null) {
            return Observable.error(new Throwable("Device has no volume controls!"));
        }
        return Observable.create(
                new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(final Subscriber<? super Integer> sub) {
                        volumeControl.subscribeVolume(new VolumeControl.VolumeListener() {
                            @Override
                            public void onSuccess(Float volume) {
                                float curVolume = volume;
                                sub.onNext(Math.round(curVolume));
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                sub.onError(new Throwable(error.getMessage()));
                            }
                        });
                    }
                }
        );
    }
}
