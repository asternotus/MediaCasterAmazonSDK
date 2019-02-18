package com.megacast.castsdk.model;

import com.megacast.castsdk.internal.model.TranscodedMediaDescription;

import java.io.File;

/**
 * Created by Dmitry on 21.09.16.
 */
public interface TranscodingProgress {

    File getOutputFile();

    String getOutputFileMimeType();

    String getOutputVideoStream();

    String getOutputAudioStream();

    long getTimeMillisec();

    long getSeconds();

    float getFps();

    boolean isReady();

    boolean isFinished();

    TranscodedMediaDescription getTranscodedMediaDescription();

    void setTranscodedMediaDescription(TranscodedMediaDescription playlistDecsription);
}
