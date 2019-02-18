package com.megacast.castsdk.managers;

import android.support.annotation.Nullable;

import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ImageDescription;
import com.megacast.castsdk.model.MediaController;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.SubtitleDescription;
import com.megacast.castsdk.model.StreamingOptions;
import com.megacast.castsdk.model.TranscodingProgress;



import rx.Observable;

/**
 * Created by Dmitry on 14.09.16.
 */
public interface StreamingManager {

    Observable<MediaController> streamVideo( Device device,  MediaDescription mediaDescription, @Nullable SubtitleDescription subtitleDescription);

    Observable<MediaController> streamAudio( Device device,  MediaDescription mediaDescription);

    Observable<MediaController> streamImage( Device device,  ImageDescription imageDescription);

    Observable<SubtitleDescription> shareSubtitles( Device device,  SubtitleDescription subtitleDescription);

    Observable<TranscodingProgress> getTranscodingProgressSubscriber();

    Observable<Void> cleanTemporaryFiles(String[] filters);

    void setStreamingOptions(@Nullable StreamingOptions options);

    boolean isTranscodingRunning();
}
