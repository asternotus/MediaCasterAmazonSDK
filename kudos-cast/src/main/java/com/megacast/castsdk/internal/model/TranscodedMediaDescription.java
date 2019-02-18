package com.megacast.castsdk.internal.model;

import android.text.TextUtils;

import com.megacast.castsdk.model.ExtendedMediaDescription;

import java.io.File;

/**
 * Created by Dmitry on 23.09.16.
 */
public class TranscodedMediaDescription extends ExtendedMediaDescription {

    private File transcodedFile;
    private String transcodedFileMimeType;

    public TranscodedMediaDescription(ExtendedMediaDescription description, File playlistFile, String playlistMimeType, String outputVideoStream, String outputAudioStream) {
        super(description);
        this.transcodedFile = playlistFile;
        this.transcodedFileMimeType = playlistMimeType;

        getVideoStreamsMap().clear();
        if (!TextUtils.isEmpty(outputVideoStream)) {
            addVideoStream("0:0", outputVideoStream);
        }

        getAudioStreams().clear();
        if (!TextUtils.isEmpty(outputAudioStream)) {
            addAudioStream("0:1", outputAudioStream);
        }
    }

    @Override
    public File getFile() {
        return transcodedFile;
    }

    @Override
    public String getMimeType() {
        return transcodedFileMimeType;
    }

    public File getOriginalFile() {
        return this.file;
    }

    public void setTranscodedFile(File transcodedFile) {
        this.transcodedFile = transcodedFile;
    }
}
