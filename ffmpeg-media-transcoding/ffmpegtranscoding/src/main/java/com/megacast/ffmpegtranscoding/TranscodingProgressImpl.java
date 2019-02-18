package com.megacast.ffmpegtranscoding;

import com.megacast.castsdk.internal.model.TranscodedMediaDescription;
import com.megacast.castsdk.model.TranscodingProgress;

import java.io.File;

/**
 * Created by Dmitry on 21.09.16.
 */
public class TranscodingProgressImpl implements TranscodingProgress {

    private File outputFile;
    private String outputFileMimeType;
    private String message;
    private boolean finished;

    private TranscodedMediaDescription playlistDescription;

    private String audioStream;
    private String videoStream;

    public TranscodingProgressImpl(String message) {
        this.message = message;
    }

    //  <editor-fold desc="Transcoding progress">

    public File getOutputFile() {
        return outputFile;
    }

    public String getOutputFileMimeType() {
        return outputFileMimeType;
    }

    @Override
    public String getOutputVideoStream() {
        return videoStream;
    }

    @Override
    public String getOutputAudioStream() {
        return audioStream;
    }

    @Override
    public long getTimeMillisec() {
        return getSeconds() * 1000;
    }

    @Override
    public long getSeconds() {
        int timeStart = message.lastIndexOf("time") + 11;
        String secondsStr = message.substring(timeStart, timeStart + 2);
        return getMinutes() * 60 + Integer.parseInt(secondsStr);
    }

    @Override
    public float getFps() {
        final int indexOf = message.lastIndexOf("fps");
        if (indexOf < 0) {
            return 0;
        }

        int fpsStart = indexOf + 4;
        int fpsEnd = message.lastIndexOf("q=") - 1;
        try {
            float parseFloat = Float.parseFloat(message.substring(fpsStart, fpsEnd));
            return parseFloat;
        } catch (NumberFormatException ex) {
            // ignore
        }
        return 0;
    }

    @Override
    public boolean isReady() {
        return outputFile != null && outputFile.exists();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public TranscodedMediaDescription getTranscodedMediaDescription() {
        return playlistDescription;
    }

    @Override
    public void setTranscodedMediaDescription(TranscodedMediaDescription playlistDecsription) {
        this.playlistDescription = playlistDecsription;
    }

    //  </editor-fold>

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setOutputFileMimeType(String outputFileMimeType) {
        this.outputFileMimeType = outputFileMimeType;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setAudioStream(String audioStream) {
        this.audioStream = audioStream;
    }

    public void setVideoStream(String videoStream) {
        this.videoStream = videoStream;
    }

    private long getMinutes() {
        int timeStart = message.lastIndexOf("time") + 8;
        String minutes = message.substring(timeStart, timeStart + 2);
        return Integer.parseInt(minutes);
    }
}

