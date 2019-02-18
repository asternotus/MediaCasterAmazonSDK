package com.megacast.castsdk.model;

import android.support.annotation.NonNull;
import com.mega.cast.utils.log.SmartLog;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * Created by Dmitry on 19.04.17.
 */

public class SegmentedMediaController implements MediaController {

    private static final String LOG_TAG = SegmentedMediaController.class.getSimpleName();

    private MediaController mediaController;
    private boolean stoppedByUser = false;

    private SegmentListener segmentListener;

    private int segmentNum;

    private PublishSubject<Integer> videoStatePublisher;
    private Subscription stateSubscription;

    public SegmentedMediaController(MediaController mediaController) {
        videoStatePublisher = PublishSubject.create();
        setMediaController(mediaController, 0);
    }

    @Override
    public Observable<Void> pause() {
        return mediaController.pause();
    }

    @Override
    public Observable<Void> play() {
        return mediaController.play();
    }

    @Override
    public Observable<Void> stop() {
        SmartLog.d(LOG_TAG, "stop ");
        stoppedByUser = true;
        return mediaController.stop();
    }

    @Override
    public Observable<Void> seek(long position) {
        return mediaController.seek(position);
    }

    @Override
    public Observable<Integer> getOnStateChangesSubscriber() {
        return videoStatePublisher.asObservable();
    }

    @Override
    public Observable<Integer> getState() {
        return mediaController.getState();
    }

    @Override
    public Observable<Long> getPosition() {
        return mediaController.getPosition();
    }

    @Override
    public Observable<Void> changeSubtitles(SubtitleDescription subtitleDescription) {
        return mediaController.changeSubtitles(subtitleDescription);
    }

    @Override
    public void setMediaDescription(MediaDescription mediaDescription) {
        mediaController.setMediaDescription(mediaDescription);
    }

    @Override
    public void setSubtitleDescription(SubtitleDescription subtitleDescription) {
        mediaController.setSubtitleDescription(subtitleDescription);
    }

    @Override
    public void setImageDescription(ImageDescription imageDescription) {
        mediaController.setImageDescription(imageDescription);
    }

    @Override
    public MediaDescription getMediaDescription() {
        return mediaController.getMediaDescription();
    }

    @Override
    public SubtitleDescription getSubtitleDescription() {
        return mediaController.getSubtitleDescription();
    }

    @Override
    public ImageDescription getImageDescription() {
        return mediaController.getImageDescription();
    }

    public void setMediaController(MediaController mediaController, int segmentNum) {
        if (stateSubscription != null) {
            stateSubscription.unsubscribe();
        }

        this.mediaController = mediaController;
        this.segmentNum = segmentNum;

        stateSubscription = mediaController.getOnStateChangesSubscriber()
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        onSegmentedVideoError(e);
                    }

                    @Override
                    public void onNext(Integer state) {
                        state = processState(state);
                        videoStatePublisher.onNext(state);
                        if (state == STATE_FINISHED || state == STATE_CANCELLED) {
                            videoStatePublisher.onCompleted();
                        }
                    }

                    @NonNull
                    private Integer processState(Integer state) {
                        SmartLog.d(LOG_TAG, "processState " + state);
                        if (state == STATE_FINISHED) {
                            if (!stoppedByUser) {
                                boolean finished = onSegmentFinished();
                                SmartLog.d(LOG_TAG, "onSegmentFinished " + finished);
                                if (!finished) {
                                    return STATE_BUFFERING;
                                }
                            }
                        }
                        return state;
                    }
                });
    }

    public void onSegmentedVideoError(Throwable e) {
        videoStatePublisher.onError(e);
    }

    public void setSegmentListener(SegmentListener segmentListener) {
        this.segmentListener = segmentListener;
    }

    private boolean onSegmentFinished() {
        return segmentListener == null || segmentListener.onSegmentFinished(segmentNum);
    }

    @Override
    public boolean isSeekSupported() {
        return false;
    }

    @Override
    public void unsubscribe() {

    }

    public void onSegmentNotFoundError() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!stoppedByUser) {
            onSegmentFinished();
        }
    }

    public interface SegmentListener {
        boolean onSegmentFinished(int num);
    }

}
