package com.megacast.ffmpegtranscoding;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mega.cast.utils.log.SmartLog;

import android.util.Pair;
import android.webkit.URLUtil;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.megacast.castsdk.model.ExtendedMediaDescription;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.SegmentTranscodingProgress;
import com.megacast.castsdk.model.TranscodingProgress;
import com.megacast.castsdk.model.constants.MediaTypes;
import com.megacast.castsdk.providers.managers.transcoding.TranscodingManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

/**
 * Created by Dmitry on 21.09.16.
 */

/*
info about current version of the FFMPEG
configuration:
--target-os=linux
--cross-prefix=/home/roman/Downloads/ffmpeg-android/toolchain-android/bin/arm-linux-androideabi-
--arch=arm
--cpu=cortex-a8
--enable-runtime-cpudetect
--sysroot=/home/roman/Downloads/ffmpeg-android/toolchain-android/sysroot
--enable-pic
--enable-libx264
--enable-libass
--enable-libfreetype
--enable-nonfree
--enable-libfdk-aac
--enable-pthreads
--disable-debug
--disable-ffserver
--enable-version3
--enable-hardcoded-tables
--disable-ffplay
--disable-ffprobe
--enable-gpl
--enable-yasm
--disable-doc
--disable-shared
--enable-static
--pkg-config=/home/roman/Downloads/ffmpeg-android/ffmpeg-pkg-config
--prefix=/home/roman/Downloads/ffmpeg-android/build/armeabi-v7a

--extra-cflags=
'
-I/home/roman/Downloads/ffmpeg-android/toolchain-android/include
-U_FORTIFY_SOURCE
-D_FORTIFY_SOURCE=2
-fno-strict-overflow
-fstack-protector-all
'

--extra-ldflags=
'
-L/home/roman/Downloads/ffmpeg-android/toolchain-android/lib
-Wl,-z,relro -Wl,-z,now
-pie
'

--extra-libs=
'
-lpng
-lexpat
-lm
'
--extra-cxxflags=

 */
public class TranscodingManagerImpl implements TranscodingManager {

    private static final String LOG_TAG = TranscodingManagerImpl.class.getSimpleName();
    private static final int MAX_KILLING_ATTEMPTS = 5;
    private static final long MIN_AVAILABLE_SPACE_THRESHOLD_BYTES = 10 * 1024 * 1024;
    private static final double REQUIRED_TRANSCODING_SPACE_MULTIPLIER = 1.5;

    private static final int TRANSCODING_SEGMENT_TIME = 60;
    private static final String ROTATED_FILE_FILTER = "rotated_temp_";

    private FFmpegHelper helper;
    private FFmpeg ffmpeg;

    private final Context context;

    private boolean prepared;

    private String storageDir;

    private long readyStateDelay;

    private int killingAttemps = 0;

    private boolean isTranscodingRunning = false;

    private long totalDeleted = 0;

    private int videoPreset = PRESET_ULTRA_FAST;

    public TranscodingManagerImpl(Context context) {
        this.context = context;
        this.ffmpeg = FFmpeg.getInstance(context);
        this.helper = new FFmpegHelper(context);
        this.prepared = false;

        this.storageDir = null;

        this.readyStateDelay = DELAY_NONE;
    }

    public Observable<Void> prepare() {
        final PublishSubject<Void> observer = PublishSubject.create();

        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    SmartLog.d(LOG_TAG, "prepare onFailure");
                    prepared = false;
                    observer.onError(new FFmpegNotSupportedException("ffmpeg binary failed to load!"));
                }

                @Override
                public void onSuccess() {
                    SmartLog.d(LOG_TAG, "prepare onSuccess");
                    prepared = true;
                    observer.onCompleted();
                }
            });
        } catch (FFmpegNotSupportedException ex) {
            //FFmpeg is not supported by device
            ex.printStackTrace();
            SmartLog.d(LOG_TAG, "FFmpegNotSupportedException " + ex.getMessage());

            observer.onError(ex);
        }
        return observer;
    }

    @Override
    public Observable<TranscodingProgress> transcodeAudio(final File file, final String... streamIndexes) {
        return Observable.create(
                new Observable.OnSubscribe<TranscodingProgress>() {
                    @Override
                    public void call(final Subscriber<? super TranscodingProgress> sub) {
                        if (file == null) {
                            sub.onError(new IllegalArgumentException("File should not be null!"));
                            return;
                        }

                        isTranscodingRunning = true;

                        transcodeMedia(file.getAbsolutePath(), null, OutputFormat.M4A, new TranscodingProgressListener() {
                            @Override
                            public void onProgress(TranscodingProgress progress) {
                                isTranscodingRunning = true;
                                sub.onNext(progress);
                            }

                            @Override
                            public void onFailure(Throwable ex) {
                                isTranscodingRunning = false;
                                sub.onError(ex);
                            }

                            @Override
                            public void onFinished() {
                                isTranscodingRunning = false;
                                sub.onCompleted();
                            }
                        }, streamIndexes);
                    }
                });
    }

    @Override
    public Observable<TranscodingProgress> transcodeMedia(final File file, final File subtitleFile, final String... streamIndexes) {
        return Observable.create(
                new Observable.OnSubscribe<TranscodingProgress>() {
                    @Override
                    public void call(final Subscriber<? super TranscodingProgress> sub) {
                        if (file == null) {
                            sub.onError(new IllegalArgumentException("File should not be null!"));
                            return;
                        }

                        if (subtitleFile != null && !subtitleFile.exists()) {
                            sub.onError(new IllegalStateException("Subtitle file should not be null!"));
                            return;
                        }

                        isTranscodingRunning = true;

                        transcodeMedia(file.getAbsolutePath(), subtitleFile != null ? subtitleFile.getAbsolutePath() : null,
                                OutputFormat.M3U8,
                                new TranscodingProgressListener() {
                                    @Override
                                    public void onProgress(TranscodingProgress progress) {
                                        isTranscodingRunning = true;
                                        sub.onNext(progress);
                                    }

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        isTranscodingRunning = false;
                                        sub.onError(ex);
                                    }

                                    @Override
                                    public void onFinished() {
                                        isTranscodingRunning = false;
                                        sub.onCompleted();
                                    }
                                },
                                streamIndexes);
                    }
                }
        );
    }

    @Override
    public Observable<TranscodingProgress> transcodeAudio(final String sourceUrl, final String... streamIndexes) {
        return Observable.create(
                new Observable.OnSubscribe<TranscodingProgress>() {
                    @Override
                    public void call(final Subscriber<? super TranscodingProgress> sub) {
                        if (TextUtils.isEmpty(sourceUrl)) {
                            sub.onError(new IllegalArgumentException("Source url should not be null!"));
                            return;
                        }

                        isTranscodingRunning = true;

                        transcodeMedia(sourceUrl, null, OutputFormat.M4A, new TranscodingProgressListener() {
                            @Override
                            public void onProgress(TranscodingProgress progress) {
                                isTranscodingRunning = true;
                                sub.onNext(progress);
                            }

                            @Override
                            public void onFailure(Throwable ex) {
                                isTranscodingRunning = false;
                                sub.onError(ex);
                            }

                            @Override
                            public void onFinished() {
                                isTranscodingRunning = false;
                                sub.onCompleted();
                            }
                        }, streamIndexes);
                    }
                });
    }

    @Override
    public Observable<TranscodingProgress> transcodeMedia(final String sourceUrl, @Nullable final File subtitleFile, final String... streamIndexes) {
        return Observable.create(
                new Observable.OnSubscribe<TranscodingProgress>() {
                    @Override
                    public void call(final Subscriber<? super TranscodingProgress> sub) {
                        if (TextUtils.isEmpty(sourceUrl)) {
                            sub.onError(new IllegalArgumentException("Source url should not be null!"));
                            return;
                        }

                        if (subtitleFile != null && !subtitleFile.exists()) {
                            sub.onError(new IllegalStateException("Subtitle file should not be null!"));
                            return;
                        }

                        isTranscodingRunning = true;

                        transcodeMedia(sourceUrl, subtitleFile != null ? subtitleFile.getAbsolutePath() : null,
                                OutputFormat.M3U8,
                                new TranscodingProgressListener() {
                                    @Override
                                    public void onProgress(TranscodingProgress progress) {
                                        isTranscodingRunning = true;
                                        sub.onNext(progress);
                                    }

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        isTranscodingRunning = false;
                                        sub.onError(ex);
                                    }

                                    @Override
                                    public void onFinished() {
                                        isTranscodingRunning = false;
                                        sub.onCompleted();
                                    }
                                },
                                streamIndexes);
                    }
                }
        );
    }

    @Override
    public Observable<SegmentTranscodingProgress> transcodeMediaSegmented(final String sourceUrl, final long duration, @Nullable final File subtitleFile) {
        return Observable.create(
                new Observable.OnSubscribe<SegmentTranscodingProgress>() {
                    @Override
                    public void call(final Subscriber<? super SegmentTranscodingProgress> sub) {
                        if (TextUtils.isEmpty(sourceUrl)) {
                            sub.onError(new IllegalArgumentException("Source url should not be null!"));
                            return;
                        }

                        if (subtitleFile != null && !subtitleFile.exists()) {
                            sub.onError(new IllegalStateException("Subtitle file should not be null!"));
                            return;
                        }

                        isTranscodingRunning = true;

                        int segmentsNumber = (int) (((duration / 1000) / TRANSCODING_SEGMENT_TIME) + 1);

                        transcodeMediaSegmented(sourceUrl, subtitleFile != null ? subtitleFile.getAbsolutePath() : null, segmentsNumber,
                                new TranscodingProgressListener<SegmentTranscodingProgress>() {
                                    @Override
                                    public void onProgress(SegmentTranscodingProgress progress) {
                                        isTranscodingRunning = true;
                                        sub.onNext(progress);
                                    }

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        isTranscodingRunning = false;
                                        sub.onError(ex);
                                    }

                                    @Override
                                    public void onFinished() {
                                        isTranscodingRunning = false;
                                        sub.onCompleted();
                                    }
                                });
                    }
                }
        );
    }

    @Override
    public Observable<SegmentTranscodingProgress> transcodeMediaSegmented(final File file, final long duration, @Nullable final File subtitleFile) {
        return Observable.create(
                new Observable.OnSubscribe<SegmentTranscodingProgress>() {
                    @Override
                    public void call(final Subscriber<? super SegmentTranscodingProgress> sub) {
                        if (file == null) {
                            sub.onError(new IllegalArgumentException("File should not be null!"));
                            return;
                        }

                        if (subtitleFile != null && !subtitleFile.exists()) {
                            sub.onError(new IllegalStateException("Subtitle file should not be null!"));
                            return;
                        }

                        isTranscodingRunning = true;

                        int segmentsNumber = (int) (((duration / 1000) / TRANSCODING_SEGMENT_TIME) + 1);

                        transcodeMediaSegmented(file.getAbsolutePath(), subtitleFile != null ? subtitleFile.getAbsolutePath() : null, segmentsNumber,
                                new TranscodingProgressListener<SegmentTranscodingProgress>() {
                                    @Override
                                    public void onProgress(SegmentTranscodingProgress progress) {
                                        isTranscodingRunning = true;
                                        sub.onNext(progress);
                                    }

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        isTranscodingRunning = false;
                                        sub.onError(ex);
                                    }

                                    @Override
                                    public void onFinished() {
                                        isTranscodingRunning = false;
                                        sub.onCompleted();
                                    }
                                });
                    }
                }
        );
    }

    @Override
    public Observable<TranscodingProgress> transcodeMediaFull(final String sourceUrl, @Nullable final File subtitleFile, final String... streamIndexes) {
        return Observable.create(
                new Observable.OnSubscribe<TranscodingProgress>() {
                    @Override
                    public void call(final Subscriber<? super TranscodingProgress> sub) {
                        if (TextUtils.isEmpty(sourceUrl)) {
                            sub.onError(new IllegalArgumentException("Source url should not be null!"));
                            return;
                        }

                        if (subtitleFile != null && !subtitleFile.exists()) {
                            sub.onError(new IllegalStateException("Subtitle file should not be null!"));
                            return;
                        }

                        isTranscodingRunning = true;

                        transcodeMedia(sourceUrl, subtitleFile != null ? subtitleFile.getAbsolutePath() : null,
                                OutputFormat.TS,
                                new TranscodingProgressListener() {
                                    @Override
                                    public void onProgress(TranscodingProgress progress) {
                                        isTranscodingRunning = true;
                                        sub.onNext(progress);
                                    }

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        isTranscodingRunning = false;
                                        sub.onError(ex);
                                    }

                                    @Override
                                    public void onFinished() {
                                        isTranscodingRunning = false;
                                        sub.onCompleted();
                                    }
                                },
                                streamIndexes);
                    }
                }
        );
    }

    @Override
    public Observable<TranscodingProgress> transcodeMediaFull(final File file, @Nullable final File subtitleFile, final String... streamIndexes) {
        return Observable.create(
                new Observable.OnSubscribe<TranscodingProgress>() {
                    @Override
                    public void call(final Subscriber<? super TranscodingProgress> sub) {
                        if (file == null) {
                            sub.onError(new IllegalArgumentException("File should not be null!"));
                            return;
                        }

                        if (subtitleFile != null && !subtitleFile.exists()) {
                            sub.onError(new IllegalStateException("Subtitle file should not be null!"));
                            return;
                        }

                        isTranscodingRunning = true;

                        transcodeMedia(file.getAbsolutePath(), subtitleFile != null ? subtitleFile.getAbsolutePath() : null,
                                OutputFormat.TS,
                                new TranscodingProgressListener() {
                                    @Override
                                    public void onProgress(TranscodingProgress progress) {
                                        isTranscodingRunning = true;
                                        sub.onNext(progress);
                                    }

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        isTranscodingRunning = false;
                                        sub.onError(ex);
                                    }

                                    @Override
                                    public void onFinished() {
                                        isTranscodingRunning = false;
                                        sub.onCompleted();
                                    }
                                },
                                streamIndexes);
                    }
                }
        );
    }

    @Override
    public Observable<File> rotateMedia(final File file, final int currentRotation, final int expectedRotation) {
        SmartLog.i(LOG_TAG, String.format("rotate file %s\n" +
                        "from %d\n" +
                        "to %d",
                file.getAbsolutePath(), currentRotation, expectedRotation));

        return Observable.create(
                new Observable.OnSubscribe<File>() {

                    @Override
                    public void call(final Subscriber<? super File> sub) {
                        if (file == null) {
                            SmartLog.e(LOG_TAG, "File should not be null!");
                            sub.onError(new IllegalArgumentException("File should not be null!"));
                            return;
                        }

                        if (currentRotation == expectedRotation) {
                            SmartLog.e(LOG_TAG, "Rotation angles are the same!");
                            sub.onNext(file);
                            sub.onCompleted();
                            return;
                        }

                        final File ffmpegOutputFile = new File(getTempStoragePath(), ROTATED_FILE_FILTER + file.getName());

                        String command = helper.getRotationCommand(file.getAbsolutePath(), currentRotation - expectedRotation, ffmpegOutputFile.getAbsolutePath());

                        SmartLog.d(LOG_TAG, String.format("FFmpeg command: %s ", command));

                        String[] cmdList = command.split("\t");

                        if (ffmpeg.isFFmpegCommandRunning()) {
                            killFFmpegProcess();
                        }

                        try {
                            ffmpeg.execute(cmdList, new ExecuteBinaryResponseHandler() {

                                @Override
                                public void onSuccess(String message) {
                                    SmartLog.d(LOG_TAG, "onSuccess " + message);
                                    sub.onNext(ffmpegOutputFile);
                                    sub.onCompleted();
                                }

                                @Override
                                public void onProgress(String message) {
                                    SmartLog.d(LOG_TAG, "onProgress " + message);
                                }

                                @Override
                                public void onFailure(String message) {
                                    SmartLog.e(LOG_TAG, "onFailure " + message);
                                    sub.onError(new Throwable(message));
                                }

                                @Override
                                public void onFinish() {
                                    SmartLog.d(LOG_TAG, "onFinish ");
                                    sub.onCompleted();
                                }

                            });
                        } catch (FFmpegCommandAlreadyRunningException e) {
                            SmartLog.e(LOG_TAG, "Could not rotate media " + e.getMessage());
                            e.printStackTrace();
                            sub.onError(e);
                        }

                    }
                });
    }

    @Override
    public Observable<Void> cancel() {
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        if (ffmpeg.isFFmpegCommandRunning()) {
                            killFFmpegProcess();
                        }
                        sub.onCompleted();
                    }
                }
        );
    }

    @Override
    public Observable<Long> getVideoDurationMillisec(final File file, final boolean stopTranscoding) {
        SmartLog.i(LOG_TAG, "getVideoDurationMillisec " + file.getAbsolutePath());
        return Observable.create(
                new Observable.OnSubscribe<Long>() {

                    @Override
                    public void call(final Subscriber<? super Long> sub) {
                        SmartLog.d(LOG_TAG, String.format("getVideoDurationMillisec \n " +
                                "file: %s\n", file.getAbsolutePath()));

                        if (file == null) {
                            SmartLog.e(LOG_TAG, "File should not be null!");
                            sub.onError(new IllegalArgumentException("File should not be null!"));
                            return;
                        }

                        if (!stopTranscoding && isTranscodingRunning) {
                            SmartLog.e(LOG_TAG, "Method should not be called while transcoding is running!");
                            sub.onError(new IllegalStateException("Method should not be called while transcoding is running!"));
                            return;
                        }

                        String command = helper.getVideoDurationCommand(file.getAbsolutePath());

                        SmartLog.d(LOG_TAG, String.format("FFmpeg command: %s ", command));

                        String[] cmdList = command.split("\t");

                        if (ffmpeg.isFFmpegCommandRunning()) {
                            killFFmpegProcess();
                        }

                        try {
                            ffmpeg.execute(cmdList, new ExecuteBinaryResponseHandler() {
                                public boolean ignoreError = false;

                                @Override
                                public void onSuccess(String message) {
                                    SmartLog.i(LOG_TAG, "getVideoDurationMillisec onSuccess " + message);
                                    sub.onCompleted();
                                }

                                @Override
                                public void onProgress(String message) {
                                    SmartLog.d(LOG_TAG, "onProgress " + message);
                                    final Long duration = extractDuration(message);
                                    if (duration != null) {
                                        sub.onNext(duration);
                                        ignoreError = true;
                                    }

                                }

                                @Override
                                public void onFailure(String message) {
                                    extractDuration(message);
                                    SmartLog.e(LOG_TAG, "onFailure " + message);
                                    SmartLog.d(LOG_TAG, "ignore error: " + ignoreError);
                                    if (!ignoreError) {
                                        sub.onError(new Throwable(message));
                                    } else {
                                        onSuccess(message);
                                    }
                                }

                                @Override
                                public void onFinish() {
                                    SmartLog.i(LOG_TAG, "getVideoDurationMillisec onFinish ");
                                    sub.onCompleted();
                                }

                                public Long extractDuration(String msg) {
                                    final String durationKey = "Duration: ";
                                    if (msg.contains(durationKey)) {
                                        msg = msg.substring(msg.indexOf(durationKey) + durationKey.length());
                                        msg = msg.substring(0, msg.indexOf(','));
                                        SmartLog.d(LOG_TAG, "extractDuration " + msg);
                                        String timeRegex = "([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])(?:\\.([0-9]{1,3}))?";
                                        Pattern pattern = Pattern.compile(timeRegex);
                                        Matcher matcher = pattern.matcher(msg);
                                        if (matcher.matches()) {
                                            String hours = matcher.group(1);
                                            String minutes = matcher.group(2);
                                            String seconds = matcher.group(3);
                                            String miliSeconds = matcher.group(4);

                                            long hoursMillisec = Integer.parseInt(hours) * 60 * 60 * 1000;
                                            long minutesMillisec = Integer.parseInt(minutes) * 60 * 1000;
                                            long secondsMillisec = Integer.parseInt(seconds) * 1000;
                                            double millisec = Double.parseDouble("0." + miliSeconds) * 1000;

                                            SmartLog.e(LOG_TAG, hours + ", " + minutes + ", " + seconds + ", " + miliSeconds);

                                            return (long) (hoursMillisec + minutesMillisec + secondsMillisec + millisec);

                                        }
                                    }
                                    return null;
                                }

                            });
                        } catch (FFmpegCommandAlreadyRunningException e) {
                            e.printStackTrace();
                            sub.onError(e);
                        }

                    }
                });
    }

    @Override
    public Observable<Bitmap> getVideoThumbnail(final File file, final boolean stopTranscoding) {
        return Observable.create(
                new Observable.OnSubscribe<Bitmap>() {

                    @Override
                    public void call(final Subscriber<? super Bitmap> sub) {
                        SmartLog.d(LOG_TAG, String.format("getVideoThumbnail \n " +
                                "file: %s\n", file.getAbsolutePath()));

                        if (file == null) {
                            SmartLog.e(LOG_TAG, "File should not be null!");
                            sub.onError(new IllegalArgumentException("File should not be null!"));
                            return;
                        }

                        if (!stopTranscoding && isTranscodingRunning) {
                            SmartLog.e(LOG_TAG, "Method should not be called while transcoding is running!");
                            sub.onError(new IllegalStateException("Method should not be called while transcoding is running!"));
                            return;
                        }

                        String nameWithoutExtension = getNameWithoutExtension(file.getAbsolutePath());
                        final File outFile = new File(getTempStorage(), nameWithoutExtension + "_thumb." + THUMB_EXTENSION);

                        SmartLog.d(LOG_TAG, "output file: " + outFile.getAbsolutePath());

                        if (outFile.exists() && !(
                                nameWithoutExtension.equals(PLAYLIST_FILE_NAME)
                                        || nameWithoutExtension.equals(AUDIO_FILE_NAME)
                                        || nameWithoutExtension.equals(DEFAULT_CONVERTED_VIDEO_FILE_NAME)
                        )) {
                            SmartLog.d(LOG_TAG, "Thumbnail was already loaded! ");
                            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                            sub.onNext(BitmapFactory.decodeFile(outFile.getAbsolutePath(), bmOptions));
                            sub.onCompleted();
                            return;
                        }

                        String command = helper.getThumbnailDecodingCommand(file.getAbsolutePath(), outFile.getAbsolutePath());

                        SmartLog.d(LOG_TAG, String.format("FFmpeg command: %s ", command));

                        String[] cmdList = command.split("\t");

                        if (ffmpeg.isFFmpegCommandRunning()) {
                            killFFmpegProcess();
                        }

                        try {
                            ffmpeg.execute(cmdList, new ExecuteBinaryResponseHandler() {

                                @Override
                                public void onSuccess(String message) {
                                    SmartLog.i(LOG_TAG, "getVideoThumbnail onSuccess " + message);
                                    if (outFile.exists()) {
                                        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                                        sub.onNext(BitmapFactory.decodeFile(outFile.getAbsolutePath(), bmOptions));
                                        sub.onCompleted();
                                    } else {
                                        sub.onError(new Throwable("Image was not created!"));
                                    }
                                }

                                @Override
                                public void onProgress(String message) {
                                    SmartLog.d(LOG_TAG, "getVideoThumbnail onProgress " + message);
                                }

                                @Override
                                public void onFailure(String message) {
                                    SmartLog.e(LOG_TAG, "getVideoThumbnail onFailure " + message);
                                    sub.onError(new Throwable(message));
                                }

                                @Override
                                public void onFinish() {
                                    SmartLog.i(LOG_TAG, "getVideoThumbnail onFinish ");
                                    sub.onCompleted();
                                }

                            });
                        } catch (FFmpegCommandAlreadyRunningException e) {
                            SmartLog.e(LOG_TAG, "FFmpegCommandAlreadyRunningException " + e.getMessage());
                            e.printStackTrace();
                            sub.onError(e);
                        }
                    }
                });
    }

    @Override
    public Observable<Bitmap> getVideoThumbnail(final String sourceUrl, final boolean stopTranscoding) {
        return Observable.create(
                new Observable.OnSubscribe<Bitmap>() {

                    @Override
                    public void call(final Subscriber<? super Bitmap> sub) {
                        SmartLog.d(LOG_TAG, String.format("getVideoThumbnail \n " +
                                "file: %s\n", sourceUrl));

                        if (sourceUrl == null) {
                            SmartLog.e(LOG_TAG, "Url should not be null!");
                            sub.onError(new IllegalArgumentException("File should not be null!"));
                            return;
                        }

                        if (!stopTranscoding && isTranscodingRunning) {
                            SmartLog.e(LOG_TAG, "Method should not be called while transcoding is running!");
                            sub.onError(new IllegalStateException("Method should not be called while transcoding is running!"));
                            return;
                        }

                        final File outFile = new File(getTempStorage(), getNameWithoutExtension(sourceUrl) + "_thumb." + THUMB_EXTENSION);

                        SmartLog.d(LOG_TAG, "output file: " + outFile.getAbsolutePath());

                        if (outFile.exists()) {
                            SmartLog.d(LOG_TAG, "Thumbnail was already loaded! ");
                            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                            sub.onNext(BitmapFactory.decodeFile(outFile.getAbsolutePath(), bmOptions));
                            sub.onCompleted();
                            return;
                        }

                        String command = helper.getThumbnailDecodingCommand(sourceUrl, outFile.getAbsolutePath());

                        SmartLog.d(LOG_TAG, String.format("FFmpeg command: %s ", command));

                        String[] cmdList = command.split("\t");

                        if (ffmpeg.isFFmpegCommandRunning()) {
                            killFFmpegProcess();
                        }

                        try {
                            ffmpeg.execute(cmdList, new ExecuteBinaryResponseHandler() {

                                @Override
                                public void onSuccess(String message) {
                                    SmartLog.i(LOG_TAG, "getVideoThumbnail onSuccess " + message);
                                    if (outFile.exists()) {
                                        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                                        sub.onNext(BitmapFactory.decodeFile(outFile.getAbsolutePath(), bmOptions));
                                        sub.onCompleted();
                                    } else {
                                        sub.onError(new Throwable("Image was not created!"));
                                    }
                                }

                                @Override
                                public void onProgress(String message) {
                                    SmartLog.d(LOG_TAG, "getVideoThumbnail onProgress " + message);
                                }

                                @Override
                                public void onFailure(String message) {
                                    SmartLog.e(LOG_TAG, "getVideoThumbnail onFailure " + message);
                                    sub.onError(new Throwable(message));
                                }

                                @Override
                                public void onFinish() {
                                    SmartLog.i(LOG_TAG, "getVideoThumbnail onFinish ");
                                    sub.onCompleted();
                                }

                            });
                        } catch (FFmpegCommandAlreadyRunningException e) {
                            SmartLog.e(LOG_TAG, "FFmpegCommandAlreadyRunningException " + e.getMessage());
                            e.printStackTrace();
                            sub.onError(e);
                        }
                    }
                });
    }

    @Override
    public Observable<ExtendedMediaDescription> getExtendedVideoDescription(final MediaDescription mediaDescription, final boolean stopTranscoding) {
        SmartLog.i(LOG_TAG, "getExtendedVideoDescription ");
        if (mediaDescription instanceof ExtendedMediaDescription) {
            SmartLog.d(LOG_TAG, "abort getExtendedVideoDescription");
            return Observable.just((ExtendedMediaDescription) mediaDescription);
        }

        return Observable.create(
                new Observable.OnSubscribe<ExtendedMediaDescription>() {

                    @Override
                    public void call(final Subscriber<? super ExtendedMediaDescription> sub) {
                        String input = null;

                        if (mediaDescription.getSourceUrl() != null) {
                            input = mediaDescription.getSourceUrl();
                        } else if (mediaDescription.getFile() != null) {
                            input = mediaDescription.getFile().getAbsolutePath();
                        } else {
                            SmartLog.e(LOG_TAG, "Source file or url should not be null!");
                            sub.onError(new IllegalArgumentException("Source file or url should not be null!"));
                            return;
                        }

                        SmartLog.d(LOG_TAG, String.format("getVideoDurationMillisec \n " +
                                "input: %s\n", input));

                        if (!stopTranscoding && isTranscodingRunning) {
                            SmartLog.e(LOG_TAG, "Method should not be called getVideoDurationMillisec while transcoding is running!");
                            sub.onError(new IllegalStateException("Method should not be called getVideoDurationMillisec while transcoding is running!"));
                            return;
                        }

                        String command = helper.getVideoInfoCommand(input);

                        SmartLog.d(LOG_TAG, String.format("FFmpeg command: %s ", command));

                        String[] cmdList = command.split("\t");

                        if (ffmpeg.isFFmpegCommandRunning()) {
                            killFFmpegProcess();
                        }
                        final ExtendedMediaDescription description = new ExtendedMediaDescription(mediaDescription);

                        try {
                            ffmpeg.execute(cmdList, new ExecuteBinaryResponseHandler() {

                                public boolean ignoreError = false;

                                @Override
                                public void onSuccess(String message) {
                                    SmartLog.d(LOG_TAG, "onSuccess " + message);
                                    sub.onNext(description);
                                    sub.onCompleted();
                                }

                                @Override
                                public void onProgress(String message) {
                                    SmartLog.d(LOG_TAG, "onProgress " + message);
                                    Long duration = extractDuration(message);
                                    if (duration != null) {
                                        description.setDuration(duration);
                                        ignoreError = true;

                                        Long bitrate = extractBitrate(message);

                                        if (bitrate != null) {
                                            description.setBitrate(bitrate);
                                        }

                                        SmartLog.i(LOG_TAG, "bitrate: " + bitrate);

                                        return;
                                    }


                                    Pair<String, String> videoData = extractStream(message, "Video");
                                    String streamIndex = videoData.first;
                                    String stream = videoData.second;
                                    if (stream != null) {
                                        description.addVideoStream(streamIndex, stream);
                                        SmartLog.i(LOG_TAG, "Extracted video stream: " + streamIndex + " : " + stream);

                                        Pair<Integer, Integer> resolution = extractResolution(message);

                                        SmartLog.i(LOG_TAG, "Extracted resolution " + resolution.first + " x " + resolution.second);

                                        description.setWidth(resolution.first);
                                        description.setHeight(resolution.second);

                                        return;
                                    }

                                    Pair<String, String> audioData = extractStream(message, "Audio");
                                    streamIndex = audioData.first;
                                    stream = audioData.second;
                                    if (stream != null) {
                                        description.addAudioStream(streamIndex, stream);
                                        SmartLog.i(LOG_TAG, "Extracted audio stream: " + streamIndex + " : " + stream);
                                        return;
                                    }

                                    Integer rotation = extractRotation(message);
                                    if (rotation != null) {
                                        description.setRotation(rotation);
                                        SmartLog.i(LOG_TAG, "Extracted rotation: " + rotation);
                                        return;
                                    }
                                }

                                @Override
                                public void onFailure(String message) {
                                    extractDuration(message);
                                    SmartLog.e(LOG_TAG, "onFailure " + message);
                                    SmartLog.d(LOG_TAG, "ignore error: " + ignoreError);

                                    message = "Unable to get video metadata!";

                                    if (!ignoreError) {
                                        sub.onError(new Throwable(message));
                                    } else {
                                        onSuccess(message);
                                    }
                                }

                                @Override
                                public void onFinish() {
                                    sub.onCompleted();
                                }

                                private Integer extractRotation(String message) {
                                    Integer rotation = null;
                                    if (message.contains("rotate")) {
                                        final String splitKey = ": ";

                                        int index = message.indexOf(splitKey);

                                        if (index != -1) {
                                            String rotationPart = message.split(splitKey)[1];
                                            rotation = Integer.parseInt(rotationPart.replace(" ", ""));
                                        }
                                    }
                                    return rotation;
                                }

                                private Pair<Integer, Integer> extractResolution(String message) {
                                    Integer width = null, height = null;


                                    Pattern pattern = Pattern.compile("\\d{3,4}x\\d{3,4}");
                                    Matcher matcher = pattern.matcher(message);
                                    while (matcher.find()) {
                                        String resolution = matcher.group();

                                        if (!resolution.isEmpty() && resolution.contains("x")) {
                                            String[] parts = resolution.split("x");

                                            try {
                                                width = Integer.parseInt(parts[0]);
                                                height = Integer.parseInt(parts[1]);

                                                if (width > 0 && height > 0) {
                                                    break;
                                                }
                                            } catch (Exception ex) {
                                                // skip
                                            }
                                        }
                                    }

                                    return new Pair<Integer, Integer>(width, height);
                                }

                                private Pair<String, String> extractStream(String message, String type) {
                                    String indexStr = null;
                                    String stream = null;

                                    final String streamKey = "Stream #";

                                    final String splitKey = type + ": ";

                                    int index = message.indexOf(splitKey);

                                    if (index != -1) {
                                        int start = message.indexOf('#') + 1;
                                        indexStr = message
                                                .substring(start,
                                                        start + 3);

                                        String streamPart = message.split(splitKey)[1];
                                        stream = streamPart.substring(0, streamPart.indexOf(","));
                                        stream = stream.replace(" ", "");
                                        int streamInfoIndex = stream.indexOf("(");
                                        if (streamInfoIndex != -1) {
                                            stream = stream.substring(0, streamInfoIndex);
                                        }
                                    }
                                    return new Pair(indexStr, stream);
                                }

                                private Long extractDuration(String msg) {
                                    final String durationKey = "Duration: ";
                                    if (msg.contains(durationKey)) {
                                        msg = msg.substring(msg.indexOf(durationKey) + durationKey.length());
                                        msg = msg.substring(0, msg.indexOf(','));
                                        SmartLog.d(LOG_TAG, "extractDuration " + msg);
                                        String timeRegex = "([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])(?:\\.([0-9]{1,3}))?";
                                        Pattern pattern = Pattern.compile(timeRegex);
                                        Matcher matcher = pattern.matcher(msg);
                                        if (matcher.matches()) {
                                            String hours = matcher.group(1);
                                            String minutes = matcher.group(2);
                                            String seconds = matcher.group(3);
                                            String miliSeconds = matcher.group(4);

                                            long hoursMillisec = Integer.parseInt(hours) * 60 * 60 * 1000;
                                            long minutesMillisec = Integer.parseInt(minutes) * 60 * 1000;
                                            long secondsMillisec = Integer.parseInt(seconds) * 1000;
                                            double millisec = Double.parseDouble("0." + miliSeconds) * 1000;

                                            SmartLog.e(LOG_TAG, hours + ", " + minutes + ", " + seconds + ", " + miliSeconds);

                                            return (long) (hoursMillisec + minutesMillisec + secondsMillisec + millisec);

                                        }
                                    }
                                    return null;
                                }

                                private Long extractBitrate(String msg) {
                                    final String durationKey = "Duration: ";
                                    if (msg.contains(durationKey)) {
                                        String bitrateKey = "bitrate: ";
                                        String[] split = msg.split(bitrateKey);
                                        if (split.length == 2) {
                                            String[] splitBitrate = split[1].split(" ");
                                            if (splitBitrate.length == 2) {
                                                String bitrate = splitBitrate[0];
                                                String bitrateMetrics = splitBitrate[1];
                                                if (bitrateMetrics.contains("kb/s")) {
                                                    return Long.parseLong(bitrate);
                                                } else {
                                                    SmartLog.e(LOG_TAG, "bitrate value not found");
                                                }
                                            } else {
                                                SmartLog.e(LOG_TAG, "bitrate msg not found");
                                            }
                                        } else {
                                            SmartLog.e(LOG_TAG, "bitrate key not found");
                                        }
                                    }
                                    return null;
                                }

                            });
                        } catch (
                                FFmpegCommandAlreadyRunningException e)

                        {
                            e.printStackTrace();
                            sub.onError(e);
                        }

                    }
                });
    }

    @Override
    public Observable<Void> cleanTemporaryFiles(final String[] filters) {
        SmartLog.d(LOG_TAG, "cleanTemporaryFiles");
        return Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(final Subscriber<? super Void> sub) {
                        SmartLog.d(LOG_TAG, String.format("cleanTemporaryFiles from %s" +
                                "\n isTranscodingRunning: %b", getTempStoragePath(), isTranscodingRunning));

                        if (ffmpeg.isFFmpegCommandRunning()) {
                            SmartLog.d(LOG_TAG, "FFmpeg is still running, we should kill the current process before cleaning!");
                            killFFmpegProcess();
                        }

                        totalDeleted = 0;

                        deleteRecursive(getTempStoragePath(), filters);

                        SmartLog.d(LOG_TAG, "all temporary files were deleted!");
                        SmartLog.d(LOG_TAG, "the size of the deleted files: " + getFileSize(totalDeleted));

                        sub.onNext(null);
                        sub.onCompleted();
                    }
                }
        );
    }

    @Override
    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public void setReadyStateDelay(long seconds) {
        this.readyStateDelay = seconds;
    }

    @Override
    public boolean isTranscodingRunning() {
        return isTranscodingRunning;
    }

    @Override
    public File getTempStorage() {
        return getTempStoragePath();
    }

    @Override
    public long getExpectedTranscodingSpace(File file) {
        return (long) (file.length() * REQUIRED_TRANSCODING_SPACE_MULTIPLIER);
    }

    @Override
    public void setVideoPreset(int preset) {
        this.videoPreset = preset;
    }

    //  <editor-fold desc="private">

    private void transcodeMedia(final String inputUrl, String subtitleUrl, final OutputFormat outputFormat,
                                final TranscodingProgressListener progressListener, String... streams) throws IllegalArgumentException, IllegalStateException {
        SmartLog.d(LOG_TAG, String.format("transcodeMedia \n " +
                        "input: %s\n" +
                        "outputFormat: %s",
                inputUrl, outputFormat.name()));

        if (streams != null) {
            SmartLog.d(LOG_TAG, "streams:");
            for (String stream : streams) {
                SmartLog.d(LOG_TAG, "stream: " + stream);
            }
        }

        if (!prepared) {
            progressListener.onFailure(new IllegalStateException("Transcoding provider is not prepared!"));
            return;
        }

        final File tempDir = getTempStoragePath();

        final String path = tempDir.getAbsolutePath();

        final String outFileName;
        final String outFileMimeType;
        final String outFileExtension;

        if (outputFormat == OutputFormat.M4A) {
            outFileName = AUDIO_FILE_NAME;
            outFileMimeType = AUDIO_MIME_TYPE;
            outFileExtension = AUDIO_EXTENSION;
        } else if (outputFormat == OutputFormat.M3U8) {
            outFileName = PLAYLIST_FILE_NAME;
            outFileMimeType = PLAYLIST_MIME_TYPE;
            outFileExtension = PLAYLIST_EXTENSION;
        } else if (outputFormat == OutputFormat.AUDIO_MP4) {
            outFileName = AUDIO_FILE_NAME;
            outFileMimeType = AUDIO_MIME_TYPE;
            outFileExtension = AUDIO_EXTENSION_MP4;
        } else if (outputFormat == OutputFormat.MP4) {
            outFileName = DEFAULT_CONVERTED_VIDEO_FILE_NAME;
            outFileMimeType = MP4_CONVERTED_VIDEO_MIME_TYPE;
            outFileExtension = MP4_CONVERTED_VIDEO_EXTENSION;
        } else if (outputFormat == OutputFormat.MPEG) {
            outFileName = DEFAULT_CONVERTED_VIDEO_FILE_NAME;
            outFileMimeType = MPEG_CONVERTED_VIDEO_MIME_TYPE;
            outFileExtension = MPEG_CONVERTED_VIDEO_EXTENSION;
        } else {
            outFileName = DEFAULT_CONVERTED_VIDEO_FILE_NAME;
            outFileMimeType = DEFAULT_CONVERTED_VIDEO_MIME_TYPE;
            outFileExtension = DEFAULT_CONVERTED_VIDEO_EXTENSION;
        }

        final File ffmpegOutputFile = new File(path, outFileName + "." + outFileExtension);

        if (ffmpegOutputFile.exists()) {
            boolean deleted = ffmpegOutputFile.delete();
            if (!deleted) {
                progressListener.onFailure(new IllegalStateException("Can not delete previous playlist!"));
                return;
            }
        }

        final String outputPlaylistItemName = helper.getProperFileName(getNameWithoutExtension(inputUrl));

        SmartLog.d(LOG_TAG, "Item name " + outputPlaylistItemName);

        SmartLog.d(LOG_TAG, "playlist path  " + ffmpegOutputFile.getAbsolutePath());

        String command;

        if (outputFormat == OutputFormat.M4A) {
            command = helper.getM4AConvertCommand(inputUrl, ffmpegOutputFile);
        } else if (outputFormat == OutputFormat.AUDIO_MP4) {
            command = helper.getMP4ConvertCommand(inputUrl, ffmpegOutputFile);
        } else if (outputFormat == OutputFormat.MP4) {
            command = helper.getFullTranscodingCommandMP4(inputUrl, ffmpegOutputFile, subtitleUrl, videoPreset);
        } else if (outputFormat == OutputFormat.MPEG) {
            command = helper.getFullTranscodingCommandMPEG(inputUrl, ffmpegOutputFile, subtitleUrl, videoPreset);
        } else if (outputFormat == OutputFormat.TS) {
            command = helper.getFullTranscodingCommand(inputUrl, ffmpegOutputFile, subtitleUrl, videoPreset);
        } else {
            command = helper.getCreateHlsPlaylistCommand(inputUrl, ffmpegOutputFile, path,
                    outputPlaylistItemName, subtitleUrl, streams, videoPreset);
        }

        SmartLog.d(LOG_TAG, String.format("FFmpeg command: %s \n wait for finish: %b",
                command, readyStateDelay == DELAY_WAIT_FOR_FINISH));


        String[] cmdList = command.split("\t");

        if (ffmpeg.isFFmpegCommandRunning()) {
            SmartLog.d(LOG_TAG, "FFmpeg is still running, we should kill the current process!");
            killFFmpegProcess();
        }

        try {
            ffmpeg.execute(cmdList, new ExecuteBinaryResponseHandler() {
                public boolean outputFileReady;

                @Override
                public void onSuccess(String message) {
                    SmartLog.d(LOG_TAG, "transcodeMedia onSuccess " + message);
                    if (ffmpegOutputFile.exists()) {
                        TranscodingProgressImpl progress = new TranscodingProgressImpl(message);
                        progress.setOutputFile(ffmpegOutputFile);
                        progress.setOutputFileMimeType(outFileMimeType);
                        progress.setFinished(true);
                        progress.setAudioStream(MediaTypes.AUDIO_CODEC_AAC);
                        progress.setVideoStream(outputFormat == OutputFormat.M4A ? null : MediaTypes.VIDEO_CODEC_H264);
                        progressListener.onProgress(progress);
                    } else {
                        progressListener.onFailure(new Throwable("Could not generate playlist file!"));
                    }
                }

                @Override
                public void onProgress(String message) {
                    SmartLog.d(LOG_TAG, "transcodeMedia onProgress " + message);

                    if (!message.contains("time=")) {
                        return;
                    }

                    TranscodingProgressImpl progress = new TranscodingProgressImpl(message);

                    SmartLog.d(LOG_TAG, String.format("progress:" +
                                    "\n seconds: %s" +
                                    "\n fps: %s",
                            progress.getSeconds(), progress.getFps()));

                    if (ffmpegOutputFile.exists()) {

                        if (readyStateDelay != DELAY_WAIT_FOR_FINISH && outputFormat == OutputFormat.M3U8) {
                            try {
                                //we can now prepare output file
                                if (!outputFileReady) {
                                    outputFileReady = readyStateDelay < progress.getSeconds();

                                    if (outputFormat == OutputFormat.M3U8) {
                                        outputFileReady = outputFileReady && isPlaylistReady(ffmpegOutputFile, outputPlaylistItemName);
                                    }

                                    if (outputFileReady) {
                                        SmartLog.d(LOG_TAG, "output file is ready!");
                                        //file is ready to cast!
                                        progress.setOutputFile(ffmpegOutputFile);
                                    }
                                }
                            } catch (IOException e) {
                                SmartLog.d(LOG_TAG, "onProgress IOException " + e.getMessage());
                                e.printStackTrace();
                                progressListener.onFailure(new Throwable("Could not update playlist! " + e.getMessage()));
                                return;
                            }
                        }
                    }

                    progress.setOutputFileMimeType(outFileMimeType);
                    progress.setAudioStream(MediaTypes.AUDIO_CODEC_AAC);
                    progress.setVideoStream(outputFormat == OutputFormat.M4A ? null : MediaTypes.VIDEO_CODEC_H264);
                    progress.setFinished(false);

                    progressListener.onProgress(progress);
                }

                @Override
                public void onFailure(String message) {
                    SmartLog.d(LOG_TAG, "transcodeMedia onFailure " + message);

                    SmartLog.d(LOG_TAG, "free space on disk: " + getFileSize(tempDir.getFreeSpace()));
                    if (tempDir.getFreeSpace() < MIN_AVAILABLE_SPACE_THRESHOLD_BYTES) {
                        SmartLog.d(LOG_TAG, "threshold " + getFileSize(MIN_AVAILABLE_SPACE_THRESHOLD_BYTES));
                        message = "Media conversion failed. Not enough available space on the sdcard!";
                    } else {
                        message = String.format("Unable to transcode '%s' to the supported format.", getNameWithoutExtension(inputUrl));
                    }

                    progressListener.onFailure(new Throwable(message));

                    cleanTemporaryFiles(null)
                            .subscribe(new Subscriber<Void>() {
                                @Override
                                public void onCompleted() {

                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onNext(Void aVoid) {

                                }
                            });
                }

                @Override
                public void onFinish() {
                    SmartLog.d(LOG_TAG, "transcodeMedia onFinish ");
                    progressListener.onFinished();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
            SmartLog.e(LOG_TAG, "FFmpegCommandAlreadyRunningException " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    private void transcodeMediaSegmented(String inputUrl, String subtitleUrl, final int segmentsNum, final TranscodingProgressListener progressListener) throws IllegalArgumentException, IllegalStateException {
        SmartLog.d(LOG_TAG, String.format("transcodeMediaSegmented \n " +
                "input: %s\n" +
                "segmentsNum: %d", inputUrl, segmentsNum));

        if (!prepared) {
            progressListener.onFailure(new IllegalStateException("Transcoding provider is not prepared!"));
            return;
        }

        final File tempDir = getTempStoragePath();

        final String path = tempDir.getAbsolutePath();

        final String outFileName = DEFAULT_CONVERTED_VIDEO_FILE_NAME;
        final String outFileMimeType = DEFAULT_CONVERTED_VIDEO_MIME_TYPE;
        final String outFileExtension = DEFAULT_CONVERTED_VIDEO_EXTENSION;

        final String outputPlaylistItemName = helper.getProperFileName(getNameWithoutExtension(inputUrl));

        SmartLog.d(LOG_TAG, "Item name " + outputPlaylistItemName);

        String command;

        command = helper.getTranscodeAndSplitCommand(inputUrl, path, outputPlaylistItemName, TRANSCODING_SEGMENT_TIME, subtitleUrl);

        SmartLog.d(LOG_TAG, String.format("FFmpeg command: %s ", command));

        String[] cmdList = command.split("\t");

        if (ffmpeg.isFFmpegCommandRunning()) {
            SmartLog.d(LOG_TAG, "FFmpeg is still running, we should kill the current process!");
            killFFmpegProcess();
        }

        try {
            ffmpeg.execute(cmdList, new ExecuteBinaryResponseHandler() {

                @Override
                public void onSuccess(String message) {
                    SmartLog.d(LOG_TAG, "transcodeMediaSegmented onSuccess " + message);

                    TranscodingSegmentedProgressImpl progress = new TranscodingSegmentedProgressImpl(message);
                    progress.setFilePattern(path + "/" + outputPlaylistItemName + "_" + "%d.ts");
                    progress.setSegmentsCount(segmentsNum);
                    progress.setFinished(true);

                    if (progress.getSegmentFile(0).exists()) {
                        progress.setReady(true);
                        progressListener.onProgress(progress);
                    } else {
                        progressListener.onFailure(new Throwable("Could not generate converted file!"));
                    }

                }

                @Override
                public void onProgress(String message) {
                    SmartLog.d(LOG_TAG, "transcodeMediaSegmented onProgress " + message);

                    if (!message.contains("time=")) {
                        return;
                    }

                    //TODO make common with FFMPEGHelper
                    TranscodingSegmentedProgressImpl progress = new TranscodingSegmentedProgressImpl(message);
                    progress.setFilePattern(path + "/" + outputPlaylistItemName + "_" + "%d.ts");
                    progress.setSegmentsCount(segmentsNum);

                    SmartLog.d(LOG_TAG, String.format("progress:" +
                                    "\n seconds: %s" +
                                    "\n fps: %s",
                            progress.getSeconds(), progress.getFps()));

                    if (segmentsNum > 1) {
                        final File segmentFile = progress.getSegmentFile(1);
                        SmartLog.d(LOG_TAG, "check segment file 1: " + segmentFile.getAbsolutePath());
                        if (segmentFile.exists()) {
                            SmartLog.d(LOG_TAG, "found first segment file ");
                            progress.setReady(true);
                        }
                    }

                    progress.setOutputFileMimeType(outFileMimeType);
                    progress.setAudioStream(MediaTypes.AUDIO_CODEC_AAC);
                    progress.setVideoStream(MediaTypes.VIDEO_CODEC_H264);
                    progress.setFinished(false);

                    progressListener.onProgress(progress);
                }

                @Override
                public void onFailure(String message) {
                    SmartLog.d(LOG_TAG, "transcodeMediaSegmented onFailure " + message);

                    SmartLog.d(LOG_TAG, "free space on disk: " + getFileSize(tempDir.getFreeSpace()));
                    if (tempDir.getFreeSpace() < MIN_AVAILABLE_SPACE_THRESHOLD_BYTES) {
                        SmartLog.d(LOG_TAG, "threshold " + getFileSize(MIN_AVAILABLE_SPACE_THRESHOLD_BYTES));
                        message = "Media conversion failed. Not enough available space on the sdcard!";
                    }

                    progressListener.onFailure(new Throwable(message));

                    cleanTemporaryFiles(null)
                            .subscribe(new Subscriber<Void>() {
                                @Override
                                public void onCompleted() {

                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onNext(Void aVoid) {

                                }
                            });
                }

                @Override
                public void onFinish() {
                    SmartLog.d(LOG_TAG, "transcodeMediaSegmented onFinish ");
                    progressListener.onFinished();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
            SmartLog.e(LOG_TAG, "FFmpegCommandAlreadyRunningException " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    private void killFFmpegProcess() {
        SmartLog.d(LOG_TAG, "killFFmpegProcess attempt " + killingAttemps);
        if (ffmpeg.isFFmpegCommandRunning() && killingAttemps < MAX_KILLING_ATTEMPTS) {
            boolean killed = ffmpeg.killRunningProcesses();

            //unfortunately, we cannot expect immediate effect from process.destroy
            //so we check is async task was cancelled and wait before
            //executing new ffmpeg command
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                SmartLog.e(LOG_TAG, "Interrupted waiting " + e.getMessage());
                e.printStackTrace();
            }

            if (ffmpeg.isFFmpegCommandRunning()) {
                killingAttemps++;
                killFFmpegProcess();
            } else {
                SmartLog.d(LOG_TAG, "Killed ffmpeg process successfully!");
                isTranscodingRunning = false;
                killingAttemps = 0;
            }

        }
    }

    private boolean isPlaylistReady(File playlistFile, String loadedFileName) throws IOException {
        FileInputStream is = new FileInputStream(playlistFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = null;

        int extInfoNum = 0;
        while ((line = reader.readLine()) != null) {
            if (line.contains(loadedFileName)) {
            } else if (line.contains("EXTINF")) {
                extInfoNum++;
                if (extInfoNum == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getNameWithoutExtension(String inputUrl) {
        return URLUtil.guessFileName(inputUrl, null, null).replaceFirst("[.][^.]+$", "");
    }

    private void deleteRecursive(File fileOrDirectory, String[] filters) {
        if (isTranscodingRunning) {
            SmartLog.e(LOG_TAG, "Transcoding is started, we should skip cleaning now!");
            //transcoding was started while we are cleaning our directories
            //we should stop cleaning process to prevent deleting new playlist files!
            return;
        }

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child, filters);
            }
        } else {
            boolean delete = false;
            if (filters != null) {
                for (String filter : filters) {
                    if (fileOrDirectory.getName().endsWith(filter) || fileOrDirectory.getName().startsWith(ROTATED_FILE_FILTER)) {
                        delete = true;
                    }
                }
            } else {
                delete = true;
            }
            if (delete) {
                SmartLog.d(LOG_TAG, "delete " + fileOrDirectory.getName());
                totalDeleted += fileOrDirectory.length();
                fileOrDirectory.delete();
            }
        }

    }

    private File getTempStoragePath() {
        String path;
        if (!TextUtils.isEmpty(storageDir)) {
            path = storageDir;
        } else {
            File dir = helper.ffmpegDir();
            path = dir.getAbsolutePath();
        }

        File tempDir = new File(path);
        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();
            if (!created) {
                throw new IllegalArgumentException("Could not create storage directory!");
            }
        }
        return tempDir;
    }

    private static String getFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void printCodecsInfo(final LoadBinaryResponseHandler handler) {
        String command = helper.getCodecsCommand();

        SmartLog.d(LOG_TAG, String.format("FFmpeg command: %s ", command));

        String[] cmdList = command.split("\t");

        if (ffmpeg.isFFmpegCommandRunning()) {
            SmartLog.d(LOG_TAG, "FFmpeg is still running, we should kill the current process!");
            killFFmpegProcess();
        }

        try {
            ffmpeg.execute(cmdList, new ExecuteBinaryResponseHandler() {

                @Override
                public void onSuccess(String message) {
                    SmartLog.i(LOG_TAG, "" + message);
                    handler.onSuccess();
                }

                @Override
                public void onProgress(String message) {
                    SmartLog.d(LOG_TAG, "" + message);
                }

                @Override
                public void onFailure(String message) {
                    SmartLog.e(LOG_TAG, "printCodecsInfo onFailure " + message);
                }

                @Override
                public void onFinish() {
                    SmartLog.i(LOG_TAG, "getVideoThumbnail onFinish ");
                    handler.onFinish();
                }

            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            SmartLog.e(LOG_TAG, "printCodecsInfo exception " + e.getMessage());
            e.printStackTrace();
            handler.onFailure();
        }
    }

//  </editor-fold>

    private interface TranscodingProgressListener<T extends TranscodingProgress> {
        void onProgress(T progress);

        void onFailure(Throwable ex);

        void onFinished();
    }

}
