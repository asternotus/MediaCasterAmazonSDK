package com.megacast.castsdk.internal.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.caverock.androidsvg.SVG;
import com.mega.cast.utils.log.SmartLog;

import android.webkit.MimeTypeMap;

import com.megacast.castsdk.exceptions.InsufficientStorageException;
import com.megacast.castsdk.exceptions.MediaUnsupportedException;
import com.megacast.castsdk.internal.model.RemoteMediaReceiverController;
import com.megacast.castsdk.internal.model.TranscodedMediaDescription;
import com.megacast.castsdk.managers.StreamingManager;
import com.megacast.castsdk.model.AbstractFile;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ExtendedMediaDescription;
import com.megacast.castsdk.model.ImageDescription;
import com.megacast.castsdk.model.MediaController;
import com.megacast.castsdk.model.MediaDescription;
import com.megacast.castsdk.model.SegmentTranscodingProgress;
import com.megacast.castsdk.model.SegmentedMediaController;
import com.megacast.castsdk.model.ServerConnection;
import com.megacast.castsdk.model.StreamingOptions;
import com.megacast.castsdk.model.SubtitleDescription;
import com.megacast.castsdk.model.TranscodingProgress;
import com.megacast.castsdk.model.constants.MediaTypes;
import com.megacast.castsdk.providers.CastManagerProvider;
import com.megacast.castsdk.providers.managers.cast.ImageCastManager;
import com.megacast.castsdk.providers.managers.cast.MediaCastManager;
import com.megacast.castsdk.providers.managers.sharing.MediaSharingManager;
import com.megacast.castsdk.providers.managers.subtitle.SubtitleConversionManager;
import com.megacast.castsdk.providers.managers.transcoding.TranscodingManager;

import org.beyka.tiffbitmapfactory.TiffBitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by Dmitry on 14.09.16.
 */
public class MegacastStreamingManager implements StreamingManager {

    private static final String LOG_TAG = MegacastStreamingManager.class.getSimpleName();

    private ImageCastManager imageCastManager;
    private MediaCastManager mediaCastManager;
    private MediaSharingManager sharingManager;
    private TranscodingManager transcodingManager;
    private SubtitleConversionManager subtitleConversionManager;

    private PublishSubject<TranscodingProgress> transcodingProgressPublisher;

    private StreamingOptions streamingOptions;

    private Context context;

    private boolean storageWarningEnabled = true;
    private boolean allow4k = false; // TODO change to false

    public MegacastStreamingManager(Context context, CastManagerProvider castManagerProvider, MediaSharingManager sharingManager,
                                    TranscodingManager transcodingManager, SubtitleConversionManager subtitleConversionManager) {
        this.context = context;
        this.imageCastManager = castManagerProvider.getImageCastManager();
        this.mediaCastManager = castManagerProvider.getMediaCastManager();
        this.sharingManager = sharingManager;
        this.transcodingManager = transcodingManager;
        this.subtitleConversionManager = subtitleConversionManager;

        transcodingManager.setStorageDir(getTempStorage());
        subtitleConversionManager.setStorageDir(getTempStorage());

        this.transcodingProgressPublisher = PublishSubject.create();
    }

    //  <editor-fold desc="public interface">

    @Override
    public Observable<MediaController> streamVideo(final Device device, final MediaDescription mediaDescription,
                                                   @Nullable final SubtitleDescription subtitleDescription) {
        boolean transcodingForced = streamingOptions.isForceTranscoding() || device.isTranscodingForced();
        return streamMedia(device, mediaDescription, subtitleDescription, false, transcodingForced);
    }

    @Override
    public Observable<MediaController> streamAudio(final Device device, final MediaDescription mediaDescription) {
        boolean transcodingForced = streamingOptions.isForceTranscoding() || device.isTranscodingForced();
        return streamMedia(device, mediaDescription, null, true, transcodingForced);
    }

    @Override
    public Observable<MediaController> streamImage(final Device device, final ImageDescription imageDescription) {
        SmartLog.d(LOG_TAG, String.format("stream image"));

        if (!TextUtils.isEmpty(imageDescription.getCastUrl())) {
            SmartLog.d(LOG_TAG, "image has cast url! " + imageDescription.getCastUrl());
            return imageCastManager.beamImageFile(device, imageDescription);
        }

        // check for abstract "file"
        if (imageDescription.getAbstractFile() != null && imageDescription.getSourceUrl() == null) {
            SmartLog.d(LOG_TAG, "cast abstract file ");
            // abstract file is basically any source of input data as long as it has InputStream
            // so we just have to share it...
            return sharingManager.shareImageFile(device, imageDescription)
                    .flatMap(new Func1<ServerConnection, Observable<MediaController>>() {
                        @Override
                        public Observable<MediaController> call(ServerConnection serverConnection) {
                            SmartLog.d(LOG_TAG, "abstract file was shared! " + imageDescription.getCastUrl());
                            // we are ready to cast media file
                            // by a new source url which was stored in cast url
                            imageDescription.setSourceUrl(imageDescription.getCastUrl());
                            imageDescription.setCastUrl(null);

                            // so, now we are able to pass a new stream to ffmpeg
                            // and continue with the standard cast process
                            return streamImage(device, imageDescription);
                        }
                    });
        }


        File file = imageDescription.getFile();
        String sourceUrl = imageDescription.getSourceUrl();

        if (file == null && TextUtils.isEmpty(sourceUrl)) {
            IllegalArgumentException exception = new IllegalArgumentException("ImageDescription should has either valid remote url or file path!");
            SmartLog.e(LOG_TAG, "IllegalArgumentException " + exception.getMessage());
            return Observable.error(exception);
        }

        final boolean isValidFormat = device.hasSupportedImageType(imageDescription.getMimeType(),
                getExtension(!TextUtils.isEmpty(sourceUrl) ? sourceUrl : file.getAbsolutePath()));
        final boolean imageResolutionSupported = imageDescription.getWidth() > 0 && imageDescription.getHeight() > 0
                && device.isImageResolutionSupported(imageDescription.getWidth(), imageDescription.getHeight());

        SmartLog.d(LOG_TAG, String.format("valid format: %b\n" +
                        "resolution supported: %b",
                isValidFormat, imageResolutionSupported));

        if (imageResolutionSupported && isValidFormat) {

            if (!TextUtils.isEmpty(sourceUrl)) {
                imageDescription.setCastUrl(sourceUrl);
                return streamImage(device, imageDescription);
            } else {
                return sharingManager.shareImageFile(device, imageDescription)
                        .flatMap(new Func1<ServerConnection, Observable<MediaController>>() {
                            @Override
                            public Observable<MediaController> call(ServerConnection serverConnection) {
                                SmartLog.d(LOG_TAG, "image was shared! ");
                                return streamImage(device, imageDescription);
                            }
                        });
            }
        }

        final Observable<ImageDescription> prepareImageObs;
        if (!TextUtils.isEmpty(sourceUrl)) {
            prepareImageObs = prepareImageLink(device, imageDescription);
        } else {
            prepareImageObs = prepareImageFile(device, imageDescription);
        }

        return prepareImageObs
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<ImageDescription, Observable<MediaController>>() {
                    @Override
                    public Observable<MediaController> call(ImageDescription imageDescription) {
                        return streamImage(device, imageDescription);
                    }
                });
    }

    @Override
    public Observable<SubtitleDescription> shareSubtitles(final Device device, final SubtitleDescription subtitleDescription) {
        final boolean hasValidSubtitles = !TextUtils.isEmpty(subtitleDescription.getCastUrl());


        if (!hasValidSubtitles) {
            final File subtitlesFile;

            if (subtitleDescription.getFile() == null && !TextUtils.isEmpty(subtitleDescription.getSourceUrl())) {
                // download file to the temp storage
                SmartLog.d(LOG_TAG, "downloading subtitles file...");

                subtitlesFile = new File(getTempStorage(), "temp_subtitle." + getExtension(subtitleDescription.getSourceUrl()));

                return downloadFile(subtitleDescription.getSourceUrl(), subtitlesFile.getAbsolutePath())
                        .subscribeOn(Schedulers.io())
                        .flatMap(new Func1<File, Observable<SubtitleDescription>>() {
                            @Override
                            public Observable<SubtitleDescription> call(File file) {
                                subtitleDescription.setFile(file);
                                return shareSubtitles(device, subtitleDescription);
                            }
                        });
            } else {
                subtitlesFile = subtitleDescription.getFile();
            }

            if (subtitlesFile == null) {
                IllegalArgumentException exception = new IllegalArgumentException("subtitleDescription should has either valid remote url or file path!");
                SmartLog.e(LOG_TAG, "IllegalArgumentException " + exception.getMessage());

                return Observable.error(exception);
            }

            //check for device-media compatibility
            boolean isSubtitleFormatSupported = device.hasSupportedSubtitleFormat(subtitleDescription.getType());

            if (!isSubtitleFormatSupported) {
                List<Integer> supportedSubtitlesFormats = device.getSupportedSubtitles();

                if (device.getSupportedSubtitles().isEmpty()) {
                    return Observable.error(new Throwable("Could not convert subtitles! Device has empty list of supported subtitle formats!"));
                }

                //pick first supported subtitle type
                return subtitleConversionManager.convert(subtitleDescription, supportedSubtitlesFormats.get(0))
                        .flatMap(new Func1<SubtitleDescription, Observable<SubtitleDescription>>() {
                            @Override
                            public Observable<SubtitleDescription> call(SubtitleDescription convertedDescription) {
                                return shareSubtitles(device, convertedDescription);
                            }
                        });
            }

            return sharingManager.shareSubtitleFile(device, subtitleDescription)
                    .flatMap(new Func1<ServerConnection, Observable<SubtitleDescription>>() {
                        @Override
                        public Observable<SubtitleDescription> call(ServerConnection serverConnection) {
                            SmartLog.d(LOG_TAG, "remote subtitle sourceUrl  " + serverConnection.getUrl());
                            return shareSubtitles(device, subtitleDescription);
                        }
                    });
        } else {
            return Observable.just(subtitleDescription);
        }
    }

    @Override
    public Observable<TranscodingProgress> getTranscodingProgressSubscriber() {
        return transcodingProgressPublisher.asObservable();
    }

    @Override
    public Observable<Void> cleanTemporaryFiles(String[] filters) {
        return transcodingManager.cleanTemporaryFiles(filters);
    }

    @Override
    public void setStreamingOptions(StreamingOptions options) {
        this.streamingOptions = options;

        this.storageWarningEnabled = streamingOptions.getInsufficientStorageWarningEnabled();

        this.allow4k = streamingOptions.isAllow4kSupport();

        transcodingManager.setReadyStateDelay(streamingOptions.getDelayBeforeStart());
        transcodingManager.setStorageDir(streamingOptions.getTempStoragePath());

        sharingManager.setPlaylistAutoPause(streamingOptions.isAutoPause());
        sharingManager.setAutoDeleteServedSegments(streamingOptions.isAutoDeleteServedSegments());

        sharingManager.setDLNAFixEnabled(streamingOptions.isDLNAFixEnabled());
    }

    @Override
    public boolean isTranscodingRunning() {
        return transcodingManager.isTranscodingRunning();
    }


    //  </editor-fold>

    //  <editor-fold desc="private">

    // Scenarios:
    // 1. stream online
    // 2. stream offline file -> supported file type -> local links -> 1.
    // 3. stream offline file -> unsupported file type -> transcoding -> 2.
    private Observable<MediaController> streamMedia(final Device device,
                                                    final MediaDescription mediaDescription,
                                                    @Nullable final SubtitleDescription subtitleDescription,
                                                    final boolean isAudio,
                                                    final boolean transcodingForced) {

        SmartLog.d(LOG_TAG, String.format("stream media:" +
                "\n isAudio: %b" +
                "\n isTranscodingForced: %b", isAudio, transcodingForced));

        final boolean hasValidUrl = !TextUtils.isEmpty(mediaDescription.getCastUrl());
        final boolean hasValidSubtitles = subtitleDescription == null || !TextUtils.isEmpty(subtitleDescription.getCastUrl());

        SmartLog.i(LOG_TAG, "media url check...");

        if (hasValidUrl && hasValidSubtitles && !transcodingForced) {
            SmartLog.d(LOG_TAG, "streamMedia " + mediaDescription.getCastUrl());
            return mediaCastManager.beamMediaFile(device, mediaDescription, subtitleDescription)
                    .onErrorResumeNext(new Func1<Throwable, Observable<? extends MediaController>>() {
                        @Override
                        public Observable<? extends MediaController> call(Throwable throwable) {
                            if (throwable instanceof MediaUnsupportedException) {
                                SmartLog.e(LOG_TAG, "Media unsupported error! We should transcode the media file.");
                                mediaDescription.setCastUrl(null);
                                return streamMedia(device, mediaDescription, subtitleDescription, isAudio, true);
                            }
                            return Observable.error(throwable);
                        }
                    });
        }

        //make valid video url
        final File file = mediaDescription.getFile();
        final String sourceUrl = mediaDescription.getSourceUrl();

        final boolean isUrl = !TextUtils.isEmpty(sourceUrl);

        //media file validation
        if (file == null && TextUtils.isEmpty(mediaDescription.getSourceUrl()) && mediaDescription.getAbstractFile() == null) {
            IllegalArgumentException exception = new IllegalArgumentException("MediaDescription should has either valid remote url or file path!");
            SmartLog.e(LOG_TAG, "IllegalArgumentException " + exception.getMessage());

            return Observable.error(exception);
        }

        if (isUrl && sourceUrl.startsWith("https:")) {
            SmartLog.d(LOG_TAG, "https detected, building abstract file... ");
            try {
                final URL url = new URL(sourceUrl);

                AbstractFile castFile = new AbstractFile() {

                    HttpURLConnection connection;

                    private void init() {
                        try {
                            connection = (HttpURLConnection) url.openConnection();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public String getName() {
                        return mediaDescription.getTitle();
                    }

                    @Override
                    public InputStream getInputSteam() throws IOException {
                        if (connection == null) {
                            init();
                        }
                        return (InputStream) connection.getURL().getContent();
                    }

                    @Override
                    public long length() {
                        if (connection == null) {
                            init();
                        }
                        return connection.getContentLength();
                    }
                };

                mediaDescription.setSourceUrl(null);
                mediaDescription.setAbstractFile(castFile);
            } catch (IOException e) {
                IllegalArgumentException exception = new IllegalArgumentException("Could not open secure connection to " + sourceUrl);
                SmartLog.e(LOG_TAG, "Could not open secure connection to " + sourceUrl);

                return Observable.error(exception);
            }
        }

        // check for abstract "file"
        if (mediaDescription.getAbstractFile() != null && mediaDescription.getSourceUrl() == null) {
            SmartLog.d(LOG_TAG, "cast abstract file ");
            // abstract file is basically any source of input data as long as it has InputStream
            // so we just have to share it...
            return sharingManager.shareMediaFile(device, mediaDescription)
                    .flatMap(new Func1<ServerConnection, Observable<MediaController>>() {
                        @Override
                        public Observable<MediaController> call(ServerConnection serverConnection) {
                            SmartLog.d(LOG_TAG, "abstract file was shared! " + mediaDescription.getCastUrl());
                            // we are ready to cast media file
                            // by a new source url which was stored in cast url
                            mediaDescription.setSourceUrl(mediaDescription.getCastUrl());
                            mediaDescription.setCastUrl(null);

                            // so, now we are able to pass a new stream to ffmpeg
                            // and continue with the standard cast process
                            return streamMedia(device, mediaDescription, subtitleDescription, isAudio, transcodingForced);
                        }
                    });
        }

        //subtitle file validation
        final File subtitlesFile;
        if (subtitleDescription != null && !hasValidSubtitles) {
            SmartLog.d(LOG_TAG, "subtitles are invalid...");

            if (subtitleDescription.getFile() == null && !TextUtils.isEmpty(subtitleDescription.getSourceUrl())) {
                // download file to the temp storage
                SmartLog.d(LOG_TAG, "downloading subtitles file...");

                subtitlesFile = new File(getTempStorage(), "temp_subtitle." + getExtension(subtitleDescription.getSourceUrl()));

                return downloadFile(subtitleDescription.getSourceUrl(), subtitlesFile.getAbsolutePath())
                        .subscribeOn(Schedulers.io())
                        .flatMap(new Func1<File, Observable<MediaController>>() {
                            @Override
                            public Observable<MediaController> call(File file) {
                                subtitleDescription.setFile(file);
                                return streamMedia(device, mediaDescription, subtitleDescription, isAudio, transcodingForced);
                            }
                        });
            } else {
                subtitlesFile = subtitleDescription.getFile();
            }

            if (subtitlesFile == null) {
                IllegalArgumentException exception = new IllegalArgumentException("subtitleDescription should has either valid remote url or file path!");
                SmartLog.e(LOG_TAG, "IllegalArgumentException " + exception.getMessage());

                return Observable.error(exception);
            }

            //check for device-media compatibility
            boolean isSubtitleFormatSupported = device.hasSupportedSubtitleFormat(subtitleDescription.getType());

            if (!isSubtitleFormatSupported) {
                List<Integer> supportedSubtitlesFormats = device.getSupportedSubtitles();

                if (device.getSupportedSubtitles().isEmpty()) {
                    return Observable.error(new Throwable("Could not convert subtitles! Device has empty list of supported subtitle formats!"));
                }

                //pick first supported subtitle type
                return subtitleConversionManager.convert(subtitleDescription, supportedSubtitlesFormats.get(0))
                        .flatMap(new Func1<SubtitleDescription, Observable<MediaController>>() {
                            @Override
                            public Observable<MediaController> call(SubtitleDescription convertedDescription) {
                                return streamMedia(device, mediaDescription, convertedDescription, isAudio, transcodingForced);
                            }
                        });
            }
        } else {
            subtitlesFile = null;
        }

        //obtain media info from ffmpeg
        return transcodingManager.getExtendedVideoDescription(mediaDescription, true)
                .flatMap(new Func1<ExtendedMediaDescription, Observable<MediaController>>() {
                    @Override
                    public Observable<MediaController> call(final ExtendedMediaDescription extendedMediaDescription) {

                        //check for device-media compatibility
                        boolean isMediaCompatible = isMediaCompatible(device, extendedMediaDescription, isAudio);

                        SmartLog.d(LOG_TAG, "is media compatible " + isMediaCompatible);

                        boolean isEmbeddedSubtitlesRequired = subtitlesFile != null && mediaDescription.isOnlyEmbeddedSubtitlesAllowed();

                        int rotation = extendedMediaDescription.getRotation();

                        Integer width = extendedMediaDescription.getWidth();
                        Integer height = extendedMediaDescription.getHeight();

                        int preset = streamingOptions.getDefaultQualityPreset();
                        if (width != null && height != null && width > 0 && height > 0) {
                            if (width < MediaTypes.HD_WIDTH && height < MediaTypes.HD_HEIGHT) {
                                preset = streamingOptions.getDefaultQualityPreset();
                            } else if (width >= MediaTypes.HD_WIDTH && height >= MediaTypes.HD_HEIGHT
                                    && width < MediaTypes.FULL_HD_WIDTH && height < MediaTypes.FULL_HD_HEIGHT) {
                                preset = streamingOptions.getHdQualityPreset();
                            } else if (width <= MediaTypes.FULL_HD_WIDTH && height <= MediaTypes.FULL_HD_HEIGHT) {
                                preset = streamingOptions.getFullHdQualityPreset();
                            } else {
                                preset = TranscodingManager.PRESET_4K;
                            }
                        }

                        if (preset == TranscodingManager.PRESET_4K) {
                            SmartLog.d(LOG_TAG, "is 4k compatible " + allow4k);
                            isMediaCompatible = isMediaCompatible && allow4k;
                            extendedMediaDescription.set4K(true);
                        }

                        mediaDescription.putExtra("width", width);
                        mediaDescription.putExtra("height", height);

                        boolean needsTranscoding = !isMediaCompatible || isEmbeddedSubtitlesRequired || transcodingForced;

                        SmartLog.d(LOG_TAG, "needs transcoding: " + needsTranscoding);
                        SmartLog.d(LOG_TAG, "is embedded subtitles required " + isEmbeddedSubtitlesRequired);
                        SmartLog.d(LOG_TAG, "media rotation " + rotation);

                        // TODO check do we really need it?
//                        if (rotation != 0 && file != null) {
//                            SmartLog.d(LOG_TAG, "rotate video from " + rotation + " angle to 0 angle");
//                            return transcodingManager.rotateMedia(file, rotation, 0)
//                                    .flatMap(new Func1<File, Observable<MediaController>>() {
//                                        @Override
//                                        public Observable<MediaController> call(File file) {
//                                            SmartLog.d(LOG_TAG, "video was rotated!");
//                                            extendedMediaDescription.setRotation(0);
//                                            extendedMediaDescription.setFile(file);
//                                            return streamMedia(device, extendedMediaDescription, extendedMediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? null : subtitleDescription, isAudio, false);
//                                        }
//                                    });
//                        }

                        if (needsTranscoding) {
                            SmartLog.d(LOG_TAG, "file type is not supported, we should pass it to the ffmpeg!");

                            if (storageWarningEnabled && file != null) {
                                long availableSpace = transcodingManager.getTempStorage().getFreeSpace();
                                long requitedSpace = transcodingManager.getExpectedTranscodingSpace(file);

                                if (availableSpace < requitedSpace) {
                                    final String detailedMessage = String.format("Not enough space for conversion of %s\nrequired space: %d\navailable space: %d",
                                            file.getAbsolutePath(), requitedSpace, availableSpace);

                                    SmartLog.e(LOG_TAG, detailedMessage);

                                    final InsufficientStorageException storageError = new InsufficientStorageException(detailedMessage);
                                    storageError.setAvailableSpace(availableSpace);
                                    storageError.setRequiredSpace(requitedSpace);

                                    return Observable.error(storageError);
                                }
                            }

                            transcodingManager.setVideoPreset(preset);

                            SmartLog.d(LOG_TAG, "transcode media with duration: " + extendedMediaDescription.getDuration());

                            final Observable<? extends TranscodingProgress> transcodingProgressObservable;

                            // leave only video & audio streams
                            String[] streamIdx = geAllStreamsIndexes(extendedMediaDescription);

                            if (isUrl) {
                                SmartLog.d(LOG_TAG, "transcode media stream with duration: " + extendedMediaDescription.getDuration());
                                if (isAudio) {
                                    if (device.isAudioPlaylistMode()) {
                                        SmartLog.d(LOG_TAG, "transcode audio in .ts playlist mode");
                                        String[] audioStreamsIdx = getAudioStreamsIndexes(extendedMediaDescription);
                                        transcodingProgressObservable = transcodingManager.transcodeMedia(sourceUrl, null, audioStreamsIdx);
                                    } else {
                                        transcodingProgressObservable = transcodingManager.transcodeAudio(sourceUrl, streamIdx);
                                    }
                                } else {
                                    switch (device.getUnsupportedVideoConversionType()) {
                                        case Device.UNSUPPORTED_VIDEO_CONVERSION_TYPE_FULL:
                                            transcodingProgressObservable = transcodingManager.transcodeMediaFull(sourceUrl, mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? subtitlesFile : null, streamIdx);
                                            break;
                                        case Device.UNSUPPORTED_VIDEO_CONVERSION_TYPE_SEGMENTED:
                                            transcodingProgressObservable = transcodingManager.transcodeMediaSegmented(sourceUrl, mediaDescription.getDuration(), mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? subtitlesFile : null);
                                            break;
                                        case Device.UNSUPPORTED_VIDEO_CONVERSION_TYPE_HLS:
                                        default:
                                            transcodingProgressObservable = transcodingManager.transcodeMedia(sourceUrl, mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? subtitlesFile : null, streamIdx);
                                    }
                                }
                            } else {
                                SmartLog.d(LOG_TAG, "transcode media file with duration: " + extendedMediaDescription.getDuration());
                                if (isAudio) {
                                    if (device.isAudioPlaylistMode()) {
                                        SmartLog.d(LOG_TAG, "transcode audio in .ts playlist mode");
                                        String[] audioStreamsIdx = getAudioStreamsIndexes(extendedMediaDescription);
                                        transcodingProgressObservable = transcodingManager.transcodeMedia(file, null, audioStreamsIdx);
                                    } else {
                                        transcodingProgressObservable = transcodingManager.transcodeAudio(file, streamIdx);
                                    }
                                } else {
                                    switch (device.getUnsupportedVideoConversionType()) {
                                        case Device.UNSUPPORTED_VIDEO_CONVERSION_TYPE_FULL:
                                            transcodingProgressObservable = transcodingManager.transcodeMediaFull(file, mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? subtitlesFile : null, streamIdx);
                                            break;
                                        case Device.UNSUPPORTED_VIDEO_CONVERSION_TYPE_SEGMENTED:
                                            transcodingProgressObservable = transcodingManager.transcodeMediaSegmented(file, mediaDescription.getDuration(), mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? subtitlesFile : null);
                                            break;
                                        case Device.UNSUPPORTED_VIDEO_CONVERSION_TYPE_HLS:
                                        default:
                                            transcodingProgressObservable = transcodingManager.transcodeMedia(file, mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? subtitlesFile : null, streamIdx);
                                    }
                                }
                            }

                            return transcodingProgressObservable
                                    .doOnError(new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            transcodingProgressPublisher.onError(throwable);
                                            transcodingProgressPublisher = PublishSubject.create();
                                        }
                                    })
                                    .filter(new Func1<TranscodingProgress, Boolean>() {
                                        @Override
                                        public Boolean call(TranscodingProgress transcodingProgress) {
                                            transcodingProgressPublisher.onNext(transcodingProgress);
                                            return transcodingProgress.isReady();
                                        }
                                    })
                                    .take(1)
                                    .flatMap(new Func1<TranscodingProgress, Observable<MediaController>>() {
                                        @Override
                                        public Observable<MediaController> call(TranscodingProgress transcodingProgress) {
                                            SmartLog.d(LOG_TAG, "transcoding is ready...");
                                            if (transcodingProgress instanceof SegmentTranscodingProgress) {
                                                return buildSegmentedMediaRequest((SegmentTranscodingProgress) transcodingProgress);
                                            } else {
                                                return buildHlsMediaRequest(transcodingProgress);
                                            }
                                        }

                                        private Observable<MediaController> buildSegmentedMediaRequest(final SegmentTranscodingProgress transcodingProgress) {
                                            final TranscodedMediaDescription description = new TranscodedMediaDescription(extendedMediaDescription,
                                                    transcodingProgress.getSegmentFile(0), transcodingProgress.getOutputFileMimeType(),
                                                    transcodingProgress.getOutputVideoStream(), transcodingProgress.getOutputAudioStream());
                                            description.setSourceUrl(null);

                                            transcodingProgress.setTranscodedMediaDescription(description);
                                            transcodingProgressPublisher.onNext(transcodingProgress);

                                            SmartLog.d(LOG_TAG, String.format("building segmented stream media request " +
                                                            "\n file: %s" +
                                                            "\n mimeType: %s",
                                                    description.getFile().getAbsolutePath(),
                                                    description.getMimeType()));

                                            return streamMedia(device, description, mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? null : subtitleDescription, isAudio, false)
                                                    .map(new Func1<MediaController, MediaController>() {
                                                        @Override
                                                        public MediaController call(MediaController mediaController) {
                                                            final SegmentedMediaController controller = new SegmentedMediaController(mediaController);
                                                            controller.setSegmentListener(new SegmentedMediaController.SegmentListener() {
                                                                @Override
                                                                public boolean onSegmentFinished(int num) {
                                                                    SmartLog.d(LOG_TAG, "segment " + num + " has been finished!");
                                                                    final int nextSegmentNum = num + 1;
                                                                    if (nextSegmentNum < transcodingProgress.getSegmentsCount()) {
                                                                        final File segmentFile = transcodingProgress.getSegmentFile(nextSegmentNum);
                                                                        if (segmentFile != null && segmentFile.exists()) {
                                                                            SmartLog.d(LOG_TAG, "play next segment file " + segmentFile.getName());
                                                                            description.setTranscodedFile(segmentFile);
                                                                            description.setCastUrl(null);
                                                                            streamMedia(device, description, mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? null : subtitleDescription, isAudio, false)
                                                                                    .subscribe(new Subscriber<MediaController>() {
                                                                                        @Override
                                                                                        public void onCompleted() {

                                                                                        }

                                                                                        @Override
                                                                                        public void onError(Throwable e) {
                                                                                            controller.onSegmentedVideoError(e);
                                                                                        }

                                                                                        @Override
                                                                                        public void onNext(MediaController mediaController) {
                                                                                            controller.setMediaController(mediaController, nextSegmentNum);
                                                                                        }
                                                                                    });
                                                                            return false;
                                                                        } else if (transcodingManager.isTranscodingRunning()) {
                                                                            SmartLog.d(LOG_TAG, "wait for a segment to finish transcoding...");
                                                                            controller.onSegmentNotFoundError();
                                                                        } else {
                                                                            controller.onSegmentedVideoError(new Throwable("Failed to play the file"));
                                                                        }
                                                                    }
                                                                    return true;
                                                                }
                                                            });
                                                            return controller;
                                                        }
                                                    });
                                        }

                                        private Observable<MediaController> buildHlsMediaRequest(TranscodingProgress transcodingProgress) {
                                            final TranscodedMediaDescription description = new TranscodedMediaDescription(extendedMediaDescription,
                                                    transcodingProgress.getOutputFile(), transcodingProgress.getOutputFileMimeType(),
                                                    transcodingProgress.getOutputVideoStream(), transcodingProgress.getOutputAudioStream());
                                            description.setSourceUrl(null);

                                            transcodingProgress.setTranscodedMediaDescription(description);
                                            transcodingProgressPublisher.onNext(transcodingProgress);

                                            SmartLog.d(LOG_TAG, String.format("building HLS stream media request " +
                                                            "\n file: %s" +
                                                            "\n mimeType: %s",
                                                    description.getFile().getAbsolutePath(),
                                                    description.getMimeType()));

                                            return streamMedia(device, description, mediaDescription.isOnlyEmbeddedSubtitlesAllowed() ? null : subtitleDescription, isAudio, false);
                                        }
                                    });
                        }

                        //if compatibility is okay, we should publish local files
                        SmartLog.d(LOG_TAG, "making local links...");

                        if (subtitlesFile != null) {
                            return sharingManager.shareSubtitleFile(device, subtitleDescription)
                                    .flatMap(new Func1<ServerConnection, Observable<MediaController>>() {
                                        @Override
                                        public Observable<MediaController> call(ServerConnection serverConnection) {
                                            SmartLog.d(LOG_TAG, "remote subtitle sourceUrl  " + serverConnection.getUrl());
                                            return streamMedia(device, extendedMediaDescription, subtitleDescription, isAudio, transcodingForced);
                                        }
                                    });
                        }

                        if (!TextUtils.isEmpty(mediaDescription.getSourceUrl())) {
                            SmartLog.d(LOG_TAG, "source url is compatible, make cast url...");
                            extendedMediaDescription.setCastUrl(mediaDescription.getSourceUrl());
                            return streamMedia(device, extendedMediaDescription, subtitleDescription, isAudio, false);
                        }

                        if (extendedMediaDescription instanceof TranscodedMediaDescription &&
                                isPlaylistDescription(extendedMediaDescription)) {

                            SmartLog.d(LOG_TAG, "share live stream... ");
                            final RemoteMediaReceiverController remoteMediaReceiver = new RemoteMediaReceiverController();
                            return sharingManager.sharePlaylistFile(device, (TranscodedMediaDescription) extendedMediaDescription, remoteMediaReceiver)
                                    .flatMap(new Func1<ServerConnection, Observable<MediaController>>() {
                                        @Override
                                        public Observable<MediaController> call(ServerConnection serverConnection) {
                                            //we are ready to cast media file
                                            //when media file is casted we can acquire Media Controller
                                            //and pass it to the RemoteMediaReceiverController to handle transcoded buffer problems
                                            return streamMedia(device, extendedMediaDescription, subtitleDescription, isAudio, transcodingForced)
                                                    .map(new Func1<MediaController, MediaController>() {
                                                        @Override
                                                        public MediaController call(MediaController mediaController) {
                                                            remoteMediaReceiver.setController(mediaController);
                                                            return mediaController;
                                                        }
                                                    });
                                        }
                                    });

                        } else {

                            SmartLog.d(LOG_TAG, "share local media... ");
                            return sharingManager.shareMediaFile(device, extendedMediaDescription)
                                    .flatMap(new Func1<ServerConnection, Observable<MediaController>>() {
                                        @Override
                                        public Observable<MediaController> call(ServerConnection serverConnection) {
                                            //we are ready to cast media file
                                            return streamMedia(device, extendedMediaDescription, subtitleDescription, isAudio, transcodingForced);
                                        }
                                    });

                        }
                    }
                });
    }

    private String[] getAudioStreamsIndexes(ExtendedMediaDescription extendedMediaDescription) {
        Set<String> set = extendedMediaDescription.getAudioStreamsMap().keySet();
        return set.toArray(new String[set.size()]);
    }

    private String[] geAllStreamsIndexes(ExtendedMediaDescription extendedMediaDescription) {
        Set<String> audioSet = extendedMediaDescription.getAudioStreamsMap().keySet();
        Set<String> videoSet = extendedMediaDescription.getVideoStreamsMap().keySet();

        HashSet<String> streams = new HashSet<>();
        streams.addAll(audioSet);
        streams.addAll(videoSet);

        return streams.toArray(new String[streams.size()]);
    }

    private boolean isMediaCompatible(Device device, ExtendedMediaDescription mediaDescription, boolean isAudio) {
        String input = null;

        if (mediaDescription.getFile() != null) {
            input = mediaDescription.getFile().getAbsolutePath();
        } else if (mediaDescription.getSourceUrl() != null) {
            input = mediaDescription.getSourceUrl();
        }

        String mimeType = mediaDescription.getMimeType();
        String extension = getExtension(input);

        SmartLog.i(LOG_TAG, String.format("compatibility check of media:" +
                "\n mimeType: %s" +
                "\n extension: %s" +
                "\n videoStreams: %s" +
                "\n audioStreams: %s", mimeType, extension, mediaDescription.listVideoStreams(), mediaDescription.listAudioStreams()));

        final boolean hasSupportedStreams;
        if (isAudio) {
            hasSupportedStreams = device.hasSupportedAudioStreams(mediaDescription.getAudioStreams());
        } else {
            hasSupportedStreams = device.hasSupportedVideoStreams(mediaDescription.getVideoStreams())
                    && device.hasSupportedAudioStreams(mediaDescription.getAudioStreams());
        }

        Integer width = mediaDescription.getWidth();
        Integer height = mediaDescription.getHeight();

        SmartLog.d(LOG_TAG, String.format("media resolution: %d,%d", width, height));

        return device.hasSupportedDataType(mimeType, extension)
                && (isPlaylistDescription(mediaDescription) || hasSupportedStreams);
    }

    private boolean isPlaylistDescription(ExtendedMediaDescription extendedMediaDescription) {
        return MediaTypes.MIME_TYPE_PLAYLIST_X_MPEGURL.equals(extendedMediaDescription.getMimeType());
    }

    private String getExtension(String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (TextUtils.isEmpty(extension)) {
            extension = url.substring(url.lastIndexOf(".") + 1, url.length());
        }
        return extension;
    }

    private Bitmap scaleBitmap(Bitmap originalImage, int width, int height) {
        Bitmap background = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

        Matrix transformation;

        float originalWidth = originalImage.getWidth();
        float originalHeight = originalImage.getHeight();

        Canvas canvas = new Canvas(background);

        if (originalWidth >= originalHeight) {
            float scale = width / originalWidth;

            float xTranslation = 0.0f;
            float yTranslation = (height - originalHeight * scale) / 2.0f;

            transformation = new Matrix();
            transformation.postTranslate(xTranslation, yTranslation);
            transformation.preScale(scale, scale);
        } else {
            float scale = height / originalHeight;

            float xTranslation = (width - originalWidth * scale) / 2.0f;
            float yTranslation = 0.0f;

            transformation = new Matrix();
            transformation.postTranslate(xTranslation, yTranslation);
            transformation.preScale(scale, scale);
        }

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        canvas.drawBitmap(originalImage, transformation, paint);

        return background;
    }

    private void saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        SmartLog.d(LOG_TAG, "saveBitmapToFile " + file.getAbsolutePath());

        if (!file.getParentFile().exists()) {
            SmartLog.d(LOG_TAG, "creating parent folder ");
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            SmartLog.d(LOG_TAG, "creating file ");
            file.createNewFile();
        }

        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
        // PNG is a lossless format, the compression factor (100) is ignored
        out.close();
    }

    private String getTempStorage() {
        if (streamingOptions != null && streamingOptions.getTempStoragePath() != null) {
            return streamingOptions.getTempStoragePath();
        }
        return getDefaultTempStoragePath();
    }

    private String getDefaultTempStoragePath() {
        File filesDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filesDir = context.getExternalFilesDir(null);
            if (filesDir == null) {
                filesDir = Environment.getExternalStorageDirectory();
            }
            if (filesDir == null) {
                filesDir = context.getFilesDir();
            }
        } else {
            filesDir = context.getFilesDir();
        }

        final File file = new File(filesDir, "temp");
        return file.getAbsolutePath();
    }

    private Observable<ImageDescription> prepareImageLink(final Device device, final ImageDescription imageDescription) {
        SmartLog.i(LOG_TAG, "prepareImageLink " + imageDescription.getSourceUrl());
        return Observable.create(
                new Observable.OnSubscribe<ImageDescription>() {
                    @Override
                    public void call(final Subscriber<? super ImageDescription> sub) {
                        try {
                            URL url = new URL(imageDescription.getSourceUrl());
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream input = connection.getInputStream();

                            Bitmap bitmap = null;
                            String extension = getExtension(imageDescription.getSourceUrl());
                            if (MediaTypes.EXTENSION_SVG.equals(extension)) {

                                SmartLog.d(LOG_TAG, "decoding svg...");
                                //Read  data about image to Options object
                                SVG svg = SVG.getFromInputStream(input);

                                if (svg.getDocumentWidth() != -1) {
                                    SmartLog.d(LOG_TAG, "svg parsed...");

                                    bitmap = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                            (int) Math.ceil(svg.getDocumentHeight()),
                                            Bitmap.Config.ARGB_8888);

                                    Canvas bmcanvas = new Canvas(bitmap);

                                    // Clear background to white
                                    bmcanvas.drawRGB(255, 255, 255);

                                    // Render our document onto our canvas
                                    svg.renderToCanvas(bmcanvas);
                                }

                            } else if (MediaTypes.EXTENSION_TIFF.equals(extension)) {

                                File file = new File(getTempStorage(), "temp_image." + extension);
                                if (file.exists()) {
                                    file.delete();
                                }
                                copyInputStreamToFile(input, file);

                                SmartLog.d(LOG_TAG, "decoding tiff...");
                                //Read data about image to Options object
                                TiffBitmapFactory.Options options = new TiffBitmapFactory.Options();
                                bitmap = TiffBitmapFactory.decodePath(file.getAbsolutePath(), options);

                            } else {

                                SmartLog.d(LOG_TAG, "decoding bitmap... ");
                                bitmap = BitmapFactory.decodeStream(input);

                            }


                            boolean validFormat = device.hasSupportedImageType(imageDescription.getMimeType(),
                                    extension);
                            boolean resolutionSupported = device.isImageResolutionSupported(bitmap.getWidth(), bitmap.getHeight());

                            if (!resolutionSupported) {
                                SmartLog.e(LOG_TAG, "device does not support resolution: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                final int height = device.getSupportedImageResolution().y;
                                final int width = device.getSupportedImageResolution().x;
                                SmartLog.d(LOG_TAG, "encode image to " + width + "x" + height);

                                bitmap = scaleBitmap(bitmap, width, height);
                            }

                            if (!validFormat || !resolutionSupported) {
                                String imageName = imageDescription.getTitle() + "." + MediaTypes.EXTENSION_PNG;
                                final File imageFile = new File(getTempStorage(), imageName);
                                saveBitmapToFile(bitmap, imageFile);

                                SmartLog.d(LOG_TAG, "a new image file " + imageFile.getAbsolutePath());

                                imageDescription.setSourceUrl(null);
                                imageDescription.setFile(imageFile);
                            }

                            imageDescription.setWidth(bitmap.getWidth());
                            imageDescription.setHeight(bitmap.getHeight());

                            sub.onNext(imageDescription);
                            sub.onCompleted();
                        } catch (Exception e) {
                            e.printStackTrace();
                            final Throwable exception = new Throwable("Could not save encoded image file! " + e.getMessage());
                            SmartLog.e(LOG_TAG, "IOException" + exception.getMessage());

                            sub.onError(exception);
                        }
                    }
                }
        );
    }

    private Observable<ImageDescription> prepareImageFile(final Device device, final ImageDescription imageDescription) {
        SmartLog.i(LOG_TAG, "prepareImageFile " + imageDescription.getFile().getAbsolutePath());
        return Observable.create(
                new Observable.OnSubscribe<ImageDescription>() {
                    @Override
                    public void call(final Subscriber<? super ImageDescription> sub) {
                        try {
                            Bitmap bitmap = null;

                            String extension = getExtension(imageDescription.getFile().getAbsolutePath());
                            if (MediaTypes.EXTENSION_SVG.equals(extension)) {

                                SmartLog.d(LOG_TAG, "decoding svg...");
                                //Read data about image to Options object
                                SVG svg = SVG.getFromInputStream(new FileInputStream(imageDescription.getFile()));

                                if (svg.getDocumentWidth() != -1) {
                                    SmartLog.d(LOG_TAG, "svg parsed...");

                                    bitmap = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                            (int) Math.ceil(svg.getDocumentHeight()),
                                            Bitmap.Config.ARGB_8888);

                                    Canvas bmcanvas = new Canvas(bitmap);

                                    // Clear background to white
                                    bmcanvas.drawRGB(255, 255, 255);

                                    // Render our document onto our canvas
                                    svg.renderToCanvas(bmcanvas);
                                }

                            } else if (MediaTypes.EXTENSION_TIFF.equals(extension)) {
                                SmartLog.d(LOG_TAG, "decoding tiff...");
                                //Read data about image to Options object
                                TiffBitmapFactory.Options options = new TiffBitmapFactory.Options();
                                bitmap = TiffBitmapFactory.decodePath(imageDescription.getFile().getAbsolutePath(), options);
                            } else {
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                                SmartLog.d(LOG_TAG, "decoding bitmap... ");
                                bitmap = BitmapFactory.decodeFile(imageDescription.getFile().getAbsolutePath(), options);
                            }

                            boolean validFormat = device.hasSupportedImageType(imageDescription.getMimeType(),
                                    extension);
                            boolean resolutionSupported = device.isImageResolutionSupported(bitmap.getWidth(), bitmap.getHeight());

                            if (!resolutionSupported) {
                                SmartLog.e(LOG_TAG, "device does not support resolution: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                final int height = device.getSupportedImageResolution().y;
                                final int width = device.getSupportedImageResolution().x;
                                SmartLog.d(LOG_TAG, "encode image to " + width + "x" + height);
                                bitmap = scaleBitmap(bitmap, width, height);
                            }

                            if (!validFormat || !resolutionSupported) {
                                String imageName = imageDescription.getFile().getName();
                                imageName = imageName.substring(0, imageName.lastIndexOf(".")) + "." + MediaTypes.EXTENSION_PNG;

                                final File imageFile = new File(getTempStorage(), imageName);
                                saveBitmapToFile(bitmap, imageFile);

                                SmartLog.d(LOG_TAG, "a new image file " + imageFile.getAbsolutePath());

                                imageDescription.setFile(imageFile);
                            }

                            imageDescription.setWidth(bitmap.getWidth());
                            imageDescription.setHeight(bitmap.getHeight());

                            sub.onNext(imageDescription);
                            sub.onCompleted();
                        } catch (Exception e) {
                            e.printStackTrace();
                            final Throwable exception = new Throwable("Could not save encoded image file! " + e.getMessage());
                            SmartLog.e(LOG_TAG, "IOException" + exception.getMessage());

                            sub.onError(exception);
                        }
                    }
                }
        );
    }

    private Observable<File> downloadFile(final String sourceUrl, final String pathToDownload) {
        return Observable.create(
                new Observable.OnSubscribe<File>() {
                    @Override
                    public void call(final Subscriber<? super File> sub) {
                        File file = new File(pathToDownload);

                        if (file.exists()) {
                            file.delete();
                        }

                        URL url = null;
                        try {
                            url = new URL(sourceUrl);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream input = connection.getInputStream();

                            copyInputStreamToFile(input, file);

                            sub.onNext(file);
                            sub.onCompleted();
                        } catch (IOException e) {
                            IllegalArgumentException exception = new IllegalArgumentException("Could not download the subtitles!");
                            SmartLog.e(LOG_TAG, "IOException " + exception.getMessage());
                            sub.onError(exception);
                        }
                    }
                }
        );
    }

    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if (out != null) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //  </editor-fold>
}
