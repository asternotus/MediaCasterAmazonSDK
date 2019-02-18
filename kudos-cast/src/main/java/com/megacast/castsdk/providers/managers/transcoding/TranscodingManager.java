package com.megacast.castsdk.providers.managers.transcoding;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.megacast.castsdk.model.ExtendedMediaDescription;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.SegmentTranscodingProgress;
import com.megacast.castsdk.model.TranscodingProgress;
import com.megacast.castsdk.model.constants.MediaTypes;

import java.io.File;

import rx.Observable;

/**
 * Created by Dmitry on 21.09.16.
 */
public interface TranscodingManager {

    int PRESET_ULTRA_FAST = 1001;
    int PRESET_SUPER_FAST = 1002;
    int PRESET_VERY_FAST = 1003;
    int PRESET_FASTER = 1004;
    int PRESET_FAST = 1005;
    int PRESET_MEDIUM = 1006;
    int PRESET_SLOW = 1007;
    int PRESET_SLOWER = 1008;
    int PRESET_VERY_SLOW = 1009;

    int PRESET_4K = 1010;

    enum OutputFormat {M3U8, M4A, AUDIO_MP4, TS, MP4, MPEG}

    String PLAYLIST_ITEM_EXTENSION = MediaTypes.EXTENSION_TS;

    String PLAYLIST_EXTENSION = MediaTypes.EXTENSION_M3U8;
    String PLAYLIST_MIME_TYPE = MediaTypes.MIME_TYPE_PLAYLIST_X_MPEGURL;
    String PLAYLIST_FILE_NAME = "list";

    String DEFAULT_CONVERTED_VIDEO_EXTENSION = MediaTypes.EXTENSION_TS;
    String DEFAULT_CONVERTED_VIDEO_MIME_TYPE = MediaTypes.MIME_TYPE_TS;
    String DEFAULT_CONVERTED_VIDEO_FILE_NAME = "video";

    String MP4_CONVERTED_VIDEO_MIME_TYPE = MediaTypes.MIME_TYPE_VIDEO_MP4;
    String MP4_CONVERTED_VIDEO_EXTENSION = MediaTypes.EXTENSION_MP4;

    String MPEG_CONVERTED_VIDEO_MIME_TYPE = MediaTypes.MIME_TYPE_VIDEO_MPEG;
    String MPEG_CONVERTED_VIDEO_EXTENSION = MediaTypes.EXTENSION_MPEG;

    String AUDIO_EXTENSION = MediaTypes.EXTENSION_M4A;
    String AUDIO_MIME_TYPE = MediaTypes.MIME_TYPE_AUDIO_MP4;
    String AUDIO_FILE_NAME = "track";

    String AUDIO_EXTENSION_MP4 = MediaTypes.EXTENSION_MP4;

    String THUMB_EXTENSION = MediaTypes.EXTENSION_JPG;

    int DELAY_WAIT_FOR_FINISH = -1;
    int DELAY_NONE = 0;

    Observable<Void> prepare();

    Observable<TranscodingProgress> transcodeAudio( File file, String... streamIndexes);

    Observable<TranscodingProgress> transcodeAudio( String sourceUrl, String... streamIndexes);

    Observable<TranscodingProgress> transcodeMedia( File file, @Nullable File subtitleFile, String... streamIndexes);

    Observable<TranscodingProgress> transcodeMedia( String sourceUrl, @Nullable File subtitleFile, String... streamIndexes);

    Observable<TranscodingProgress> transcodeMediaFull(File file, @Nullable File subtitleFile, String... streamIndexes);

    Observable<TranscodingProgress> transcodeMediaFull( String sourceUrl, @Nullable File subtitleFile, String... streamIndexes);

    Observable<SegmentTranscodingProgress> transcodeMediaSegmented( File file, long duration, @Nullable File subtitleFile);

    Observable<SegmentTranscodingProgress> transcodeMediaSegmented( String sourceUrl, long duration, @Nullable File subtitleFile);

    Observable<File> rotateMedia(File file, int currentRotation, int expectedRotation);

    Observable<Void> cancel();

    Observable<Long> getVideoDurationMillisec( File file, boolean stopTranscoding);

    Observable<Bitmap> getVideoThumbnail( File file, boolean stopTranscoding);

    Observable<Bitmap> getVideoThumbnail( String sourceUrl, boolean stopTranscoding);

    Observable<ExtendedMediaDescription> getExtendedVideoDescription( MediaDescription mediaDescription, boolean stopTranscoding);

    Observable<Void> cleanTemporaryFiles(String[] filters);

    void setStorageDir(String storageDir);

    void setReadyStateDelay(long seconds);

    boolean isTranscodingRunning();

    File getTempStorage();

    long getExpectedTranscodingSpace(File file);

    void setVideoPreset(int preset);
}
