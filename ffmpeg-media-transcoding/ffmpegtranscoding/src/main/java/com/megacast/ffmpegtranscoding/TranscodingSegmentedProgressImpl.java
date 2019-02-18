package com.megacast.ffmpegtranscoding;

import com.megacast.castsdk.model.SegmentTranscodingProgress;

import java.io.File;

/**
 * Created by Dmitry on 19.04.17.
 */

public class TranscodingSegmentedProgressImpl extends TranscodingProgressImpl implements SegmentTranscodingProgress {

    private boolean ready = false;
    private String filePattern;
    private int segmentsCount;

    public TranscodingSegmentedProgressImpl(String message) {
        super(message);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public File getSegmentFile(int index) {
        return new File(String.format(filePattern, index));
    }

    @Override
    public int getSegmentsCount() {
        return segmentsCount;
    }

    public void setSegmentsCount(int segmentsCount) {
        this.segmentsCount = segmentsCount;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }


}
