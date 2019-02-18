package com.connectsdk.service;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
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
import com.samsung.multiscreen.application.Application;
import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.channel.Channel;
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
 * Created by Dmitry on 27.04.17.
 */

public class SamsungLegacySmartViewService extends DeviceService implements MediaPlayer, MediaControl, VolumeControl, WakeControl {

    public static final String ID = "SamsungLegacySmartView";

    private static final String LOG_TAG = SamsungLegacySmartViewService.class.getSimpleName();

    private static final String AVPLAY_UNSUPPORTED_VIDEO_FORMAT_ERROR = "AVPlayUnsupportedVideoFormatError";
    private static final String AVPLAY_UNSUPPORTED_VIDEO_RESOLUTION_ERROR = "AVPlayUnsupportedVideoResolutionError";
    private static final String AVPLAY_UNSUPPORTED_VIDEO_FRAME_RATE_ERROR = "AVPlayUnsupportedVideoFrameRateError";
    private static final String AVPLAY_CORRUPTED_STREAM_ERROR = "AVPlayCorruptedStreamError";
    private static final String AVPLAY_INVALID_VALUES_ERROR = "InvalidValuesError";

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

    private static final String SET_CLIENT_VERSION_REQUEST = "set_client_version";

    private static final String URL_EXTRA_KEY = "url";
    private static final String TITLE_EXTRA_KEY = "title";
    private static final String CONTENT_EXTRA_KEY = "content";
    private static final String STATE_EXTRA_KEY = "state";
    private static final String POSITION_EXTRA_KEY = "position";
    private static final String MSG_EXTRA_KEY = "msg";
    private static final String CLIENT_VERSION_EXTRA_KEY = "client_version";

    private static final String CAST_TYPE_AUDIO = "audio";
    private static final String CAST_TYPE_IMAGE = "image";
    private static final String CAST_TYPE_VIDEO = "video";

    private static final String SUBTITLE_URL_EXTRA_KEY = "subtitle_url";

    private static final String VALUE_EXTRA_KEY = "value";

    private static final String VOLUME_EXTRA_KEY = "volume";

    private static final String TERMINATED_STATUS = "TERMINATED";
    private static final String MSG_TYPE_LOG = "log";

    private PlayStateSubscription playStateSubscription;
    private VolumeSubscription volumeSubscription;

    private PlayStateStatus currentPlaystate = PlayStateStatus.Unknown;

    private String clientVersion = "FREE";
    private String appId;
    private String channelId;

    private Device device;
    private Channel channel;

    private CopyOnWriteArrayList<ConvergenceListener> nsListeners
            = new CopyOnWriteArrayList<>();

    public SamsungLegacySmartViewService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        init(serviceDescription);
    }

    public SamsungLegacySmartViewService(ServiceConfig serviceConfig) {
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

        updateInfoFromDescription(serviceDescription);
    }

    @Override
    public CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> clazz) {
        if (clazz != null) {
            if (clazz.equals(MediaPlayer.class)) {
                return getMediaPlayerCapabilityLevel();
            } else if (clazz.equals(MediaControl.class)) {
                return getMediaControlCapabilityLevel();
            } else if (clazz.equals(VolumeControl.class)) {
                return getVolumeControlCapabilityLevel();
            } else if (clazz.equals(WakeControl.class)) {
                return CapabilityPriorityLevel.HIGH;
            }
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED;
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
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override
    public void hideProgressScreen(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override
    public MediaControl getMediaControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void play(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "play ");

        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "play state received " + object.name());
                if (object == PlayStateStatus.Playing) {
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

        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "pause state received " + object.name());
                if (object == PlayStateStatus.Paused) {
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

        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "stop state received " + object.name());
                if (object == PlayStateStatus.Finished || object == PlayStateStatus.Idle) {
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

        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "seek state received " + object.name());
                if (object == PlayStateStatus.Playing || object == PlayStateStatus.Paused) {
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
    public void getDuration(final DurationListener listener) {
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
    public void getPosition(final PositionListener listener) {
        SmartLog.d(LOG_TAG, "getPosition ");
        ConvergenceListener nsListener = new ConvergenceListener("getPosition") {
            @Override
            protected void onMessageReceived(JSONObject object) {
                SmartLog.d(LOG_TAG, "getPosition " + object.toString());
                long duration = getPositionFromResponse(object);
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
    public void getPlayState(final PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "getPlayState ");

        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "play state received " + object.name());
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
    public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "subscribePlayState ");
        if (!playStateSubscription.getListeners().contains(listener)) {
            playStateSubscription.addListener(listener);
        }
        getPlayState(listener);
        return playStateSubscription;
    }

    @Override
    public void unSubscribePlayState(PlayStateListener listener) {
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
                        )) {
                    SmartLog.d(LOG_TAG, "unsupported error! ");
                    return true;
                }
            }
        }

        return false;
    }


    private PlayStateStatus createPlayStateStatusFromFireTVStatus(JSONObject stateObj) {
        String type = stateObj.optString(MSG_TYPE);
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(MSG_TYPE_STATE)) {
                String state = stateObj.optString(STATE_EXTRA_KEY);
                if (state != null) {
                    switch (state) {
                        case "Error":
                            currentPlaystate = PlayStateStatus.Error;
                            break;
                        case "Buffering":
                            currentPlaystate = PlayStateStatus.Buffering;
                            break;
                        case "Playing":
                            currentPlaystate = PlayStateStatus.Playing;
                            break;
                        case "Paused":
                            currentPlaystate = PlayStateStatus.Paused;
                            break;
                        case "Stopped":
                            currentPlaystate = PlayStateStatus.Finished;
                            break;
                        case "Cancelled":
                            currentPlaystate = PlayStateStatus.Cancelled;
                            break;
                        default:
                            currentPlaystate = PlayStateStatus.Unknown;
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
            if (id != null && id.equals(appId) && status != null && status.equals(TERMINATED_STATUS)) {
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
    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void getMediaInfo(MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<MediaInfoListener> subscribeMediaInfo(MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
        return null;
    }

    @Override
    public void displayImage(MediaInfo mediaInfo, LaunchListener listener) {
        setMediaSource(mediaInfo, listener);
    }

    @Override
    public void playMedia(MediaInfo mediaInfo, boolean shouldLoop, LaunchListener listener) {
        setMediaSource(mediaInfo, listener);
    }

    @Override
    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener) {
        stop(listener);
    }

    @Override
    public void displayImage(String url, String mimeType, String title, String description, String iconSrc, LaunchListener listener) {
        setMediaSource(new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build(), listener);
    }

    @Override
    public void playMedia(String url, String mimeType, String title, String description, String iconSrc, boolean shouldLoop, LaunchListener listener) {
        setMediaSource(new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build(), listener);
    }

    private void setMediaSource(MediaInfo mediaInfo, final LaunchListener listener) {
        SmartLog.d(LOG_TAG, "setMediaSource " + mediaInfo.getTitle());

        if (TextUtils.isEmpty(mediaInfo.getMimeType())) {
            listener.onError(new ServiceCommandError("Wrong media info: mime type is null!"));
        }

        HashMap<String, String> params = new HashMap<>();
        String url = mediaInfo.getUrl();
        params.put(URL_EXTRA_KEY, url);

        String castType;
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

        playStateSubscription.addListener(new PlayStateListener() {
            public boolean ignoreFirstStatus = true;

            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "[play] state received " + object.name());
                if (object == PlayStateStatus.Playing) {
                    if (!ignoreFirstStatus) {
                        SmartLog.i(LOG_TAG, "play successful! ");
                        listener.onSuccess(createMediaLaunchObject());
                        playStateSubscription.removeListener(this);
                    }
                    ignoreFirstStatus = false;
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "[play] onError ");

                if (error.getCode() == ServiceCommandError.MEDIA_UNSUPPORTED) {
                    SmartLog.e(LOG_TAG, "[play] media format is not supported! ");
                }

                listener.onError(error);

                playStateSubscription.removeListener(this);
            }
        });
    }

    private MediaLaunchObject createMediaLaunchObject() {
        LaunchSession launchSession = new LaunchSession();
        launchSession.setService(this);
        launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
        launchSession.setAppId(device.getId());
        launchSession.setAppName(device.getName());
        MediaLaunchObject mediaLaunchObject = new MediaLaunchObject(launchSession, this);
        return mediaLaunchObject;
    }

    //  </editor-fold>

    //  <editor-fold desc="volume control">

    @Override
    public VolumeControl getVolumeControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
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

        volumeSubscription.addListener(new VolumeListener() {
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
    public void getVolume(final VolumeListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void setMute(boolean isMute, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void getMute(MuteListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener listener) {
        SmartLog.i(LOG_TAG, "subscribeVolume ");
        volumeSubscription.addListener(listener);
        return volumeSubscription;
    }

    @Override
    public ServiceSubscription<MuteListener> subscribeMute(MuteListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
        return null;
    }

    //  </editor-fold>

    //  <editor-fold desc="connection">

    /**
     * Will attempt to connect to the DeviceService. The failure/success will be reported back to the DeviceServiceListener. If the connection attempt reveals that pairing is required, the DeviceServiceListener will also be notified in that event.
     */
    public void connect() {
        prepareAndConnect();
    }

    private void prepareAndConnect() {
        SmartLog.d(LOG_TAG, "prepareAndConnect ");

        if (device == null) {
            SmartLog.e(LOG_TAG, "Device should not be null!");
            onConnectionFailure(new ServiceCommandError(-1, "Device should not be null!"));
            return;
        }

        device.getApplication(appId, new DeviceAsyncResult<Application>() {
            @Override
            public void onResult(final Application application) {
                SmartLog.d(LOG_TAG, "got application object with status " + application.getLastKnownStatus());
                switch (application.getLastKnownStatus()) {
                    case INSTALLABLE:
                        installAndConnect(application);
                        break;
                    case RUNNING:
                        connectToDevice();
                        break;
                    case STOPPED:
                        launchAndConnect(application);
                        break;
                }
            }

            @Override
            public void onError(DeviceError deviceError) {
                SmartLog.e(LOG_TAG, "Could not get application " + deviceError.getMessage());
//                ServiceCommandError serviceCommandError =
//                        new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed. " + deviceError.getMessage());
//                onConnectionFailure(serviceCommandError);

                connectToDevice();
            }
        });
    }

    public void launchAndConnect(Application application) {
        SmartLog.d(LOG_TAG, "launchAndConnect " + application.getRunTitle());

        application.launch(new ApplicationAsyncResult<Boolean>() {
            @Override
            public void onResult(Boolean aBoolean) {
                SmartLog.d(LOG_TAG, "application launch successful " + aBoolean);
                if (aBoolean) {
                    connectToDevice();
                } else {
                    SmartLog.e(LOG_TAG, "Could not launch application!");
                    ServiceCommandError serviceCommandError =
                            new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed.");
                    onConnectionFailure(serviceCommandError);
                }
            }

            @Override
            public void onError(ApplicationError applicationError) {
                SmartLog.e(LOG_TAG, "Could not launch application! " + applicationError.getMessage());
                ServiceCommandError serviceCommandError =
                        new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed. " + applicationError.getMessage());
                onConnectionFailure(serviceCommandError);
            }
        });
    }

    public void installAndConnect(Application application) {
        SmartLog.d(LOG_TAG, "install application " + application.getRunTitle());

        application.install(new ApplicationAsyncResult<Boolean>() {
            @Override
            public void onResult(Boolean aBoolean) {
                SmartLog.d(LOG_TAG, "application installed " + aBoolean);
                if (aBoolean) {
                    prepareAndConnect();
                } else {
                    SmartLog.e(LOG_TAG, "Could not install application!");
                    ServiceCommandError serviceCommandError =
                            new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed.");
                    onConnectionFailure(serviceCommandError);
                }
            }

            @Override
            public void onError(ApplicationError applicationError) {
                SmartLog.e(LOG_TAG, "Could not install application! " + applicationError.getMessage());
                ServiceCommandError serviceCommandError =
                        new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed. " + applicationError.getMessage());
                onConnectionFailure(serviceCommandError);
            }
        });
    }

    private void connectToDevice() {
        SmartLog.d(LOG_TAG, "connect ");

        if (device == null) {
            SmartLog.e(LOG_TAG, "Device should not be null!");
            final ServiceCommandError serviceCommandError = new ServiceCommandError(-1, "Device should not be null!");
            onConnectionFailure(serviceCommandError);
            return;
        }

        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(channelId)) {
            SmartLog.e(LOG_TAG, "App ID or Channel ID is empty!");
            final ServiceCommandError serviceCommandError = new ServiceCommandError(-1, "App ID or Channel ID is empty!");
            onConnectionFailure(serviceCommandError);
            return;
        }

        channel = null;

        if (playStateSubscription != null) {
            playStateSubscription.unsubscribe();
            volumeSubscription.unsubscribe();
        }

        playStateSubscription = new PlayStateSubscription();
        volumeSubscription = new VolumeSubscription();

        SmartLog.d(LOG_TAG, "connecting to " + device.getIPAddress() + " " + device.getName());

        Map<String, String> clientAttributes = new HashMap<String, String>();
        clientAttributes.put("name", "Mobile Client");

        device.connectToChannel(channelId, clientAttributes, new DeviceAsyncResult<Channel>() {

            @Override
            public void onResult(Channel channel) {
                SmartLog.d(LOG_TAG, "On Channel connected! ");

                SamsungLegacySmartViewService.this.channel = channel;

                channel.setListener(new IChannelListener() {
                    @Override
                    public void onConnect() {
                        SmartLog.d(LOG_TAG, "Channel onConnect");
                    }

                    @Override
                    public void onDisconnect() {
                        SmartLog.d(LOG_TAG, "Channel onDisconnect");
                    }

                    @Override
                    public void onClientConnected(ChannelClient channelClient) {
                        SmartLog.d(LOG_TAG, "onClientConnected ");
                    }

                    @Override
                    public void onClientDisconnected(ChannelClient channelClient) {
                        SmartLog.d(LOG_TAG, "onClientDisconnected ");
                    }

                    @Override
                    public void onClientMessage(ChannelClient channelClient, String message) {
                        SmartLog.d(LOG_TAG, "onClientMessage " + message);
                        for (ConvergenceListener nsListener : nsListeners) {
                            nsListener.onMessageReceived(message);
                        }
                    }
                });

                registerListener(playStateSubscription.getNsListener());
                registerListener(volumeSubscription.getNsListener());

                connected = true;
                reportConnected(true);
                sendClientInfo();
            }

            // error callback
            public void onError(final DeviceError error) {
                SmartLog.e(LOG_TAG, "Could not connect to channel " + error.getMessage() + ", error code " + error.getCode());

                final ServiceCommandError serviceCommandError = new ServiceCommandError(ServiceCommandError.RECEIVER_APP_NOT_INSTALLED, "Connection failed. " + error.getMessage());

                onConnectionFailure(serviceCommandError);
            }
        });
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

        sendDisconnectMsg();

        if (playStateSubscription != null) {
            if (currentPlaystate != PlayStateStatus.Finished && currentPlaystate != PlayStateStatus.Idle) {
                playStateSubscription.notifyListeners(PlayStateStatus.Finished);
            }

            playStateSubscription.unsubscribe();
        }

        if (volumeSubscription != null) {
            volumeSubscription.unsubscribe();
        }
    }

    private void sendClientInfo() {
        HashMap<String, String> params = new HashMap<>();
        params.put(CLIENT_VERSION_EXTRA_KEY, clientVersion);

        sendMessage(SET_CLIENT_VERSION_REQUEST, params);
    }

    private void sendDisconnectMsg() {
        sendMessage(DISCONNECT_REQUEST, null);
    }

    private void updateInfoFromDescription(ServiceDescription serviceDescription) {
        if (serviceDescription != null
                && serviceDescription.getDevice() instanceof Device) {
            this.device = ((Device) serviceDescription.getDevice());
        } else {
            this.device = null;
        }
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
            if (channel != null) {
                channel.broadcast(msg.toString());
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

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    @Override
    public void setServiceDescription(ServiceDescription serviceDescription) {
        super.setServiceDescription(serviceDescription);
        updateInfoFromDescription(serviceDescription);
    }

    @Override
    public boolean isDeviceOnline() {
        return true;
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private class PlayStateSubscription extends Subscription<PlayStateStatus, PlayStateListener> {

        private ConvergenceListener nsListener;

        public PlayStateSubscription(PlayStateListener listener) {
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
                    if (isTerminationStatus(object)) {
                        SmartLog.d(LOG_TAG, "disconnect from the device ");
                        disconnect();
                        listener.onDisconnect(SamsungLegacySmartViewService.this, null);
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

                        notifyListenersError(error);
                    } else {
                        PlayStateStatus status = createPlayStateStatusFromFireTVStatus(object);
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

    private class VolumeSubscription extends Subscription<Float, VolumeListener> {
        private ConvergenceListener nsListener;

        public VolumeSubscription(VolumeListener listener) {
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
                    SmartLog.d(appId, "" + getLogMsg(messageObj));
                } else {
                    SmartLog.d(LOG_TAG, "onMessageReceived " + message);
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

}
