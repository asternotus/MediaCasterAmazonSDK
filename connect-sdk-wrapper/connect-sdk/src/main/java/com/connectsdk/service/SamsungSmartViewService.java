package com.connectsdk.service;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.provider.samsung.SamsungSmartViewDeviceInfo;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WakeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.convergence.NSDevice;
import com.connectsdk.service.convergence.NSListener;
import com.connectsdk.service.samsung.OnBLEDiscoveryListener;
import com.connectsdk.service.sessions.LaunchSession;
import com.samsung.multiscreenfix.Application;
import com.samsung.multiscreenfix.ApplicationInfo;
import com.samsung.multiscreenfix.Channel;
import com.samsung.multiscreenfix.Client;
import com.samsung.multiscreenfix.Error;
import com.samsung.multiscreenfix.Message;
import com.samsung.multiscreenfix.Result;
import com.samsung.multiscreen.channel.ChannelClient;
import com.samsung.multiscreen.channel.IChannelListener;
import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Dmitry on 17.05.17.
 */

public class SamsungSmartViewService extends DeviceService implements MediaPlayer, MediaControl, VolumeControl, WakeControl {

    public static final String ID = "SamsungSmartViewService";

    private static final String LOG_TAG = SamsungSmartViewService.class.getSimpleName();

    private static final String SMART_VIEW_TV_LOG_TAG = "SmartViewTvLogs";

    private static final String AVPLAY_UNSUPPORTED_VIDEO_FORMAT_ERROR = "AVPlayUnsupportedVideoFormatError";
    private static final String AVPLAY_UNSUPPORTED_VIDEO_RESOLUTION_ERROR = "AVPlayUnsupportedVideoResolutionError";
    private static final String AVPLAY_UNSUPPORTED_VIDEO_FRAME_RATE_ERROR = "AVPlayUnsupportedVideoFrameRateError";
    private static final String AVPLAY_CORRUPTED_STREAM_ERROR = "AVPlayCorruptedStreamError";
    private static final String AVPLAY_INVALID_VALUES_ERROR = "InvalidValuesError";

    private static final String TIZEN_PLAYER_ERROR_VIDEO_CODEC_NOT_SUPPORTED = "PLAYER_ERROR_VIDEO_CODEC_NOT_SUPPORTED";
    private static final String TIZEN_PLAYER_ERROR_AUDIO_CODEC_NOT_SUPPORTED = "PLAYER_ERROR_AUDIO_CODEC_NOT_SUPPORTED";
    private static final String TIZEN_PLAYER_ERROR_NOT_SUPPORTED_FILE = "PLAYER_ERROR_NOT_SUPPORTED_FILE";
    private static final String TIZEN_PLAYER_ERROR_NOT_SUPPORTED_FORMAT = "PLAYER_ERROR_NOT_SUPPORTED_FORMAT";

    private static final int MAX_LEGACY_SMART_VIEW_CONNECTION_ATTEMPTS = 5;

    private static final String MSG_TYPE = "type";

    private static final String MSG_TYPE_STATE = "state_event";
    private static final String MSG_TYPE_ERROR = "error_event";
    private static final String MSG_TYPE_DURATION = "info_duration";
    private static final String MSG_TYPE_POSITION = "info_position";

    private static final String STATUS_REQUEST = "get_state";
    private static final String PLAY_REQUEST = "play";
    private static final String PAUSE_REQUEST = "pause";
    private static final String RESUME_REQUEST = "resume";
    private static final String STOP_REQUEST = "stop";
    private static final String SET_POSITION_REQUEST = "set_position";
    private static final String GET_POSITION_REQUEST = "get_position";
    private static final String GET_DURATION_REQUEST = "get_duration";
    private static final String SET_VOLUME_REQUEST = "set_volume";
    private static final String DISCONNECT_REQUEST = "disconnect";
    private static final String SHOW_PROGRESS_SCREEN_REQUEST = "show_progress_screen";
    private static final String HIDE_PROGRESS_SCREEN_REQUEST = "hide_progress_screen";

    private static final String SET_CLIENT_VERSION_REQUEST = "set_client_version";

    private static final String URL_EXTRA_KEY = "url";
    private static final String TITLE_EXTRA_KEY = "title";
    private static final String CONTENT_EXTRA_KEY = "content";
    private static final String STATE_EXTRA_KEY = "state";
    private static final String POSITION_EXTRA_KEY = "position";
    private static final String MSG_EXTRA_KEY = "msg";
    private static final String CLIENT_VERSION_EXTRA_KEY = "client_version";
    private static final String CLIENT_APP_TYPE_EXTRA_KEY = "client_app_type";

    private static final String CAST_TYPE_AUDIO = "audio";
    private static final String CAST_TYPE_IMAGE = "image";
    private static final String CAST_TYPE_VIDEO = "video";

    private static final String SUBTITLE_URL_EXTRA_KEY = "subtitle_url";

    private static final String VALUE_EXTRA_KEY = "value";

    private static final String VOLUME_EXTRA_KEY = "volume";

    private static final String TERMINATED_STATUS = "TERMINATED";
    private static final String MSG_TYPE_LOG = "log";
    private static final Integer MAX_RECONNECTION_ATTEMPTS = 5;

    private PlayStateSubscription playStateSubscription;
    private VolumeSubscription volumeSubscription;

    private MediaControl.PlayStateStatus currentPlaystate = MediaControl.PlayStateStatus.Unknown;

    private String clientVersion = "FREE";

    private String tizenAppId;
    private String orsayAppId;

    private String channelId;

    private Channel smartViewChannel;
    private com.samsung.multiscreen.channel.Channel legacySmartViewChannel;

    private Application receiverApp;

    private CopyOnWriteArrayList<ConvergenceListener> nsListeners
            = new CopyOnWriteArrayList<>();

    private SamsungSmartViewDeviceInfo deviceInfo;

    private int remainingConnectionAttempts;
    private DeviceServiceListener connectionListener;
    private PlayStateStatus lastState = PlayStateStatus.Finished;
    private long lastPosition = 0;
    private MediaInfo lastMediaInfo = null;
    private boolean blockStateReceiving = false;
    private String clientAppType;

    public SamsungSmartViewService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        init(serviceDescription);
    }

    public SamsungSmartViewService(ServiceConfig serviceConfig) {
        super(serviceConfig);
        init(serviceDescription);
    }

    /**
     * Get filter instance for this service which contains a name of service and id. It is used in
     * discovery process
     */
    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, "SamsungSmartTv");
    }

    protected void init(ServiceDescription serviceDescription) {
        SmartLog.d(LOG_TAG, "init ");
        if (serviceDescription != null
                && serviceDescription.getDevice() instanceof SamsungSmartViewDeviceInfo) {
            this.deviceInfo = (SamsungSmartViewDeviceInfo) serviceDescription.getDevice();
        } else {
            SmartLog.wtf(LOG_TAG, "service is empty! ");
            this.deviceInfo = null;
        }
    }

    @Override
    public CapabilityMethods.CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> clazz) {
        if (clazz != null) {
            if (clazz.equals(MediaPlayer.class)) {
                return getMediaPlayerCapabilityLevel();
            } else if (clazz.equals(MediaControl.class)) {
                return getMediaControlCapabilityLevel();
            } else if (clazz.equals(VolumeControl.class)) {
                return getVolumeControlCapabilityLevel();
            } else if (clazz.equals(WakeControl.class)) {
                return CapabilityMethods.CapabilityPriorityLevel.HIGH;
            }
        }
        return CapabilityMethods.CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override
    protected void updateCapabilities() {
        List<String> capabilities = new ArrayList<String>();
        capabilities.add(MediaPlayer.MediaInfo_Get);
        capabilities.add(MediaPlayer.Display_Image);
        capabilities.add(MediaPlayer.Play_Audio);
        capabilities.add(MediaPlayer.Play_Video);
        capabilities.add(MediaPlayer.Close);
        capabilities.add(MediaPlayer.MetaData_MimeType);
        capabilities.add(MediaPlayer.MetaData_Thumbnail);
        capabilities.add(MediaPlayer.MetaData_Title);

        capabilities.add(MediaControl.Play);
        capabilities.add(MediaControl.Pause);
        capabilities.add(MediaControl.Stop);
        capabilities.add(MediaControl.Seek);
        capabilities.add(MediaControl.Duration);
        capabilities.add(MediaControl.Position);
        capabilities.add(MediaControl.PlayState);
        capabilities.add(MediaControl.PlayState_Subscribe);

        capabilities.add(WakeControl.Wake_Device);

        setCapabilities(capabilities);
    }

    //  <editor-fold desc="listeners">

    private OnBLEDiscoveryListener mBLEListener = new OnBLEDiscoveryListener() {

        @Override
        public void onBLEDeviceFound(String devName) {
            SmartLog.d(LOG_TAG, "onBLEDeviceFound " + devName);
        }
    };

    //  </editor-fold>

    //  <editor-fold desc="media control">

    @Override
    public void showProgressScreen(ResponseListener<Object> responseListener) {
        SmartLog.d(LOG_TAG, "showProgressScreen");
        sendMessage(SHOW_PROGRESS_SCREEN_REQUEST, null);
    }

    @Override
    public void hideProgressScreen(ResponseListener<Object> responseListener) {
        SmartLog.d(LOG_TAG, "hideProgressScreen");
        sendMessage(HIDE_PROGRESS_SCREEN_REQUEST, null);
    }

    @Override
    public MediaControl getMediaControl() {
        return this;
    }

    @Override
    public CapabilityMethods.CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void play(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "play ");

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "play state received " + object.name());
                if (object == MediaControl.PlayStateStatus.Playing) {
                    SmartLog.d(LOG_TAG, "play successful! ");
                    listener.onSuccess(object);
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });

        sendMessage(RESUME_REQUEST, null);
    }

    @Override
    public void changeSubtitles(SubtitleInfo info, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void pause(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "pause ");

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "pause state received " + object.name());
                if (object == MediaControl.PlayStateStatus.Paused) {
                    SmartLog.d(LOG_TAG, "pause successful! ");
                    listener.onSuccess(object);
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });

        sendMessage(PAUSE_REQUEST, null);
    }

    @Override
    public void stop(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "stop ");

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "stop state received " + object.name());
                if (object == MediaControl.PlayStateStatus.Finished || object == MediaControl.PlayStateStatus.Idle) {
                    SmartLog.d(LOG_TAG, "stop successful! ");
                    listener.onSuccess(object);
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });

        sendMessage(STOP_REQUEST, null);
    }

    @Override
    public void rewind(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "rewind ");
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void fastForward(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "fastForward ");
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void previous(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void next(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void seek(long position, final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "seek " + position);

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "seek state received " + object.name());
                if (object == MediaControl.PlayStateStatus.Playing || object == MediaControl.PlayStateStatus.Paused) {
                    SmartLog.d(LOG_TAG, "seek successful! ");
                    listener.onSuccess(object);
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "seek onError ");
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });

        HashMap<String, String> params = new HashMap<>();
        params.put(POSITION_EXTRA_KEY, "" + position);

        sendMessage(SET_POSITION_REQUEST, params);
    }

    @Override
    public void getDuration(final MediaControl.DurationListener listener) {
        SmartLog.d(LOG_TAG, "getDuration ");
        ConvergenceListener nsListener = new ConvergenceListener("getDuration") {
            @Override
            protected void onMessageReceived(JSONObject object) {
                long duration = getDurationFromResponse(object);
                if (duration >= 0) {
                    listener.onSuccess(duration);
                    deleteListener(this);
                }
            }
        };
        registerListener(nsListener);

        sendMessage(GET_DURATION_REQUEST, null);
    }

    @Override
    public void getPosition(final MediaControl.PositionListener listener) {
        SmartLog.d(LOG_TAG, "getPosition ");
        ConvergenceListener nsListener = new ConvergenceListener("getPosition") {
            @Override
            protected void onMessageReceived(JSONObject object) {
                SmartLog.d(LOG_TAG, "getPosition " + object.toString());
                long duration = getPositionFromResponse(object);
                lastPosition = duration;
                if (duration >= 0) {
                    listener.onSuccess(duration);
                    deleteListener(this);
                }
            }
        };
        registerListener(nsListener);

        sendMessage(GET_POSITION_REQUEST, null);

    }

    @Override
    public void getPlayState(final MediaControl.PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "getPlayState ");

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {
            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "play state received " + object.name());
                lastState = object;
                listener.onSuccess(object);
                playStateSubscription.removeListener(this);
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "getPlayState onError ");
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });

        sendMessage(STATUS_REQUEST, null);
    }

    @Override
    public ServiceSubscription<MediaControl.PlayStateListener> subscribePlayState(MediaControl.PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "subscribePlayState ");
        if (!playStateSubscription.getListeners().contains(listener)) {
            playStateSubscription.addListener(listener);
        }
        getPlayState(listener);
        return playStateSubscription;
    }

    @Override
    public void unSubscribePlayState(MediaControl.PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "unSubscribePlayState ");
        if (playStateSubscription != null && playStateSubscription.getListeners().contains(listener)) {
            SmartLog.d(LOG_TAG, "remove listener ");
            playStateSubscription.removeListener(listener);
        }
    }

    private boolean isErrorMsg(JSONObject stateObj) {
        String type = stateObj.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_ERROR)) {
                return true;
            }
        }

        return false;
    }

    private boolean isLogMsg(JSONObject stateObj) {
        String type = stateObj.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_LOG)) {
                return true;
            }
        }
        return false;
    }

    private String getLogMsg(JSONObject stateObj) {
        String type = stateObj.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_LOG)) {
                return stateObj.optString(MSG_EXTRA_KEY);
            }
        }
        return null;
    }

    private boolean isUnsupportedFileError(JSONObject stateObj) {
        String type = stateObj.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_ERROR)) {
                SmartLog.d(LOG_TAG, "received error description ");
                String errorName = stateObj.optString(MSG_EXTRA_KEY);
                SmartLog.d(LOG_TAG, "Error name: " + errorName);
                if (errorName != null &&
                        (errorName.equals(AVPLAY_UNSUPPORTED_VIDEO_FORMAT_ERROR)
                                || errorName.equals(AVPLAY_UNSUPPORTED_VIDEO_FRAME_RATE_ERROR)
                                || errorName.equals(AVPLAY_UNSUPPORTED_VIDEO_RESOLUTION_ERROR)
                                || errorName.equals(AVPLAY_INVALID_VALUES_ERROR)
                                || errorName.equals(AVPLAY_CORRUPTED_STREAM_ERROR)
                                || errorName.equals(TIZEN_PLAYER_ERROR_VIDEO_CODEC_NOT_SUPPORTED)
                                || errorName.equals(TIZEN_PLAYER_ERROR_AUDIO_CODEC_NOT_SUPPORTED)
                                || errorName.equals(TIZEN_PLAYER_ERROR_NOT_SUPPORTED_FILE)
                                || errorName.equals(TIZEN_PLAYER_ERROR_NOT_SUPPORTED_FORMAT)
                        )) {
                    SmartLog.d(LOG_TAG, "unsupported error! ");
                    return true;
                }
            }
        }

        return false;
    }

    private MediaControl.PlayStateStatus createPlayStateStatusFromFireTVStatus(JSONObject stateObj) {
        String type = stateObj.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_STATE)) {
                String state = stateObj.optString(STATE_EXTRA_KEY);
                if (state != null) {
                    switch (state) {
                        case "Error":
                            currentPlaystate = MediaControl.PlayStateStatus.Error;
                            break;
                        case "Buffering":
                            currentPlaystate = MediaControl.PlayStateStatus.Buffering;
                            break;
                        case "Playing":
                            currentPlaystate = MediaControl.PlayStateStatus.Playing;
                            break;
                        case "Paused":
                            currentPlaystate = MediaControl.PlayStateStatus.Paused;
                            break;
                        case "Stopped":
                            currentPlaystate = MediaControl.PlayStateStatus.Finished;
                            break;
                        case "Cancelled":
                            currentPlaystate = MediaControl.PlayStateStatus.Cancelled;
                            break;
                        default:
                            currentPlaystate = MediaControl.PlayStateStatus.Unknown;
                    }
                    return currentPlaystate;
                }
            }
        }

        return null;
    }

    private boolean isTerminationStatus(JSONObject object) {
        final JSONObject widgetInfo = object.optJSONObject("widgetInfo");
        if (widgetInfo != null) {
            SmartLog.d(LOG_TAG, "received widget info: " + widgetInfo.toString());
            String id = widgetInfo.optString("ID");
            String status = widgetInfo.optString("Status");
            if (id != null && (id.equals(tizenAppId) || id.equals(orsayAppId)) && status != null && status.equals(TERMINATED_STATUS)) {
                return true;
            }
        }
        return false;
    }

    private float getVolumeFromStateObj(JSONObject stateObj) {
        String type = stateObj.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_STATE)) {
                return stateObj.optLong(VOLUME_EXTRA_KEY, 0);
            }
        }
        return -1;
    }

    private long getDurationFromResponse(JSONObject object) {
        String type = object.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_DURATION)) {
                return object.optLong(VALUE_EXTRA_KEY, 0);
            }
        }
        return -1;
    }

    private long getPositionFromResponse(JSONObject object) {
        String type = object.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_POSITION)) {
                return object.optLong(VALUE_EXTRA_KEY, 0);
            }
        }
        return -1;
    }

    //  </editor-fold>

    //  <editor-fold desc="media player">

    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override
    public CapabilityMethods.CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void getMediaInfo(MediaPlayer.MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<MediaPlayer.MediaInfoListener> subscribeMediaInfo(MediaPlayer.MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
        return null;
    }

    @Override
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener listener) {
        setMediaSource(mediaInfo, listener);
    }

    @Override
    public void playMedia(MediaInfo mediaInfo, boolean shouldLoop, MediaPlayer.LaunchListener listener) {
        setMediaSource(mediaInfo, listener);
    }

    @Override
    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener) {
        stop(listener);
    }

    @Override
    public void displayImage(String url, String mimeType, String title, String description, String iconSrc, MediaPlayer.LaunchListener listener) {
        setMediaSource(new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build(), listener);
    }

    @Override
    public void playMedia(String url, String mimeType, String title, String description, String iconSrc, boolean shouldLoop, MediaPlayer.LaunchListener listener) {
        setMediaSource(new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build(), listener);
    }

    private void setMediaSource(final MediaInfo mediaInfo, final MediaPlayer.LaunchListener listener) {
        SmartLog.d(LOG_TAG, "setMediaSource " + mediaInfo.getTitle());

        lastMediaInfo = mediaInfo;

        if (TextUtils.isEmpty(mediaInfo.getMimeType())) {
            if (listener != null) {
                listener.onError(new ServiceCommandError("Wrong media info: mime type is null!"));
            }
        }

        HashMap<String, String> params = new HashMap<>();
        String url = mediaInfo.getUrl();
        params.put(URL_EXTRA_KEY, url);

        final String castType;
        if (mediaInfo.getMimeType().contains("audio")) {
            castType = CAST_TYPE_AUDIO;
        } else if (mediaInfo.getMimeType().contains("image")) {
            castType = CAST_TYPE_IMAGE;
        } else {
            castType = CAST_TYPE_VIDEO;
        }

        params.put(CONTENT_EXTRA_KEY, castType);

        String title = mediaInfo.getTitle();
        params.put(TITLE_EXTRA_KEY, title);

        SubtitleInfo subtitleInfo = mediaInfo.getSubtitleInfo();
        if (subtitleInfo != null) {
            String subUrl = subtitleInfo.getUrl();
            SmartLog.d(LOG_TAG, "add subtitle " + subUrl);
            params.put(SUBTITLE_URL_EXTRA_KEY, subUrl);
        }

        SmartLog.d(LOG_TAG, "start playback of " + url);

        sendMessage(PLAY_REQUEST, params);

        playStateSubscription.addListener(new MediaControl.PlayStateListener() {

            public boolean bufferingStarted = false;

            @Override
            public void onSuccess(MediaControl.PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "[play] state received " + object.name());
                if (object == PlayStateStatus.Buffering) {
                    bufferingStarted = true;
                } else if ((bufferingStarted || castType.equals(CAST_TYPE_IMAGE)) && object == MediaControl.PlayStateStatus.Playing) {
                    SmartLog.d(LOG_TAG, "play successful! ");
                    if (listener != null) {
                        listener.onSuccess(createMediaLaunchObject());
                    }
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "[play] onError ");

                if (error.getCode() == ServiceCommandError.MEDIA_UNSUPPORTED) {
                    SmartLog.e(LOG_TAG, "[play] media format is not supported! ");
                }

                if (listener != null) {
                    listener.onError(error);
                }

                playStateSubscription.removeListener(this);
            }
        });
    }

    private MediaPlayer.MediaLaunchObject createMediaLaunchObject() {
        LaunchSession launchSession = new LaunchSession();
        launchSession.setService(this);
        launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
        launchSession.setAppId(deviceInfo.getSmartViewService().getId());
        launchSession.setAppName(deviceInfo.getSmartViewService().getName());
        MediaPlayer.MediaLaunchObject mediaLaunchObject = new MediaPlayer.MediaLaunchObject(launchSession, this);
        return mediaLaunchObject;
    }

    //  </editor-fold>

    //  <editor-fold desc="volume control">

    @Override
    public VolumeControl getVolumeControl() {
        return this;
    }

    @Override
    public CapabilityMethods.CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void volumeUp(final ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void volumeDown(final ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void setVolume(float volume, final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "setVolume " + volume);

        volumeSubscription.addListener(new VolumeControl.VolumeListener() {
            @Override
            public void onSuccess(Float value) {
                SmartLog.d(LOG_TAG, "volume received " + value);
                listener.onSuccess(value);
                volumeSubscription.removeListener(this);
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "setVolume onError ");
                listener.onError(error);
                volumeSubscription.removeListener(this);
            }
        });

        HashMap<String, String> params = new HashMap<>();
        params.put(VOLUME_EXTRA_KEY, "" + ((int) volume));

        sendMessage(SET_VOLUME_REQUEST, params);
    }

    @Override
    public void getVolume(final VolumeControl.VolumeListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void setMute(boolean isMute, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void getMute(VolumeControl.MuteListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<VolumeControl.VolumeListener> subscribeVolume(VolumeControl.VolumeListener listener) {
        SmartLog.d(LOG_TAG, "subscribeVolume ");
        volumeSubscription.addListener(listener);
        return volumeSubscription;
    }

    @Override
    public ServiceSubscription<VolumeControl.MuteListener> subscribeMute(VolumeControl.MuteListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
        return null;
    }

    //  </editor-fold>

    //  <editor-fold desc="connection">

    /**
     * Will attempt to connect to the DeviceService. The failure/success will be reported back to the DeviceServiceListener. If the connection attempt reveals that pairing is required, the DeviceServiceListener will also be notified in that event.
     */
    public void connect() {
        prepareAndConnect(null);
    }

    private void prepareAndConnect(final Integer reconnectionAttempts) {
        SmartLog.d(LOG_TAG, "prepareAndConnect ");

        if (deviceInfo == null || deviceInfo.getSmartViewService() == null) {
            SmartLog.e(LOG_TAG, "Device info should not be null!");
            onConnectionFailure(new ServiceCommandError(-1, "Device info should not be null!"));
            return;
        }

        if ((TextUtils.isEmpty(orsayAppId) || TextUtils.isEmpty(tizenAppId)) || TextUtils.isEmpty(channelId)) {
            SmartLog.e(LOG_TAG, "App ID or Channel ID is empty!");
            final ServiceCommandError serviceCommandError = new ServiceCommandError(-1, "App ID or Channel ID is empty!");
            onConnectionFailure(serviceCommandError);
            return;
        }

        String appID = deviceInfo.isTizen() ? tizenAppId : orsayAppId;

        SmartLog.d(LOG_TAG, String.format("Connecting to application %s, channel %s", appID, channelId));

        final Application application = deviceInfo.getSmartViewService().createApplication(appID, channelId);

        application.getInfo(new Result<ApplicationInfo>() {
            @Override
            public void onSuccess(ApplicationInfo applicationInfo) {
                SmartLog.d(LOG_TAG, "getInfo onSuccess, app is launched " + applicationInfo.isRunning());
                connectToDevice(application, applicationInfo, reconnectionAttempts);
            }

            @Override
            public void onError(Error error) {
                SmartLog.e(LOG_TAG, "onError " + error.getMessage());
                installAndConnect(application);
            }
        });
    }

    public void installAndConnect(final Application application) {
        SmartLog.d(LOG_TAG, "install application " + application.getUri());

        application.install(new Result<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                SmartLog.d(LOG_TAG, "application installed " + aBoolean);
                if (aBoolean) {
                    application.getInfo(new Result<ApplicationInfo>() {
                        @Override
                        public void onSuccess(ApplicationInfo applicationInfo) {
                            SmartLog.d(LOG_TAG, "getInfo onSuccess, app is launched " + applicationInfo.isRunning());
                            connectToDevice(application, applicationInfo, null);
                        }

                        @Override
                        public void onError(Error error) {
                            SmartLog.e(LOG_TAG, "onError " + error.getMessage());
                            ServiceCommandError serviceCommandError =
                                    new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed.");
                            onConnectionFailure(serviceCommandError);
                        }
                    });
                } else {
                    SmartLog.e(LOG_TAG, "Could not install application!");
                    ServiceCommandError serviceCommandError =
                            new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed.");
                    onConnectionFailure(serviceCommandError);
                }
            }

            @Override
            public void onError(Error error) {
                SmartLog.e(LOG_TAG, "Could not install application! " + error.getMessage());
                ServiceCommandError serviceCommandError =
                        new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed. " + error.getMessage());
                onConnectionFailure(serviceCommandError);
            }
        });
    }

    private void connectToDevice(final Application application, final ApplicationInfo applicationInfo, final Integer reconnectionAttempts) {
        if (reconnectionAttempts != null && reconnectionAttempts == 0) {
            SmartLog.d(LOG_TAG, "Smart View channel disconnected");
            disconnect();
            return;
        }

        SmartLog.d(LOG_TAG, "Connect via Smart View");
        SmartLog.d(LOG_TAG, "Application is running: " + applicationInfo.isRunning());

        if (application == null) {
            SmartLog.e(LOG_TAG, "Application should not be null!");
            final ServiceCommandError serviceCommandError = new ServiceCommandError(-1, "Application should not be null!");
            onConnectionFailure(serviceCommandError);
            return;
        }

        smartViewChannel = null;
        receiverApp = null;

        if (reconnectionAttempts == null) {
            SmartLog.d(LOG_TAG, "reset subscriptions...");
            if (playStateSubscription != null) {
                playStateSubscription.unsubscribe();
                volumeSubscription.unsubscribe();
            }

            playStateSubscription = new PlayStateSubscription();
            volumeSubscription = new VolumeSubscription();
        }

        if (applicationInfo.isRunning() && !deviceInfo.isTizen()) {
            //we should wait until smart view library launches the application
            remainingConnectionAttempts = MAX_LEGACY_SMART_VIEW_CONNECTION_ATTEMPTS;
            connectLegacyDevice(applicationInfo);
            return;
        }


        Map<String, String> clientAttributes = new HashMap<String, String>();
        clientAttributes.put("name", "SamsungSmartViewService");

        application.connect(clientAttributes, new Result<Client>() {
            @Override
            public void onSuccess(Client client) {
                SmartLog.d(LOG_TAG, "Connected to application " + application.getId());

                SmartLog.d(LOG_TAG, "client is host: " + client.isHost());
                SmartLog.d(LOG_TAG, "client connect time " + client.getConnectTime());
                SmartLog.d(LOG_TAG, "client attributes ");
                for (String key : client.getAttributes().keySet()) {
                    SmartLog.d(LOG_TAG, key + " " + client.getAttributes().get(key));
                }

                smartViewChannel = client.getChannel();

                receiverApp = application;

                smartViewChannel.setOnDisconnectListener(new Channel.OnDisconnectListener() {
                    @Override
                    public void onDisconnect(Client client) {
                        SmartLog.d(LOG_TAG, "Smart View channel disconnected");
                        disconnect();
//                        if (connected && (lastState == PlayStateStatus.Playing || lastState == PlayStateStatus.Buffering)) {
//                            restoreCastStateFromDisconnect();
//                        }
//                        prepareAndConnect(reconnectionAttempts == null ? MAX_RECONNECTION_ATTEMPTS : reconnectionAttempts - 1);
                    }
                });

                smartViewChannel.addOnMessageListener("message", new Channel.OnMessageListener() {
                    @Override
                    public void onMessage(Message message) {
                        for (ConvergenceListener nsListener : nsListeners) {
                            nsListener.onMessageReceived(message.getData().toString());
                        }
                    }
                });

                smartViewChannel.setOnErrorListener(new Channel.OnErrorListener() {
                    @Override
                    public void onError(Error error) {
                        SmartLog.e(LOG_TAG, "Smart View Channel crashed with error " + error.getMessage());
                    }
                });


                if (deviceInfo.isTizen()) {
                    if (applicationInfo.isRunning()) {
                        onConnectedToChannel();
                    } else {
                        smartViewChannel.setOnConnectListener(new Channel.OnConnectListener() {
                            @Override
                            public void onConnect(Client client) {
                                onConnectedToChannel();
                            }
                        });
                    }
                } else {
                    //we should wait until smart view library launches the application

                    remainingConnectionAttempts = MAX_LEGACY_SMART_VIEW_CONNECTION_ATTEMPTS;

                    connectLegacyDevice(applicationInfo);
                }

            }

            @Override
            public void onError(Error error) {
                SmartLog.e(LOG_TAG, "Could not connect to smart view channel " + error.getMessage() + ", error code " + error.getCode());

                int code = (int) error.getCode();

                final ServiceCommandError serviceCommandError = new ServiceCommandError(code, "Connection failed. " + error.getMessage());

                onConnectionFailure(serviceCommandError);
            }
        });
    }

    protected void restoreCastStateFromDisconnect() {
        SmartLog.i(LOG_TAG, "Smart View channel disconnected. Initiating reconnection attempt...");
        final long position = lastPosition;
        connectionListener = new DeviceServiceConnectionListener() {
            @Override
            public void onConnectionSuccess(DeviceService service) {
                super.onConnectionSuccess(service);
                SmartLog.i(LOG_TAG, "Smart View channel reconnected. Initiating state restoration...");
                blockStateReceiving = true;
                showProgressScreen(new ResponseListener<Object>() {
                    @Override
                    public void onSuccess(Object object) {
                        SmartLog.d(LOG_TAG, "show progress screen");
                        setMediaSource(lastMediaInfo, new LaunchListener() {
                            @Override
                            public void onSuccess(MediaLaunchObject object) {
                                SmartLog.i(LOG_TAG, "State restored. Initiating seek.");
                                blockStateReceiving = false;
                                seek(position, new ResponseListener<Object>() {
                                    @Override
                                    public void onSuccess(Object object) {
                                        SmartLog.i(LOG_TAG, "State restoration successful. Seek successful");
                                    }

                                    @Override
                                    public void onError(ServiceCommandError error) {
                                        SmartLog.e(LOG_TAG, "Could not seek to position. Continue without seek...");
                                    }
                                });
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                SmartLog.i(LOG_TAG, "Could not restore cast state. Disconnect.");
                                disconnect();
                            }
                        });
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        disconnect();
                    }
                });
            }
        };
    }

    private void connectLegacyDevice(final ApplicationInfo applicationInfo) {
        SmartLog.d(LOG_TAG, "Connect via Legacy Smart View library");

        final Device device = deviceInfo.getSmartViewLegacyDevice();

        if (device == null) {
            SmartLog.e(LOG_TAG, "Device should not be null!");
            final ServiceCommandError serviceCommandError = new ServiceCommandError(-1, "Device should not be null!");
            onConnectionFailure(serviceCommandError);
            return;
        }

        if (TextUtils.isEmpty(orsayAppId) || TextUtils.isEmpty(channelId)) {
            SmartLog.e(LOG_TAG, "App ID and Channel ID should not be empty!");
            final ServiceCommandError serviceCommandError = new ServiceCommandError(-1, "App ID and Channel ID should not be empty!");
            onConnectionFailure(serviceCommandError);
            return;
        }

        Map<String, String> clientAttributes = new HashMap<String, String>();
        clientAttributes.put("name", "Mobile Client");

        device.connectToChannel(channelId, clientAttributes, new DeviceAsyncResult<com.samsung.multiscreen.channel.Channel>() {

            @Override
            public void onResult(com.samsung.multiscreen.channel.Channel channel) {
                SmartLog.d(LOG_TAG, "Connected to legacy device " + device.getId());

                legacySmartViewChannel = channel;

                if (applicationInfo.isRunning()) {
                    onConnectedToChannel();
                }

                channel.setListener(new IChannelListener() {
                    @Override
                    public void onConnect() {
                        SmartLog.d(LOG_TAG, "Legacy Smart View channel connected");
                        if (!applicationInfo.isRunning()) {
                            onConnectedToChannel();
                        }
                    }

                    @Override
                    public void onDisconnect() {
                        SmartLog.d(LOG_TAG, "Legacy Smart View channel disconnected");
                        disconnect();
                    }

                    @Override
                    public void onClientConnected(ChannelClient channelClient) {
                        SmartLog.d(LOG_TAG, "Legacy Smart View client connected");
                    }

                    @Override
                    public void onClientDisconnected(ChannelClient channelClient) {
                        SmartLog.d(LOG_TAG, "Legacy Smart View client disconnected");
                    }

                    @Override
                    public void onClientMessage(ChannelClient channelClient, String message) {
                        SmartLog.d(LOG_TAG, "onClientMessage " + message);
                        for (ConvergenceListener nsListener : nsListeners) {
                            nsListener.onMessageReceived(message);
                        }
                    }
                });
            }

            // error callback
            public void onError(final DeviceError error) {
                SmartLog.e(LOG_TAG, "Could not connect to smartViewChannel " + error.getMessage() + ", error code " + error.getCode());

                if (remainingConnectionAttempts <= 0) {
                    ServiceCommandError serviceCommandError = new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed. " + error.getMessage());
                    onConnectionFailure(serviceCommandError);
                } else {
                    remainingConnectionAttempts--;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connectLegacyDevice(applicationInfo);
                }

            }
        });
    }

    private void onConnectedToChannel() {
        if (connectionListener != null) {
            connectionListener.onConnectionSuccess(this);
            connectionListener = null;
        }

        SmartLog.d(LOG_TAG, String.format("Channel %s is opened!", channelId));

        registerListener(playStateSubscription.getNsListener());
        registerListener(volumeSubscription.getNsListener());

        connected = true;
        reportConnected(true);
        sendClientInfo();
    }

    private void onConnectionFailure(ServiceCommandError serviceCommandError) {
        listener.onConnectionFailure(this, serviceCommandError);
    }

    private void registerListener(ConvergenceListener convergenceListener) {
        nsListeners.add(convergenceListener);
    }

    private void deleteListener(ConvergenceListener convergenceListener) {
        nsListeners.remove(convergenceListener);
    }

    /**
     * Will attempt to disconnect from the DeviceService. The failure/success will be reported back to the DeviceServiceListener.
     */
    public void disconnect() {
        SmartLog.d(LOG_TAG, "disconnect");
        connected = false;

        blockStateReceiving = false;

        connectionListener = null;

        sendDisconnectMsg();

        if (receiverApp != null) {
            receiverApp.disconnect(true, new Result<Client>() {
                @Override
                public void onSuccess(Client client) {
                    SmartLog.d(LOG_TAG, "receiver disconnected successfully! ");
                }

                @Override
                public void onError(Error error) {
                    SmartLog.d(LOG_TAG, "receiver disconnected with error, " + error.getMessage());
                }
            });
        }

        if (playStateSubscription != null) {
            if (currentPlaystate != MediaControl.PlayStateStatus.Finished && currentPlaystate != MediaControl.PlayStateStatus.Idle) {
                playStateSubscription.notifyListeners(MediaControl.PlayStateStatus.Finished);
            }

            playStateSubscription.unsubscribe();
        }

        if (volumeSubscription != null) {
            volumeSubscription.unsubscribe();
        }

        listener.onDisconnect(SamsungSmartViewService.this, null);
    }

    private void sendClientInfo() {
        SmartLog.d(LOG_TAG, "sendClientInfo " + clientVersion);

        HashMap<String, String> params = new HashMap<>();
        params.put(CLIENT_VERSION_EXTRA_KEY, clientVersion);
        params.put(CLIENT_APP_TYPE_EXTRA_KEY, clientAppType);

        sendMessage(SET_CLIENT_VERSION_REQUEST, params);
    }

    private void sendDisconnectMsg() {
        sendMessage(DISCONNECT_REQUEST, null);
    }

    private void sendMessage(String type, HashMap<String, String> params) {
        try {
            JSONObject msg = new JSONObject();
            msg.putOpt(MSG_TYPE, type);
            if (params != null) {
                for (String key : params.keySet()) {
                    msg.putOpt(key, params.get(key));
                }
            }

            if (deviceInfo.isTizen()) {
                if (smartViewChannel != null) {
                    smartViewChannel.publish("message", msg.toString());
                } else {
                    SmartLog.e(LOG_TAG, "Could not send message " + msg + "\n reason: channel not found");
                }
            } else {
                if (legacySmartViewChannel != null) {
                    legacySmartViewChannel.broadcast(msg.toString());
                } else {
                    SmartLog.e(LOG_TAG, "Could not send message " + msg + "\n reason: channel not found");
                }
            }
        } catch (Exception e) {
            SmartLog.e(LOG_TAG, "Could not send message! " + e.getMessage());
            e.printStackTrace();
        }

    }

    @NonNull
    private String getDeviceID(NSDevice device) {
        return device.getIP() + device.getName();
    }

    /**
     * Whether the DeviceService is currently connected
     */
    public boolean isConnected() {
        return connected;
    }

    public boolean isConnectable() {
        return true;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public void setTizenAppId(String appId) {
        this.tizenAppId = appId;
    }

    public void setOrsayAppId(String appId) {
        this.orsayAppId = appId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    @Override
    public void setServiceDescription(ServiceDescription serviceDescription) {
        super.setServiceDescription(serviceDescription);
        init(serviceDescription);
    }

    @Override
    public boolean isDeviceOnline() {
        return true;
    }

    public boolean isLegacyDevice() {
        return !deviceInfo.isTizen();
    }

    public void setClientAppType(String clientAppType) {
        this.clientAppType = clientAppType;
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private class PlayStateSubscription extends Subscription<MediaControl.PlayStateStatus, MediaControl.PlayStateListener> {

        private ConvergenceListener nsListener;

        public PlayStateSubscription(MediaControl.PlayStateListener listener) {
            super(listener);
            initNsListener();
        }

        public PlayStateSubscription() {
            initNsListener();
        }

        private void initNsListener() {
            setStatusRepetitionAllowed(true);
            nsListener = new ConvergenceListener("PlayStateSubscription") {
                @Override
                protected void onMessageReceived(JSONObject object) {
                    if (blockStateReceiving) {
                        return;
                    }

                    SmartLog.d(LOG_TAG, "onMessageReceived " + object.toString());

                    if (isTerminationStatus(object)) {
                        SmartLog.d(LOG_TAG, "disconnect from the device ");
                        disconnect();
                    } else if (isErrorMsg(object)) {
                        SmartLog.d(LOG_TAG, "received unsupported media error! ");
                        final ServiceCommandError error;
                        if (isUnsupportedFileError(object)) {
                            error = new ServiceCommandError(ServiceCommandError.MEDIA_UNSUPPORTED,
                                    "Media type is not supported!");
                        } else {
                            error = new ServiceCommandError(ServiceCommandError.INTERNAL_SERVER_ERROR,
                                    "Could not play content!");
                        }

                        notifyListeners(PlayStateStatus.UnsupportedFormatError);
                        notifyListenersError(error);
                    } else {
                        MediaControl.PlayStateStatus status = createPlayStateStatusFromFireTVStatus(object);
                        if (status != null) {
                            notifyListeners(status);
                        }
                    }
                }
            };
        }

        @Override
        public void unsubscribe() {
            deleteListener(nsListener);
        }

        public ConvergenceListener getNsListener() {
            return nsListener;
        }
    }

    private class VolumeSubscription extends Subscription<Float, VolumeControl.VolumeListener> {
        private ConvergenceListener nsListener;

        public VolumeSubscription(VolumeControl.VolumeListener listener) {
            super(listener);
            initNsListener();
        }

        public VolumeSubscription() {
            initNsListener();
        }

        private void initNsListener() {
            setStatusRepetitionAllowed(true);
            nsListener = new ConvergenceListener("VolumeSubscription") {
                @Override
                protected void onMessageReceived(JSONObject object) {
                    float volume = getVolumeFromStateObj(object);
                    if (volume >= 0) {
                        notifyListeners(volume);
                    }
                }
            };
        }

        @Override
        public void unsubscribe() {
            deleteListener(nsListener);
        }

        public ConvergenceListener getNsListener() {
            return nsListener;
        }
    }

    private abstract static class Subscription<Status, Listener extends ResponseListener<Status>>
            implements ServiceSubscription<Listener> {

        List<Listener> listeners = new ArrayList<Listener>();

        Status prevStatus;
        private boolean statusRepetitionAllowed = false;

        public Subscription() {
        }

        public Subscription(Listener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
        }

        synchronized void notifyListeners(final Status status) {
            if (!status.equals(prevStatus) || statusRepetitionAllowed) {
                List<Listener> listenersIterate = new ArrayList<Listener>();
                listenersIterate.addAll(listeners);

                for (Listener listener : listenersIterate) {
                    listener.onSuccess(status);
                }
            }
            prevStatus = status;
        }

        synchronized void notifyListenersError(ServiceCommandError error) {
            List<Listener> listenersIterate = new ArrayList<Listener>();
            listenersIterate.addAll(listeners);

            for (Listener listener : listenersIterate) {
                listener.onError(error);
            }
        }

        @Override
        public Listener addListener(Listener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
            return listener;
        }

        @Override
        public void removeListener(Listener listener) {
            listeners.remove(listener);
        }

        @Override
        public List<Listener> getListeners() {
            return listeners;
        }

        public void setStatusRepetitionAllowed(boolean statusRepetitionAllowed) {
            this.statusRepetitionAllowed = statusRepetitionAllowed;
        }
    }

    //  </editor-fold>

    //  <editor-fold desc="play listener container">

    private class ConvergenceListener extends NSListener {

        public ConvergenceListener(String tag) {
            super(tag);
        }

        @Override
        public void onWifiChanged() {

        }

        @Override
        public void onDeviceChanged(List<NSDevice> devices) {

        }

        @Override
        public void onConnected(NSDevice device) {

        }

        @Override
        public void onDisconnected() {
            SmartLog.d(LOG_TAG, "onDisconnected ");
        }

        @Override
        public void onConnectionFailed(int code) {

        }

        @Override
        public void onMessageSent() {

        }

        @Override
        public void onMessageSendFailed(int code) {

        }

        @Override
        public void onMessageReceived(String message) {
            try {
                final JSONObject messageObj = new JSONObject(message);
                if (isLogMsg(messageObj)) {
                    SmartLog.d(SMART_VIEW_TV_LOG_TAG, "" + getLogMsg(messageObj));
                }
                onMessageReceived(messageObj);
            } catch (JSONException e) {
                SmartLog.e(LOG_TAG, String.format("Could not parse msg from the receiver!\nmsg: %s\ncause: %s", message, e.getMessage()));
                e.printStackTrace();
            }
        }

        protected void onMessageReceived(JSONObject object) {

        }
    }

    //  </editor-fold>

    class DeviceServiceConnectionListener implements DeviceServiceListener {

        @Override
        public void onConnectionRequired(DeviceService service) {

        }

        @Override
        public void onConnectionSuccess(DeviceService service) {

        }

        @Override
        public void onCapabilitiesUpdated(DeviceService service, List<String> added, List<String> removed) {

        }

        @Override
        public void onDisconnect(DeviceService service, java.lang.Error error) {

        }

        @Override
        public void onConnectionFailure(DeviceService service, java.lang.Error error) {

        }

        @Override
        public void onPairingRequired(DeviceService service, PairingType pairingType, Object pairingData) {

        }

        @Override
        public void onPairingSuccess(DeviceService service) {

        }

        @Override
        public void onPairingFailed(DeviceService service, java.lang.Error error) {

        }
    }

}
