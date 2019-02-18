package com.connectsdk.service;

import android.net.Uri;
import android.text.TextUtils;
import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.provider.samsung.SmartViewErrorCodes;
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
import com.connectsdk.service.samsung.CastManager;
import com.connectsdk.service.samsung.OnBLEDiscoveryListener;
import com.connectsdk.service.samsung.OnFCErrorListener;
import com.connectsdk.service.samsung.OnTVMessageListener;
import com.connectsdk.service.samsung.exceptions.FCError;
import com.connectsdk.service.samsung.model.CastStates;
import com.connectsdk.service.samsung.model.ConnectData;
import com.connectsdk.service.samsung.msgs.ClientInfoMessage;
import com.connectsdk.service.samsung.msgs.CustomEvent;
import com.connectsdk.service.samsung.msgs.MessageType;
import com.connectsdk.service.samsung.msgs.PlayMessage;
import com.connectsdk.service.samsung.msgs.SimpleTextMessage;
import com.connectsdk.service.samsung.msgs.StatusMessage;
import com.connectsdk.service.samsung.statemachine.StateChangeArgs;
import com.connectsdk.service.samsung.statemachine.StateChangeListener;
import com.connectsdk.service.sessions.LaunchSession;
import com.samsung.multiscreenfix.Error;
import com.samsung.multiscreenfix.Message;
import com.samsung.multiscreenfix.Result;
import com.samsung.multiscreenfix.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dmitry on 04.01.17.
 */

public class SamsungFastCastService extends DeviceService implements MediaPlayer, MediaControl, VolumeControl, WakeControl {

    private static final String LOG_TAG = SamsungFastCastService.class.getSimpleName();

    public static final String ID = "SamsungSmartTv";
    private static final String SMART_TV_LOG_TAG = "SMART_TV";

    private static final int DEFAULT_VIDEO_ID = 1;
    private static final long DEFAULT_START_POSITION = 0;

    private static final String STATUS_REQUEST = "request_status";

    private static final String URL_EXTRA_KEY = "url";

    private static final String CAST_TYPE_AUDIO = "audio";
    private static final String CAST_TYPE_IMAGE = "image";
    private static final String CAST_TYPE_VIDEO = "video";
    private static final String CAST_TYPE_EXTRA_KEY = "cast_type";

    private static final String TITLE_EXTRA_KEY = "title";
    private static final String DESCRIPTION_EXTRA_KEY = "description";
    private static final String ICON_URL_EXTRA_KEY = "icon_url";

    private static final String SUBTITLE_URL_EXTRA_KEY = "url";
    private static final String SUBTITLE_TITLE_EXTRA_KEY = "title";
    private static final String SUBTITLE_MIME_TYPE_EXTRA_KEY = "mime_type";
    private static final String SUBTITLE_INFO_EXTRA_KEY = "subtitle_info";

    private CastManager mCastManager;

    private Service service;

    private PlayStateSubscription playStateSubscription;
    private VolumeSubscription volumeSubscription;

    private PlayStateStatus currentPlaystate = PlayStateStatus.Unknown;

    private String clientVersion = "FREE";
    private String appId;
    private String channelId;
    private String deviceMac;
    private Uri deviceUri;

    public SamsungFastCastService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        init(serviceDescription);
    }

    public SamsungFastCastService(ServiceConfig serviceConfig) {
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

    private void init(ServiceDescription serviceDescription) {
        SmartLog.d(LOG_TAG, "init ");
        mCastManager = CastManager.getInstance();

        updateInfoFromDescription(serviceDescription);

        playStateSubscription = new PlayStateSubscription();
        volumeSubscription = new VolumeSubscription();

        mCastManager.addOnTVMessageListener(playStateSubscription);
        mCastManager.addOnTVMessageListener(volumeSubscription);

        mCastManager.addOnBLEDiscoveryListener(mBLEListener);
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
        mCastManager.getRemoteController().playPause();
        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "play state received " + object.name());
                if (object == PlayStateStatus.Playing || object == PlayStateStatus.Paused) {
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
    }

    @Override
    public void changeSubtitles(SubtitleInfo info, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported());
    }

    @Override
    public void pause(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "pause ");
        mCastManager.getRemoteController().playPause();
        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "pause state received " + object.name());
                if (object == PlayStateStatus.Playing || object == PlayStateStatus.Paused) {
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
        mCastManager.getRemoteController().stop();
    }

    @Override
    public void rewind(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "rewind ");
        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "rewind state received " + object.name());
                if (object == PlayStateStatus.Playing || object == PlayStateStatus.Paused) {
                    SmartLog.d(LOG_TAG, "rewind successful! ");
                    listener.onSuccess(object);
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "rewind onError ");
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });
        mCastManager.getRemoteController().rewind();
    }

    @Override
    public void fastForward(final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "fastForward ");
        playStateSubscription.addListener(new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "fastForward state received " + object.name());
                if (object == PlayStateStatus.Playing || object == PlayStateStatus.Paused) {
                    SmartLog.d(LOG_TAG, "fastForward successful! ");
                    listener.onSuccess(object);
                    playStateSubscription.removeListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "fastForward onError ");
                listener.onError(error);
                playStateSubscription.removeListener(this);
            }
        });
        mCastManager.getRemoteController().fastForward();
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
        mCastManager.getRemoteController().seek(position);
    }

    @Override
    public void getDuration(final DurationListener listener) {
        SmartLog.d(LOG_TAG, "getPosition ");


        mCastManager.addOnTVMessageListener(new PlayStateSubscription() {
            @Override
            public void onStatusMessage(StatusMessage msg) {
                SmartLog.d(LOG_TAG, "position received! ");
                long duration = msg.getTotalTime();
                listener.onSuccess(duration);
                unsubscribe();
            }
        });

        final SimpleTextMessage simpleTextMessage = new SimpleTextMessage();
        simpleTextMessage.setMessage(STATUS_REQUEST);
        mCastManager.send(MessageType.TEXT_MESSAGE, simpleTextMessage);
    }

    @Override
    public void getPosition(final PositionListener listener) {
        SmartLog.d(LOG_TAG, "getPosition ");
        mCastManager.addOnTVMessageListener(new PlayStateSubscription() {
            @Override
            public void onStatusMessage(StatusMessage msg) {
                SmartLog.d(LOG_TAG, "position received! ");
                long position = msg.getPosition();
                listener.onSuccess(position);
                unsubscribe();
            }
        });

        final SimpleTextMessage simpleTextMessage = new SimpleTextMessage();
        simpleTextMessage.setMessage(STATUS_REQUEST);
        mCastManager.send(MessageType.TEXT_MESSAGE, simpleTextMessage);
    }

    @Override
    public void getPlayState(PlayStateListener listener) {
        SmartLog.d(LOG_TAG, "getPlayState ");
        mCastManager.addOnTVMessageListener(new PlayStateSubscription(listener) {
            @Override
            public void onStatusMessage(StatusMessage msg) {
                SmartLog.d(LOG_TAG, "play state received! ");
                super.onStatusMessage(msg);
                unsubscribe();
            }
        });

        final SimpleTextMessage simpleTextMessage = new SimpleTextMessage();
        simpleTextMessage.setMessage(STATUS_REQUEST);
        mCastManager.send(MessageType.STATUS, simpleTextMessage);
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

    private PlayStateStatus createPlayStateStatusFromFireTVStatus(StatusMessage msg) {
        PlayStateStatus playState = PlayStateStatus.Unknown;
        switch (msg.getState()) {
            case IDLE:
                playState = PlayStateStatus.Idle;
                break;
            case BUFFERING:
                playState = PlayStateStatus.Buffering;
                break;
            case READY:
                playState = PlayStateStatus.Buffering;
                break;
            case PLAYING:
                playState = PlayStateStatus.Playing;
                break;
            case PAUSED:
                playState = PlayStateStatus.Paused;
                break;
            case ERROR:
                playState = PlayStateStatus.Error;
                break;
            case STOPPED:
                playState = PlayStateStatus.Finished;
                break;
        }
        if (playState == PlayStateStatus.Idle
                && (currentPlaystate == PlayStateStatus.Playing
                || currentPlaystate == PlayStateStatus.Paused)) {
            playState = PlayStateStatus.Finished;
        }
        currentPlaystate = playState;
        return playState;
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

        JSONObject castData = null;
        try {
            castData = new JSONObject();

            String url = mediaInfo.getUrl();
            castData.put(URL_EXTRA_KEY, url);

            String castType;
            if (mediaInfo.getMimeType().contains("audio")) {
                castType = CAST_TYPE_AUDIO;
            } else if (mediaInfo.getMimeType().contains("image")) {
                castType = CAST_TYPE_IMAGE;
            } else {
                castType = CAST_TYPE_VIDEO;
            }

            castData.put(CAST_TYPE_EXTRA_KEY, castType);

            String title = mediaInfo.getTitle();
            castData.put(TITLE_EXTRA_KEY, title);

            String description = mediaInfo.getDescription();
            castData.put(DESCRIPTION_EXTRA_KEY, description);

            String iconUrl = null;

            if (mediaInfo.getImages() != null) {
                final ImageInfo imageInfo = mediaInfo.getImages().get(0);
                if (imageInfo != null) {
                    iconUrl = imageInfo.getUrl();
                }
            }

            castData.put(ICON_URL_EXTRA_KEY, iconUrl);

            SubtitleInfo subtitleInfo = mediaInfo.getSubtitleInfo();
            if (subtitleInfo != null) {
                String subUrl = subtitleInfo.getUrl();
                String subTitle = subtitleInfo.getLabel();
                String subMimeType = subtitleInfo.getMimeType();

                JSONObject subtitleInfoJson = new JSONObject();
                subtitleInfoJson.put(SUBTITLE_URL_EXTRA_KEY, subUrl);
                subtitleInfoJson.put(SUBTITLE_TITLE_EXTRA_KEY, subTitle);
                subtitleInfoJson.put(SUBTITLE_MIME_TYPE_EXTRA_KEY, subMimeType);

                castData.put(SUBTITLE_INFO_EXTRA_KEY, subtitleInfoJson);
            }

            SmartLog.d(LOG_TAG, "start playback of " + url);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        mCastManager.getRemoteController().startPlayback(DEFAULT_VIDEO_ID, DEFAULT_START_POSITION, castData);

        final PlayListener playListener = new PlayListener() {

            @Override
            public void onSuccess(PlayStateStatus object) {
                SmartLog.d(LOG_TAG, "[play] state received " + object.name());
                if (object == PlayStateStatus.Playing) {
                    SmartLog.d(LOG_TAG, "play successful! ");
                    listener.onSuccess(createMediaLaunchObject());

                    playStateSubscription.removeListener(this);
                    mCastManager.removeOnFCErrorListener(this);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                SmartLog.e(LOG_TAG, "[play] onError ");
                listener.onError(error);

                playStateSubscription.removeListener(this);
                mCastManager.removeOnFCErrorListener(this);
            }

            @Override
            public void OnError(FCError error) {
                SmartLog.e(LOG_TAG, "[play] onError " + error.getCode());

                final ServiceCommandError commandError;
                if (error.getCode() == SmartViewErrorCodes.MEDIA_NOT_SUPPORTED) {
                    commandError = new ServiceCommandError(ServiceCommandError.MEDIA_UNSUPPORTED, "Media type is not supported!");
                } else {
                    commandError = new ServiceCommandError(error.getCode(), error.getMessage());
                }
                listener.onError(commandError);

                playStateSubscription.removeListener(this);
                mCastManager.removeOnFCErrorListener(this);
            }
        };

        mCastManager.addOnFCErrorListener(playListener);

        playStateSubscription.addListener(playListener);
    }

    private MediaLaunchObject createMediaLaunchObject() {
        LaunchSession launchSession = new LaunchSession();
        launchSession.setService(this);
        launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
        launchSession.setAppId(service.getId());
        launchSession.setAppName(service.getName());
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
        mCastManager.addOnTVMessageListener(new PlayStateSubscription() {
            @Override
            public void onStatusMessage(StatusMessage msg) {
                SmartLog.d(LOG_TAG, "volume received! ");
                int volume = msg.getVolume();
                if (volume < 100) {
                    volume = Math.abs(volume + 1);
                    mCastManager.getRemoteController().setVolume(volume);
                }
                listener.onSuccess(createPlayStateStatusFromFireTVStatus(msg));
                unsubscribe();
            }
        });
    }

    @Override
    public void volumeDown(final ResponseListener<Object> listener) {
        mCastManager.addOnTVMessageListener(new PlayStateSubscription() {
            @Override
            public void onStatusMessage(StatusMessage msg) {
                SmartLog.d(LOG_TAG, "volume received! ");
                int volume = msg.getVolume();
                if (volume > -100 && volume != 0) {
                    volume = Math.abs(volume) - 1;
                    mCastManager.getRemoteController().setVolume(volume);
                }
                listener.onSuccess(createPlayStateStatusFromFireTVStatus(msg));
                unsubscribe();
            }
        });
    }

    @Override
    public void setVolume(float volume, final ResponseListener<Object> listener) {
        SmartLog.d(LOG_TAG, "setVolume " + volume);
        mCastManager.getRemoteController().setVolume((int) volume);
        getVolume(new VolumeListener() {
            @Override
            public void onSuccess(Float object) {
                listener.onSuccess(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                listener.onError(error);
            }
        });
    }

    @Override
    public void getVolume(final VolumeListener listener) {
        mCastManager.addOnTVMessageListener(new PlayStateSubscription() {
            @Override
            public void onStatusMessage(StatusMessage msg) {
                SmartLog.d(LOG_TAG, "volume received! ");
                int volume = msg.getVolume();
                listener.onSuccess((float) volume);
                unsubscribe();
            }
        });
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
        if (isDeviceOnline()) {
            connectToDevice();
        } else {
            wakeAndConnect();
        }
    }

    private void wakeAndConnect() {
        SmartLog.d(LOG_TAG, "wakeAndConnect ");
        if (TextUtils.isEmpty(deviceMac)) {
            SmartLog.e(LOG_TAG, "Could not retrieve device mac!");
            listener.onConnectionFailure(SamsungFastCastService.this, new ServiceCommandError(-1, "Could not retrieve device mac!"));
            return;
        }

        SmartLog.d(LOG_TAG, "connect to  " + deviceMac + "\n" + deviceUri.toString());

        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                Service.WakeOnWirelessAndConnect(deviceMac, deviceUri, 60000, new Result<Service>() {
                    @Override
                    public void onError(Error arg0) {
                        SmartLog.e(LOG_TAG, "WakeOnWirelessAndConnect onError " + arg0.getMessage());
                        listener.onConnectionFailure(SamsungFastCastService.this, new ServiceCommandError(-1, "WOW Timeout. Wake on WiFi failed."));
                    }

                    @Override
                    public void onSuccess(Service service) {
                        SmartLog.d(LOG_TAG, "TV wake was successful! ");
                        SamsungFastCastService.this.service = service;
                        connectToDevice();
                    }
                });
            }
        });
    }

    private void connectToDevice() {
        SmartLog.d(LOG_TAG, "connect " + appId + " " + channelId + " " + clientVersion);

        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(channelId)) {
            SmartLog.e(LOG_TAG, "App ID or Channel ID is empty! ");
            listener.onConnectionFailure(SamsungFastCastService.this, new ServiceCommandError(-1, "App ID or Channel ID is empty!"));
            return;
        }

        // Connection attributes
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("name", "Mobile");

        // Creates connection and sets AppId and ChannelId
        ConnectData connectionParams = new ConnectData();
        connectionParams.setAppId(appId);
        connectionParams.setChannelId(channelId);

        Map<String, Object> startArgs = new HashMap<>();
        startArgs.put(Message.PROPERTY_MESSAGE_ID, clientVersion);
        connectionParams.setStartArgs(startArgs);

        // Get from adapter clicked service and set it to connect with
        connectionParams.setService(service);
        connectionParams.setConnectionAttributes(attributes);

        final OnFCErrorListener errorListener = new OnFCErrorListener() {
            @Override
            public void OnError(FCError error) {
                int code = error.getCode();
                String msg = "Unknown error with code " + code;
                SmartLog.e(LOG_TAG, "OnFCErrorListener on Error! ");
                // Here you can handle msf lib errors
                if (code == SmartViewErrorCodes.ACCESS_DENIED_ERROR/*403*/) { // Access denied error
                    //TODO handle
                    msg = "Access denied error!";
                } else if (code == 200) {
                    if (error.getMsfError().getCode() == SmartViewErrorCodes.NOT_TV_APP_ERROR/*404*/) { // No TV app
                        msg = "Receiver application is not installed!";
                        code = ServiceCommandError.RECEIVER_APP_NOT_INSTALLED;
                    }
                } else if (code == SmartViewErrorCodes.TIMEOUT_ERROR/*110*/) { // timeout (No TV app installed?)
                    //TODO handle
                    msg = "Time out error!";
                } else if (code == SmartViewErrorCodes.WOW_ERROR/*120*/) {
                    msg = "WoW error!";
                } else if (code != SmartViewErrorCodes.DISCONNECT_FAILED) {
                    msg = "Disconnect failed!";
                    mCastManager.disconnect();
                } else if (error.getMessage() != null) {
                    msg = error.getMessage();
                }
                SmartLog.e(LOG_TAG, "[connect] OnError " + code + " " + msg);
                listener.onConnectionFailure(SamsungFastCastService.this, new ServiceCommandError(code, msg));
                mCastManager.removeOnFCErrorListener(this);
            }
        };

        StateChangeListener<CastStates> stateChangeListener = new StateChangeListener<CastStates>() {

            @Override
            public void stateChanged(StateChangeArgs<CastStates> args) {
                SmartLog.d(LOG_TAG, "[connect] stateChangeListener " + args.state_new.name());
                switch (args.state_new) {
                    case READY:
                        connected = true;
                        reportConnected(true);
                        sendClientInfo();
                        unsubscribeListener();
                        break;
                    case CONNECTED:
                        // TV app and mobile app connected, but not yet ready
                        break;
                    case CONNECTING:
                        break;
                    case INSTALLING:
                        // TV App installing state
                        break;
                    case IDLE:
                        break;
                }
            }

            private void unsubscribeListener() {
                mCastManager.onStateStarts.removeListener(this);
                mCastManager.removeOnFCErrorListener(errorListener);
            }
        };

        mCastManager.onStateStarts.addListener(stateChangeListener);
        mCastManager.addOnFCErrorListener(errorListener);

        // Send connection request with connection data
        mCastManager.connect(connectionParams, 60*1000);
    }

    /**
     * Will attempt to disconnect from the DeviceService. The failure/success will be reported back to the DeviceServiceListener.
     */
    public void disconnect() {
        SmartLog.d(LOG_TAG, "disconnect");
        connected = false;

        mCastManager.disconnect(true);

        if (currentPlaystate != PlayStateStatus.Finished && currentPlaystate != PlayStateStatus.Idle) {
            playStateSubscription.notifyListeners(PlayStateStatus.Finished);
        }
    }

    private void sendClientInfo() {
        final ClientInfoMessage message = new ClientInfoMessage();
        message.setVersion(clientVersion);
        mCastManager.send(MessageType.CLIENTINFO, message);
    }

    private void updateInfoFromDescription(ServiceDescription serviceDescription) {
        if (serviceDescription != null
                && serviceDescription.getDevice() instanceof Service) {
            this.service = (Service) serviceDescription.getDevice();
        } else if (serviceDescription.getWifiMac() != null) {
            SmartLog.d(LOG_TAG, "service in standby mode ");
            this.deviceMac = serviceDescription.getWifiMac();
            this.deviceUri = Uri.parse(serviceDescription.getIpAddress());
            this.service = null;
        } else {
            SmartLog.wtf(LOG_TAG, "service is empty! ");
            this.service = null;
        }
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
        SmartLog.d(LOG_TAG, "isDeviceOnline " + (service != null));
        return service != null;
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private class PlayStateSubscription extends Subscription<PlayStateStatus, PlayStateListener>
            implements OnTVMessageListener {
        public PlayStateSubscription(PlayStateListener listener) {
            super(listener);
        }

        public PlayStateSubscription() {
        }

        @Override
        public void unsubscribe() {
            mCastManager.removeOnTVMessageListener(this);
        }

        @Override
        public void onStatusMessage(StatusMessage msg) {
            final PlayStateStatus status = createPlayStateStatusFromFireTVStatus(msg);
            notifyListeners(status);
        }

        @Override
        public void onPlayMessage(PlayMessage msg) {
            SmartLog.i(LOG_TAG, "onPlayMessage " + msg);
        }

        @Override
        public void onSimpleTextMessage(SimpleTextMessage msg) {
            SmartLog.i(SMART_TV_LOG_TAG, msg.getMessage());
        }

        @Override
        public void onSuspendMessage() {
            SmartLog.i(LOG_TAG, "onSuspendMessage ");
        }

        @Override
        public void onRestoreMessage() {
            SmartLog.i(LOG_TAG, "onRestoreMessage ");
        }

        @Override
        public void onCustomEventMessage(CustomEvent eventMessage) {
            SmartLog.i(LOG_TAG, "onCustomEventMessage " + eventMessage.getDataJSON());
        }
    }

    private class VolumeSubscription extends Subscription<Float, VolumeListener>
            implements OnTVMessageListener {
        public VolumeSubscription(VolumeListener listener) {
            super(listener);
        }

        public VolumeSubscription() {
            setStatusRepetitionAllowed(true);
        }

        @Override
        public void unsubscribe() {
            mCastManager.removeOnTVMessageListener(this);
        }

        @Override
        public void onStatusMessage(StatusMessage msg) {
            SmartLog.d(LOG_TAG, "onVolumeReceived " + msg.getVolume());
            float volume = msg.getVolume();
            notifyListeners(volume);
        }

        @Override
        public void onPlayMessage(PlayMessage msg) {
            SmartLog.i(LOG_TAG, "onPlayMessage " + msg);
        }

        @Override
        public void onSimpleTextMessage(SimpleTextMessage msg) {
            SmartLog.i(SMART_TV_LOG_TAG, msg.getMessage());
        }

        @Override
        public void onSuspendMessage() {
            SmartLog.i(LOG_TAG, "onSuspendMessage ");
        }

        @Override
        public void onRestoreMessage() {
            SmartLog.i(LOG_TAG, "onRestoreMessage ");
        }

        @Override
        public void onCustomEventMessage(CustomEvent eventMessage) {
            SmartLog.i(LOG_TAG, "onCustomEventMessage " + eventMessage.getDataJSON());
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
                Util.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        List<Listener> listenersIterate = new ArrayList<Listener>();
                        listenersIterate.addAll(listeners);

                        for (Listener listener : listenersIterate) {
                            listener.onSuccess(status);
                        }
                    }
                });
                prevStatus = status;
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

    interface PlayListener extends OnFCErrorListener, PlayStateListener {

    }

    //  </editor-fold>

}
