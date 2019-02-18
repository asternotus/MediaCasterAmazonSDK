package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.service.capability.Launcher;
import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.PlaylistControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession;
import com.megacast.castsdk.model.ImageDescription;
import com.megacast.castsdk.model.MediaController;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.SubtitleDescription;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

/**
 * Created by Dmitry on 13.09.2016.
 */
public class MediaControllerImpl implements MediaController {

    private static final String LOG_TAG = MediaControllerImpl.class.getSimpleName();

    private LaunchSession launchSession;
    private MediaControl mediaControl;
    private PlaylistControl playlistControl;

    private MediaDescription mediaDescription;
    private SubtitleDescription subtitleDescription;
    private ImageDescription imageDescription;

    private PublishSubject<Integer> videoStatePublisher;

    private MediaControl.PlayStateListener playStateListener = new MediaControl.PlayStateListener() {
        @Override
        public void onSuccess(MediaControl.PlayStateStatus object) {
            SmartLog.d(LOG_TAG, "play state " + object.name());
            int state = getMediaControlState(object);
            videoStatePublisher.onNext(state);
            if (state == STATE_FINISHED || state == STATE_CANCELLED) {
                unsubscribe();
            }
        }

        @Override
        public void onError(ServiceCommandError error) {
            SmartLog.d(LOG_TAG, "play state error " + error.getMessage());
            videoStatePublisher.onError(new Throwable(error.getMessage()));
        }
    };

    private boolean unsubscribed = false;

    public MediaControllerImpl(LaunchSession launchSession, final MediaControl mediaControl, PlaylistControl playlistControl) {
        this.launchSession = launchSession;
        this.mediaControl = mediaControl;
        this.playlistControl = playlistControl;

        this.videoStatePublisher = PublishSubject.create();

        mediaControl.subscribePlayState(playStateListener);
    }

    @Override
    public Observable<Void> pause() {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        mediaControl.pause(new WebAppSession.MessageListener() {
                            @Override
                            public void onMessage(Object message) {
                            }

                            @Override
                            public void onSuccess(Object object) {
                                sub.onNext(null);
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
    public Observable<Void> play() {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        mediaControl.play(new WebAppSession.MessageListener() {
                            @Override
                            public void onMessage(Object message) {
                            }

                            @Override
                            public void onSuccess(Object object) {
                                sub.onNext(null);
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
    public Observable<Void> stop() {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        mediaControl.stop(new WebAppSession.MessageListener() {
                            @Override
                            public void onMessage(Object message) {

                            }

                            @Override
                            public void onSuccess(Object object) {
                                sub.onNext(null);
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
    public Observable<Void> seek(final long position) {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        mediaControl.seek(position, new ResponseListener<Object>() {
                            @Override
                            public void onSuccess(Object object) {
                                sub.onNext(null);
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
    public Observable<Integer> getOnStateChangesSubscriber() {
        return videoStatePublisher.asObservable();
    }

    @Override
    public Observable<Integer> getState() {
        return Observable.create(
                new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(final Subscriber<? super Integer> sub) {
                        mediaControl.getPlayState(new MediaControl.PlayStateListener() {
                            @Override
                            public void onSuccess(MediaControl.PlayStateStatus object) {
                                sub.onNext(getMediaControlState(object));
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
    public Observable<Long> getPosition() {
        return Observable.create(
                new Observable.OnSubscribe<Long>() {
                    @Override
                    public void call(final Subscriber<? super Long> sub) {
                        mediaControl.getPosition(new MediaControl.PositionListener() {
                            @Override
                            public void onSuccess(Long position) {
                                sub.onNext(position);
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
    public Observable<Void> changeSubtitles(final SubtitleDescription subtitleDescription) {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        SubtitleInfo subtitles = new SubtitleInfo.Builder(subtitleDescription.getCastUrl())
                                .setMimeType(subtitleDescription.getMimeType())
                                .setLanguage(subtitleDescription.getLanguage())
                                .setLabel(subtitleDescription.getLabel())
                                .build();

                        mediaControl.changeSubtitles(subtitles, new ResponseListener<Object>() {
                            @Override
                            public void onSuccess(Object object) {
                                sub.onNext(null);
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
    public void setMediaDescription(MediaDescription mediaDescription) {
        this.mediaDescription = mediaDescription;
    }

    @Override
    public void setSubtitleDescription(SubtitleDescription subtitleDescription) {
        this.subtitleDescription = subtitleDescription;
    }

    @Override
    public void setImageDescription(ImageDescription imageDescription) {
        this.imageDescription = imageDescription;
    }

    @Override
    public MediaDescription getMediaDescription() {
        return mediaDescription;
    }

    @Override
    public SubtitleDescription getSubtitleDescription() {
        return subtitleDescription;
    }

    @Override
    public ImageDescription getImageDescription() {
        return imageDescription;
    }

    @Override
    public boolean isSeekSupported() {
        return true;
    }

    @Override
    public void unsubscribe() {
        if (unsubscribed) {
            return;
        }

        videoStatePublisher.onCompleted();
        SmartLog.d(LOG_TAG, "unsubscribe play state");
        mediaControl.unSubscribePlayState(playStateListener);
    }

    private int getMediaControlState(MediaControl.PlayStateStatus object) {
        int state = STATE_UNKNOWN;
        switch (object) {
            case UnsupportedFormatError:
                state = STATE_UNSUPPORTED_FORMAT_ERROR;
                break;
            case Error:
                state = STATE_ERROR;
                break;
            case Idle:
                state = STATE_IDLE;
                break;
            case Playing:
                state = STATE_PLAYING;
                break;
            case Paused:
                state = STATE_PAUSED;
                break;
            case Buffering:
                state = STATE_BUFFERING;
                break;
            case Finished:
                state = STATE_FINISHED;
                break;
            case Cancelled:
                state = STATE_CANCELLED;
                break;
        }
        return state;
    }

}
