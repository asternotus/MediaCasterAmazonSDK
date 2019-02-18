package com.megacast.castsdk.model;

import com.megacast.castsdk.providers.managers.transcoding.TranscodingManager;

/**
 * Created by Dmitry on 26.09.16.
 */
public class StreamingOptions {

    public static final int DELAY_FINISH_TRANSCODING = TranscodingManager.DELAY_WAIT_FOR_FINISH;
    public static final int DELAY_NONE = TranscodingManager.DELAY_NONE;

    private long delayBeforeStart;

    private boolean autoPause;

    private String tempStoragePath;

    private boolean insufficientStorageWarningEnabled;

    private boolean forceTranscoding;

    private int defaultQualityPreset = TranscodingManager.PRESET_ULTRA_FAST;
    private int hdQualityPreset = TranscodingManager.PRESET_ULTRA_FAST;
    private int fullHdQualityPreset = TranscodingManager.PRESET_ULTRA_FAST;

    private boolean allow4kSupport = false; // TODO change to false

    private boolean autoDeleteServedSegments = false;
    private boolean DLNAFixEnabled = false;

    public StreamingOptions() {
        delayBeforeStart = DELAY_NONE;
        autoPause = false;
        tempStoragePath = null;
        insufficientStorageWarningEnabled = true;
        forceTranscoding = false;
    }

    public long getDelayBeforeStart() {
        return delayBeforeStart;
    }

    public void setDelayBeforeStart(long delayBeforeStart) {
        this.delayBeforeStart = delayBeforeStart;
    }

    public boolean isAutoPause() {
        return autoPause;
    }

    public void setAutoPause(boolean autoPause) {
        this.autoPause = autoPause;
    }

    public String getTempStoragePath() {
        return tempStoragePath;
    }

    public void setTempStoragePath(String tempStoragePath) {
        this.tempStoragePath = tempStoragePath;
    }

    public void setInsufficientStorageWarningEnabled(boolean insufficientStorageWarningEnabled) {
        this.insufficientStorageWarningEnabled = insufficientStorageWarningEnabled;
    }

    public boolean getInsufficientStorageWarningEnabled() {
        return insufficientStorageWarningEnabled;
    }

    public boolean isForceTranscoding() {
        return forceTranscoding;
    }

    public void setForceTranscoding(boolean forceTranscoding) {
        this.forceTranscoding = forceTranscoding;
    }

    public void setDefaultQualityPreset(int preset) {
        this.defaultQualityPreset = preset;
    }

    public void setHdQualityPreset(int preset) {
        this.hdQualityPreset = preset;
    }

    public void setFullHdQualityPreset(int preset) {
        this.fullHdQualityPreset = preset;
    }

    public int getDefaultQualityPreset() {
        return defaultQualityPreset;
    }

    public int getHdQualityPreset() {
        return hdQualityPreset;
    }

    public int getFullHdQualityPreset() {
        return fullHdQualityPreset;
    }

    public boolean isAutoDeleteServedSegments() {
        return autoDeleteServedSegments;
    }

    public void setAllow4kSupport(boolean allow4kSupport) {
        this.allow4kSupport = allow4kSupport;
    }

    public boolean isAllow4kSupport() {
        return allow4kSupport;
    }

    public void setAutoDeleteServedSegments(boolean autoDeleteServedSegments) {
        this.autoDeleteServedSegments = autoDeleteServedSegments;
    }

    public boolean isDLNAFixEnabled() {
        return DLNAFixEnabled;
    }

    public void setDLNAFixEnabled(boolean DLNAFixEnabled) {
        this.DLNAFixEnabled = DLNAFixEnabled;
    }
}

