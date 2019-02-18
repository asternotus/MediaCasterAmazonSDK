//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build;
import android.os.Build.VERSION;
import com.mega.cast.utils.log.SmartLog;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.samsung.multiscreenfix.Channel.OnClientConnectListener;
import com.samsung.multiscreenfix.Channel.OnClientDisconnectListener;
import com.samsung.multiscreenfix.Channel.OnConnectListener;
import com.samsung.multiscreenfix.Channel.OnDisconnectListener;
import com.samsung.multiscreenfix.Channel.OnErrorListener;
import com.samsung.multiscreenfix.Channel.OnReadyListener;
import com.samsung.multiscreenfix.util.HttpUtil;
import com.samsung.multiscreenfix.util.HttpUtil.ResultCreator;

import java.lang.*;
import java.util.Map;
import org.json.JSONObject;

public class Player {
    private static final String DEFAULT_MEDIA_PLAYER = "samsung.default.media.player";
    private static final String APP_ID = "3201412000694";
    private static final String TAG = "Player";
    private static final String PROPERTY_RUNNING = "running";
    private static final String PROPERTY_ISCONTENTS = "isContents";
    private static final String PROPERTY_DMP_RUNNING = "media_player";
    private static final String PROPERTY_APP_VISIBLE = "visible";
    static final String CONTENT_URI = "uri";
    static final String PLAYER_NOTICE_RESPONSE_EVENT = "playerNotice";
    static final String PLAYER_TYPE = "playerType";
    static final String PLAYER_DATA = "data";
    static final String PLAYER_SUB_EVENT = "subEvent";
    static final String PLAYER_READY_SUB_EVENT = "playerReady";
    static final String PLAYER_CHANGE_SUB_EVENT = "playerChange";
    static final String PLAYER_CONTROL_EVENT = "playerControl";
    static final String PLAYER_CONTENT_CHANGE_EVENT = "playerContentChange";
    static final String PLAYER_QUEUE_EVENT = "playerQueueEvent";
    static final String PLAYER_CURRENT_PLAYING_EVENT = "currentPlaying";
    static final String PLAYER_ERROR_MESSAGE_EVENT = "error";
    static final String PLAYER_APP_STATUS_EVENT = "appStatus";
    private static final String PLAYER_BGIMAGE1 = "url1";
    private static final String PLAYER_BGIMAGE2 = "url2";
    private static final String PLAYER_BGIMAGE3 = "url2";
    static Application mApplication = null;
    JSONObject mAdditionalData = null;
    private Player.ContentType mContentType = null;
    private String mAppName;
    private boolean debug = false;

    Player(Service service, Uri uri, String appName) {
        this.mAppName = appName;
        mApplication = service.createApplication(uri, "samsung.default.media.player");
        if(this.isDebug()) {
            SmartLog.d("Player", "Player Created");
        }

    }

    private void connect() {
        this.connect((Result)null);
    }

    private void connect(Result<Client> result) {
        this.connect((Map)null, result);
    }

    private void connect(Map<String, String> attributes, Result<Client> callback) {
        mApplication.connectToPlay(attributes, callback);
    }

    public void disconnect() {
        this.disconnect(true, (Result)null);
    }

    public void disconnect(Result<Client> result) {
        this.disconnect(true, result);
    }

    public void disconnect(boolean stopOnDisconnect, Result<Client> callback) {
        mApplication.disconnect(stopOnDisconnect, callback);
    }

    public boolean isConnected() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Player Connection Status : " + mApplication.isConnected());
        }

        return mApplication.isConnected();
    }

    public void setOnConnectListener(OnConnectListener onConnectListener) {
        mApplication.setOnConnectListener(onConnectListener);
    }

    public void setOnDisconnectListener(OnDisconnectListener onDisconnectListener) {
        mApplication.setOnDisconnectListener(onDisconnectListener);
    }

    public void setOnClientConnectListener(OnClientConnectListener onClientConnectListener) {
        mApplication.setOnClientConnectListener(onClientConnectListener);
    }

    public void setOnClientDisconnectListener(OnClientDisconnectListener onClientDisconnectListener) {
        mApplication.setOnClientDisconnectListener(onClientDisconnectListener);
    }

    public void setOnReadyListener(OnReadyListener OnReadyListener) {
        mApplication.setOnReadyListener(OnReadyListener);
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mApplication.setOnErrorListener(onErrorListener);
    }

    final void getDMPStatus(Result<Player.DMPStatus> result) {
        Builder builder = mApplication.getService().getUri().buildUpon();
        builder.appendPath("webapplication");
        builder.appendPath("");
        StringCallback httpStringCallback = HttpHelper.createHttpCallback(new ResultCreator<Player.DMPStatus>() {
            public Player.DMPStatus createResult(Map<String, Object> data) {
                Player.DMPStatus status = Player.this.new DMPStatus();
                if(data != null) {
                    String id = (String)data.get("id");
                    if(data.containsKey("appName")) {
                        status.mAppName = (String)data.get("appName");
                    }

                    if(data.containsKey("visible")) {
                        status.mVisible = (Boolean)data.get("visible");
                    }

                    if(data.containsKey("media_player")) {
                        status.mDMPRunning = (Boolean)data.get("media_player");
                    }

                    if(data.containsKey("running")) {
                        status.mRunning = (Boolean)data.get("running");
                    }

                    if(id != null && id.contains("3201412000694")) {
                        return status;
                    }
                }

                return null;
            }
        }, result);
        HttpUtil.executeJSONRequest(builder.build(), "GET", httpStringCallback);
    }

    final void playContent(final JSONObject contentInfo, Player.ContentType playerName, final Result<Boolean> result) {
        if(this.isDebug()) {
            SmartLog.d("Player", "Is Connected Status : " + this.isConnected());
        }

        this.mAdditionalData = contentInfo;
        this.mContentType = playerName;
        if(!this.isConnected()) {
            this.connect(new Result<Client>() {
                public void onSuccess(Client client) {
                    Player.this.startPlay(contentInfo, result);
                }

                public void onError(Error error) {
                    if(result != null) {
                        result.onError(error);
                    }

                }
            });
        } else {
            this.startPlay(contentInfo, result);
        }

    }

    private void sendStartDMPApplicationRequest(String contentUrl, final Result<Boolean> callback) {
        Map params = mApplication.getParams();
        Map startArgs = mApplication.getStartArgs();
        if(startArgs != null) {
            params.put("args", startArgs);
        }

        String contentType = this.mContentType.name();
        if(contentType.equalsIgnoreCase(Player.ContentType.photo.name())) {
            contentType = "picture";
        }

        params.put("isContents", contentType);
        params.put("url", contentUrl);
        params.put("os", VERSION.RELEASE);
        params.put("library", "Android SDK");
        params.put("version", "2.4.1");
        params.put("appName", this.mAppName);
        params.put("modelNumber", Build.MODEL);
        if(this.isDebug()) {
            SmartLog.d("Player", "Send ms.webapplication.start with params " + params);
        }

        mApplication.invokeMethod("ms.webapplication.start", params, new Result<Boolean>() {
            public void onSuccess(Boolean success) {
                if(Player.this.isDebug()) {
                    SmartLog.d("Player", "DMP Launched Successfully");
                }

                if(callback != null) {
                    callback.onSuccess(Boolean.valueOf(true));
                }

            }

            public void onError(Error error) {
                Player.mApplication.closeConnection();
                if(Player.this.isDebug()) {
                    SmartLog.e("Player", "DMP Launch Failed with error message : " + error.toString());
                }

                if(callback != null) {
                    callback.onError(error);
                }

            }
        });
    }

    private void startPlay(final JSONObject data, final Result<Boolean> result) {
        String url = null;
        ErrorCode contentUrl;
        if(null == data) {
            contentUrl = new ErrorCode("PLAYER_ERROR_UNKNOWN");
            if(result != null) {
                result.onError(Error.create((long)contentUrl.value(), contentUrl.name(), contentUrl.name()));
            }

            if(this.isDebug()) {
                SmartLog.e("Player", "startPlay() Error: \'data\' is NULL.");
            }

        } else {
            if(data.has("uri")) {
                try {
                    url = data.getString("uri");
                } catch (Exception var5) {
                    if(this.isDebug()) {
                        SmartLog.e("Player", "startPlay() : Error in parsing JSON data: " + var5.getMessage());
                    }

                    return;
                }
            }

            if(url == null) {
                contentUrl = new ErrorCode("PLAYER_ERROR_UNKNOWN");
                if(result != null) {
                    result.onError(Error.create((long)contentUrl.value(), contentUrl.name(), contentUrl.name()));
                }

                if(this.isDebug()) {
                    SmartLog.e("Player", "startPlay() Error: \'url\' is NULL.");
                }

            } else {
                if(this.isDebug()) {
                    SmartLog.d("Player", "Content Url : " + url);
                }

                final String finalUrl = url;
                this.getDMPStatus(new Result<Player.DMPStatus>() {
                    public void onSuccess(Player.DMPStatus status) {
                        if(status == null) {
                            ErrorCode e = new ErrorCode("PLAYER_ERROR_INVALID_TV_RESPONSE");
                            if(Player.this.isDebug()) {
                                SmartLog.e("Player", "getDMPStatus() : Error: " + e.name());
                            }

                            if(result != null) {
                                result.onError(Error.create((long)e.value(), e.name(), e.name()));
                            }

                        } else {
                            if(Player.this.isDebug()) {
                                SmartLog.d("Player", "DMP AppName : " + status.mAppName);
                                SmartLog.d("Player", "DMP Visible : " + status.mVisible);
                                SmartLog.d("Player", "DMP Running : " + status.mDMPRunning);
                            }

                            if(status.mDMPRunning.booleanValue() && status.mRunning.booleanValue()) {
                                if(status.mAppName != null && status.mAppName.equals(Player.this.mAppName)) {
                                    if(status.mVisible.booleanValue()) {
                                        try {
                                            data.put("subEvent", Player.PlayerContentSubEvents.CHANGEPLAYINGCONTENT.name());
                                            data.put("playerType", Player.this.mContentType.name());
                                        } catch (Exception var3) {
                                            if(Player.this.isDebug()) {
                                                SmartLog.e("Player", "Error while creating ChangePlayingContent Request : " + var3.getMessage());
                                            }

                                            return;
                                        }

                                        Player.mApplication.publish("playerContentChange", data);
                                        if(result != null) {
                                            result.onSuccess(Boolean.valueOf(true));
                                        }
                                    } else {
                                        Player.this.sendStartDMPApplicationRequest(finalUrl, new Result<Boolean>() {
                                            public void onSuccess(Boolean success) {
                                                if(Player.this.isDebug()) {
                                                    SmartLog.d("Player", "DMP Launched Successfully, Sending ChangePlayingContent Request..");
                                                }

                                                try {
                                                    data.put("subEvent", Player.PlayerContentSubEvents.CHANGEPLAYINGCONTENT.name());
                                                    data.put("playerType", Player.this.mContentType.name());
                                                } catch (Exception var3) {
                                                    if(Player.this.isDebug()) {
                                                        SmartLog.e("Player", "Error while creating ChangePlayingContent Request : " + var3.getMessage());
                                                    }

                                                    return;
                                                }

                                                Player.mApplication.publish("playerContentChange", data);
                                                if(result != null) {
                                                    result.onSuccess(Boolean.valueOf(true));
                                                }

                                            }

                                            public void onError(Error error) {
                                                Player.mApplication.closeConnection();
                                                if(Player.this.isDebug()) {
                                                    SmartLog.e("Player", "DMP Launch Failed with error message : " + error.toString());
                                                }

                                                if(result != null) {
                                                    result.onError(error);
                                                }

                                            }
                                        });
                                    }
                                } else {
                                    Player.this.sendStartDMPApplicationRequest(finalUrl, result);
                                }
                            } else {
                                Player.this.sendStartDMPApplicationRequest(finalUrl, result);
                            }

                        }
                    }

                    public void onError(Error error) {
                        if(Player.this.isDebug()) {
                            SmartLog.e("Player", "StartPlay() Error: " + error.toString());
                        }

                        result.onError(error);
                    }
                });
            }
        }
    }

    public final void resumeApplicationInForeground() {
        String dummyUrl = "http://DummyUrlToBringAppToForeground.msf";
        this.sendStartDMPApplicationRequest(dummyUrl, new Result<Boolean>() {
            public void onSuccess(Boolean status) {
                if(Player.this.isDebug()) {
                    SmartLog.d("Player", "resumeApplicationInForeground() onSuccess(): Successfully resumed application in foreground.");
                }

            }

            public void onError(Error error) {
                if(Player.this.isDebug()) {
                    SmartLog.e("Player", "resumeApplicationInForeground() onError(): " + error.toString());
                }

            }
        });
    }

    public void play() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send Play");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.play.name());
    }

    public void pause() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send Pause");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.pause.name());
    }

    public void stop() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send Stop");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.stop.name());
    }

    public void mute() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send Mute");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.mute.name());
    }

    public void unMute() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send Un-Mute");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.unMute.name());
    }

    public void setVolume(int volume) {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send SetVolume : " + volume);
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.setVolume.name() + ":" + volume);
    }

    public void getControlStatus() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send getControlStatus");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.getControlStatus.name());
    }

    public void volumeUp() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send VolumeUp");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.volumeUp.name());
    }

    public void volumeDown() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send VolumeDown");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.volumeDown.name());
    }

    public void previous() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send Previous");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.previous.name());
    }

    public void next() {
        if(this.isDebug()) {
            SmartLog.d("Player", "Send Next");
        }

        mApplication.publish("playerControl", Player.PlayerControlEvents.next.name());
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return this.debug;
    }

    class DMPStatus {
        Boolean mVisible;
        Boolean mDMPRunning;
        Boolean mRunning;
        String mAppName;

        DMPStatus() {
            this.mVisible = Boolean.valueOf(false);
            this.mDMPRunning = Boolean.valueOf(false);
            this.mRunning = Boolean.valueOf(false);
            this.mAppName = null;
        }

        DMPStatus(Boolean visible, Boolean dmpRunning, Boolean running, String appName) {
            this.mVisible = visible;
            this.mRunning = running;
            this.mDMPRunning = dmpRunning;
            this.mAppName = appName;
        }
    }

    static enum PlayerControlStatus {
        volume,
        mute,
        repeat,
        shuffle;

        private PlayerControlStatus() {
        }
    }

    public static enum RepeatMode {
        repeatOff,
        repeatSingle,
        repeatAll;

        private RepeatMode() {
        }
    }

    static enum PlayerApplicationStatusEvents {
        suspend,
        resume;

        private PlayerApplicationStatusEvents() {
        }
    }

    static enum PlayerContentSubEvents {
        ADDITIONALMEDIAINFO,
        CHANGEPLAYINGCONTENT;

        private PlayerContentSubEvents() {
        }
    }

    static enum PlayerQueueSubEvents {
        enqueue,
        dequeue,
        clear,
        fetch;

        private PlayerQueueSubEvents() {
        }
    }

    static enum PlayerControlEvents {
        play,
        pause,
        stop,
        mute,
        unMute,
        setVolume,
        getControlStatus,
        getVolume,
        volumeUp,
        volumeDown,
        previous,
        next,
        FF,
        RWD,
        seekTo,
        repeat,
        setRepeat,
        shuffle,
        setShuffle,
        playMusic,
        stopMusic;

        private PlayerControlEvents() {
        }
    }

    static enum ContentType {
        audio,
        video,
        photo;

        private ContentType() {
        }
    }
}
