package com.megacast.castsdk.model;

import rx.Observable;

/**
 * Created by Dmitry on 13.09.2016.
 */
public interface MediaController {

    int STATE_UNSUPPORTED_FORMAT_ERROR = -3;

    int STATE_ERROR = -2;

    /**
     * Unknown state
     */
    int STATE_UNKNOWN = -1;

    /**
     * Media source is not set.
     */
    int STATE_IDLE = 0;

    /**
     * Media is playing.
     */
    int STATE_PLAYING = 1;

    /**
     * Media is paused.
     */
    int STATE_PAUSED = 2;

    /**
     * Media is buffering on the first screen device (e.g. on the TV)
     */
    int STATE_BUFFERING = 3;

    /**
     * Playback is finished.
     */
    int STATE_FINISHED = 4;

    /**
     * Playback was cancelled.
     */
    int STATE_CANCELLED = 5;

    Observable<Void> pause();

    Observable<Void> play();

    Observable<Void> stop();

    Observable<Void> seek(long position);

    Observable<Integer> getOnStateChangesSubscriber();

    Observable<Integer> getState();

    Observable<Long> getPosition();

    Observable<Void> changeSubtitles(SubtitleDescription subtitleDescription);

    void setMediaDescription(MediaDescription mediaDescription);

    void setSubtitleDescription(SubtitleDescription subtitleDescription);

    void setImageDescription(ImageDescription imageDescription);

    MediaDescription getMediaDescription();

    SubtitleDescription getSubtitleDescription();

    ImageDescription getImageDescription();

    boolean isSeekSupported();

    void unsubscribe();
}
