//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.net.Uri;
import com.mega.cast.utils.log.SmartLog;

import com.samsung.multiscreenfix.Channel.OnMessageListener;

import java.lang.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class VideoPlayer extends Player {
    private static final String TAG = "VideoPlayer";
    private static final String VIDEO_PLAYER_CONTROL_RESPONSE = "state";
    private static final String VIDEO_PLAYER_INTERNAL_RESPONSE = "Video State";
    private static final String VIDEO_PLAYER_QUEUE_EVENT_RESPONSE = "queue";
    private VideoPlayer.OnVideoPlayerListener mVideoPlayerListener = null;

    VideoPlayer(Service service, Uri uri, String appName) {
        super(service, uri, appName);
    }

    public void playContent(Uri contentUrl, Result<Boolean> result) {
        this.playContent(contentUrl, (String)null, (Uri)null, result);
    }

    public void playContent(Uri contentUrl, String title, Uri thumbnailUrl, Result<Boolean> result) {
        JSONObject contentInfo = new JSONObject();

        try {
            if(contentUrl == null || contentUrl.toString().isEmpty()) {
                ErrorCode e = new ErrorCode("PLAYER_ERROR_INVALID_URI");
                if(this.isDebug()) {
                    SmartLog.e("VideoPlayer", "There\'s no media url to launch!");
                }

                if(result != null) {
                    result.onError(Error.create((long)e.value(), e.name(), e.name()));
                }

                return;
            }

            contentInfo.put("uri", contentUrl);
            if(title != null) {
                contentInfo.put(VideoPlayer.VideoPlayerAttributes.title.name(), title);
            }

            if(thumbnailUrl != null) {
                contentInfo.put(VideoPlayer.VideoPlayerAttributes.thumbnailUrl.name(), thumbnailUrl);
            }
        } catch (Exception var8) {
            ErrorCode error = new ErrorCode("PLAYER_ERROR_UNKNOWN");
            if(this.isDebug()) {
                SmartLog.e("VideoPlayer", "Unable to create JSONObject!");
            }

            if(result != null) {
                result.onError(Error.create((long)error.value(), error.name(), error.name()));
            }
        }

        super.playContent(contentInfo, ContentType.video, result);
    }

    public void forward() {
        if(this.isDebug()) {
            SmartLog.d("VideoPlayer", "Send Forward");
        }

        mApplication.publish("playerControl", PlayerControlEvents.FF.name());
    }

    public void rewind() {
        if(this.isDebug()) {
            SmartLog.d("VideoPlayer", "Send Rewind");
        }

        mApplication.publish("playerControl", PlayerControlEvents.RWD.name());
    }

    public void seekTo(int time, TimeUnit timeUnit) {
        long seconds = TimeUnit.SECONDS.convert((long)time, timeUnit);
        if(this.isDebug()) {
            SmartLog.d("VideoPlayer", "Send SeekTo : " + seconds + " seconds");
        }

        mApplication.publish("playerControl", PlayerControlEvents.seekTo.name() + ":" + seconds);
    }

    public void repeat() {
        if(this.isDebug()) {
            SmartLog.d("VideoPlayer", "Send repeat");
        }

        mApplication.publish("playerControl", PlayerControlEvents.repeat.name());
    }

    public void setRepeat(RepeatMode mode) {
        if(this.isDebug()) {
            SmartLog.d("VideoPlayer", "Send setRepeat");
        }

        mApplication.publish("playerControl", PlayerControlEvents.setRepeat.name() + ":" + mode.name());
    }

    public void getList() {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.fetch.name());
            data.put("playerType", ContentType.video.name());
        } catch (Exception var3) {
            SmartLog.w("VideoPlayer", "fetchQueue(): Error in parsing JSON object: " + var3.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void clearList() {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.clear.name());
            data.put("playerType", ContentType.video.name());
        } catch (Exception var3) {
            SmartLog.w("VideoPlayer", "clearQueue(): Error in parsing JSON object: " + var3.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void removeFromList(Uri uri) {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.dequeue.name());
            data.put("playerType", ContentType.video.name());
            data.put("uri", uri.toString());
        } catch (Exception var4) {
            SmartLog.w("VideoPlayer", "deQueue(): Error in parsing JSON object: " + var4.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void addToList(Uri ContentUri) {
        this.addToList(ContentUri, (String)null, (Uri)null);
    }

    public void addToList(Uri uri, String title, Uri thumbUri) {
        HashMap item = new HashMap();
        item.put("uri", uri.toString());
        item.put(VideoPlayer.VideoPlayerAttributes.title.name(), title);
        item.put(VideoPlayer.VideoPlayerAttributes.thumbnailUrl.name(), thumbUri.toString());
        ArrayList list = new ArrayList();
        list.add(item);
        this.addToList((List)list);
    }

    public void addToList(final List<Map<String, String>> videoList) {
        if(videoList != null && !videoList.isEmpty()) {
            if(this.isConnected()) {
                this.getDMPStatus(new Result<DMPStatus>() {
                    public void onSuccess(DMPStatus status) {
                        if(status == null) {
                            SmartLog.d("VideoPlayer", "Error : Something went wrong with Node server!");
                        } else {
                            if(status.mDMPRunning.booleanValue() && status.mRunning.booleanValue()) {
                                for(int it = 0; it < videoList.size(); ++it) {
                                    Uri contentThumbUri = null;
                                    String contentTitle = null;
                                    Map item = (Map)videoList.get(it);
                                    if(!item.containsKey("uri")) {
                                        if(VideoPlayer.this.isDebug()) {
                                            SmartLog.d("VideoPlayer", "enQueue(): ContentUrl can not be Optional.");
                                        }

                                        return;
                                    }

                                    Uri contentUri = Uri.parse((String)item.get("uri"));
                                    if(item.containsKey(VideoPlayer.VideoPlayerAttributes.title.name())) {
                                        contentTitle = (String)item.get(VideoPlayer.VideoPlayerAttributes.title.name());
                                    }

                                    if(item.containsKey(VideoPlayer.VideoPlayerAttributes.thumbnailUrl.name())) {
                                        contentThumbUri = Uri.parse((String)item.get(VideoPlayer.VideoPlayerAttributes.thumbnailUrl.name()));
                                    }

                                    JSONObject data = new JSONObject();

                                    try {
                                        data.put("subEvent", PlayerQueueSubEvents.enqueue.name());
                                        data.put("playerType", ContentType.video.name());
                                        data.put("uri", contentUri.toString());
                                        data.put(VideoPlayer.VideoPlayerAttributes.title.name(), contentTitle);
                                        if(contentThumbUri != null) {
                                            data.put(VideoPlayer.VideoPlayerAttributes.thumbnailUrl.name(), contentThumbUri.toString());
                                        }
                                    } catch (Exception var9) {
                                        SmartLog.w("VideoPlayer", "enQueue(): Error in parsing JSON object: " + var9.toString());
                                    }

                                    Player.mApplication.publish("playerQueueEvent", data);
                                }
                            } else if(VideoPlayer.this.isDebug()) {
                                SmartLog.e("VideoPlayer", "enQueue() Error: DMP Un-Initialized!");
                            }

                        }
                    }

                    public void onError(Error error) {
                        if(VideoPlayer.this.isDebug()) {
                            SmartLog.e("VideoPlayer", "enQueue() Error: " + error.toString());
                        }

                    }
                });
            } else if(this.mVideoPlayerListener != null) {
                ErrorCode errorCode = new ErrorCode("PLAYER_ERROR_PLAYER_NOT_LOADED");
                this.mVideoPlayerListener.onError(Error.create((long)errorCode.value(), errorCode.name(), errorCode.name()));
            }

        } else {
            if(this.isDebug()) {
                SmartLog.d("VideoPlayer", "enQueue(): videoList is NULL.");
            }

        }
    }

    public void addOnMessageListener(VideoPlayer.OnVideoPlayerListener listener) {
        this.mVideoPlayerListener = listener;
        mApplication.addOnMessageListener("playerNotice", new VideoPlayer.OnVideoPlayerMessageListener());
    }

    public interface OnVideoPlayerListener {
        void onBufferingStart();

        void onBufferingComplete();

        void onBufferingProgress(int var1);

        void onCurrentPlayTime(int var1);

        void onStreamingStarted(int var1);

        void onStreamCompleted();

        void onPlayerInitialized();

        void onPlayerChange(String var1);

        void onPlay();

        void onPause();

        void onStop();

        void onForward();

        void onRewind();

        void onMute();

        void onUnMute();

        void onNext();

        void onPrevious();

        void onControlStatus(int var1, Boolean var2, RepeatMode var3);

        void onVolumeChange(int var1);

        void onAddToList(JSONObject var1);

        void onRemoveFromList(JSONObject var1);

        void onClearList();

        void onGetList(JSONArray var1);

        void onRepeat(RepeatMode var1);

        void onCurrentPlaying(JSONObject var1, String var2);

        void onApplicationResume();

        void onApplicationSuspend();

        void onError(Error var1);
    }

    private class OnVideoPlayerMessageListener implements OnMessageListener {
        private OnVideoPlayerMessageListener() {
        }

        public void onMessage(Message message) {
            if(VideoPlayer.this.isDebug()) {
                SmartLog.d("VideoPlayer", "onMessage : " + message);
            }

            if(VideoPlayer.this.mVideoPlayerListener == null) {
                if(VideoPlayer.this.isDebug()) {
                    SmartLog.e("VideoPlayer", "Unregistered PlayerListener.");
                }

            } else if(!message.getEvent().equals("playerNotice")) {
                if(VideoPlayer.this.isDebug()) {
                    SmartLog.w("VideoPlayer", "In-Valid Player Message");
                }

            } else {
                try {
                    JSONObject response = (JSONObject)(new JSONTokener((String)message.getData())).nextValue();
                    if(response == null) {
                        if(VideoPlayer.this.isDebug()) {
                            SmartLog.e("VideoPlayer", "NULL Response - Unable to parse JSON string.");
                        }

                        return;
                    }

                    String e;
                    if(response.has("subEvent")) {
                        e = response.getString("subEvent");
                        if(e.equals("playerReady")) {
                            if(VideoPlayer.this.mAdditionalData != null) {
                                VideoPlayer.this.mAdditionalData.put("playerType", ContentType.video.name());
                                VideoPlayer.this.mAdditionalData.put("subEvent", PlayerContentSubEvents.ADDITIONALMEDIAINFO.name());
                                Player.mApplication.publish("playerContentChange", VideoPlayer.this.mAdditionalData);
                            }

                            VideoPlayer.this.mVideoPlayerListener.onPlayerInitialized();
                        } else if(e.equals("playerChange")) {
                            VideoPlayer.this.mVideoPlayerListener.onPlayerChange(ContentType.video.name());
                        }
                    } else {
                        int volumeStr;
                        if(response.has("playerType")) {
                            e = response.getString("playerType");
                            if(e.equalsIgnoreCase(ContentType.video.name())) {
                                if(response.has("state")) {
                                    this.handlePlayerControlResponse(response.getString("state"));
                                } else if(response.has("Video State")) {
                                    this.handlePlayerInternalResponse(response.getString("Video State"));
                                } else if(response.has("queue")) {
                                    this.handlePlayerQueueEventResponse(response.getString("queue"));
                                } else if(response.has("currentPlaying")) {
                                    VideoPlayer.this.mVideoPlayerListener.onCurrentPlaying(response.getJSONObject("currentPlaying"), e);
                                } else if(response.has("error")) {
                                    String index = response.getString("error");

                                    try {
                                        volumeStr = Integer.parseInt(index);
                                        ErrorCode volumeLevel = new ErrorCode(volumeStr);
                                        VideoPlayer.this.mVideoPlayerListener.onError(Error.create((long)volumeLevel.value(), volumeLevel.name(), volumeLevel.name()));
                                    } catch (NumberFormatException var9) {
                                        VideoPlayer.this.mVideoPlayerListener.onError(Error.create(index));
                                    }
                                }
                            }
                        } else if(response.has("state")) {
                            e = response.toString();
                            if(e.contains(PlayerControlEvents.getControlStatus.name())) {
                                JSONObject index1 = (JSONObject)(new JSONTokener(e)).nextValue();
                                volumeStr = 0;
                                Boolean volumeLevel1 = Boolean.valueOf(false);
                                RepeatMode repeatMode = RepeatMode.repeatOff;
                                if(index1.has(PlayerControlStatus.volume.name())) {
                                    volumeStr = index1.getInt(PlayerControlStatus.volume.name());
                                }

                                if(index1.has(PlayerControlStatus.mute.name())) {
                                    volumeLevel1 = Boolean.valueOf(index1.getBoolean(PlayerControlStatus.mute.name()));
                                }

                                if(index1.has(PlayerControlStatus.repeat.name())) {
                                    String repeatStr = index1.getString(PlayerControlStatus.repeat.name());
                                    repeatMode = null;
                                    if(repeatStr.equals(RepeatMode.repeatAll.name())) {
                                        repeatMode = RepeatMode.repeatAll;
                                    } else if(repeatStr.equals(RepeatMode.repeatSingle.name())) {
                                        repeatMode = RepeatMode.repeatSingle;
                                    } else if(repeatStr.equals(RepeatMode.repeatOff.name())) {
                                        repeatMode = RepeatMode.repeatOff;
                                    }
                                }

                                VideoPlayer.this.mVideoPlayerListener.onControlStatus(volumeStr, volumeLevel1, repeatMode);
                            } else if(e.contains(PlayerControlEvents.mute.name())) {
                                VideoPlayer.this.mVideoPlayerListener.onMute();
                            } else if(e.contains(PlayerControlEvents.unMute.name())) {
                                VideoPlayer.this.mVideoPlayerListener.onUnMute();
                            } else if(e.contains(PlayerControlEvents.getVolume.name())) {
                                int index2 = e.lastIndexOf(":");
                                String volumeStr1 = e.substring(index2 + 1, e.length() - 2).trim();
                                int volumeLevel2 = Integer.parseInt(volumeStr1);
                                VideoPlayer.this.mVideoPlayerListener.onVolumeChange(volumeLevel2);
                            }
                        } else if(response.has("appStatus")) {
                            e = response.toString();
                            if(e.contains(PlayerApplicationStatusEvents.suspend.name())) {
                                VideoPlayer.this.mVideoPlayerListener.onApplicationSuspend();
                            } else if(e.contains(PlayerApplicationStatusEvents.resume.name())) {
                                VideoPlayer.this.mVideoPlayerListener.onApplicationResume();
                            }
                        }
                    }
                } catch (Exception var10) {
                    if(VideoPlayer.this.isDebug()) {
                        SmartLog.e("VideoPlayer", "Error while parsing response : " + var10.getMessage());
                    }
                }

            }
        }

        private void handlePlayerControlResponse(String response) {
            try {
                if(response.contains(PlayerControlEvents.play.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onPlay();
                } else if(response.contains(PlayerControlEvents.pause.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onPause();
                } else if(response.contains(PlayerControlEvents.stop.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onStop();
                } else if(response.contains(PlayerControlEvents.next.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onNext();
                } else if(response.contains(PlayerControlEvents.previous.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onPrevious();
                } else if(response.contains(PlayerControlEvents.FF.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onForward();
                } else if(response.contains(PlayerControlEvents.RWD.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onRewind();
                } else if(response.contains(PlayerControlEvents.repeat.name())) {
                    if(response.contains(RepeatMode.repeatAll.name())) {
                        VideoPlayer.this.mVideoPlayerListener.onRepeat(RepeatMode.repeatAll);
                    } else if(response.contains(RepeatMode.repeatSingle.name())) {
                        VideoPlayer.this.mVideoPlayerListener.onRepeat(RepeatMode.repeatSingle);
                    } else if(response.contains(RepeatMode.repeatOff.name())) {
                        VideoPlayer.this.mVideoPlayerListener.onRepeat(RepeatMode.repeatOff);
                    }
                }
            } catch (Exception var3) {
                if(VideoPlayer.this.isDebug()) {
                    SmartLog.e("VideoPlayer", "Error while parsing control response : " + var3.getMessage());
                }
            }

        }

        private void handlePlayerInternalResponse(String response) {
            try {
                if(response.equalsIgnoreCase(VideoPlayer.VideoPlayerInternalEvents.bufferingstart.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onBufferingStart();
                } else if(response.equalsIgnoreCase(VideoPlayer.VideoPlayerInternalEvents.bufferingcomplete.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onBufferingComplete();
                } else {
                    int progress;
                    int index;
                    if(response.contains(VideoPlayer.VideoPlayerInternalEvents.bufferingprogress.name())) {
                        index = response.indexOf(":");
                        progress = Integer.parseInt(response.substring(index + 1).trim());
                        VideoPlayer.this.mVideoPlayerListener.onBufferingProgress(progress);
                    } else if(response.contains(VideoPlayer.VideoPlayerInternalEvents.currentplaytime.name())) {
                        index = response.indexOf(":");
                        progress = Integer.parseInt(response.substring(index + 1).trim());
                        VideoPlayer.this.mVideoPlayerListener.onCurrentPlayTime(progress);
                    } else if(response.contains(VideoPlayer.VideoPlayerInternalEvents.streamcompleted.name())) {
                        VideoPlayer.this.mVideoPlayerListener.onStreamCompleted();
                    } else if(response.contains(VideoPlayer.VideoPlayerInternalEvents.totalduration.name())) {
                        index = response.indexOf(":");
                        progress = Integer.parseInt(response.substring(index + 1).trim());
                        VideoPlayer.this.mVideoPlayerListener.onStreamingStarted(progress);
                    }
                }
            } catch (Exception var5) {
                if(VideoPlayer.this.isDebug()) {
                    SmartLog.e("VideoPlayer", "Error while parsing Internal Event response : " + var5.getMessage());
                }
            }

        }

        private void handlePlayerQueueEventResponse(String response) {
            try {
                JSONObject e = new JSONObject(response);
                String event = e.getString("subEvent");
                e.remove("subEvent");
                if(event == null) {
                    if(VideoPlayer.this.isDebug()) {
                        SmartLog.e("VideoPlayer", "Sub-Event key missing from message.");
                    }

                    return;
                }

                if(event.equals(PlayerQueueSubEvents.enqueue.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onAddToList(e);
                } else if(event.equals(PlayerQueueSubEvents.dequeue.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onRemoveFromList(e);
                } else if(event.equals(PlayerQueueSubEvents.clear.name())) {
                    VideoPlayer.this.mVideoPlayerListener.onClearList();
                } else if(event.equals(PlayerQueueSubEvents.fetch.name()) && e.has("data")) {
                    VideoPlayer.this.mVideoPlayerListener.onGetList(e.getJSONArray("data"));
                }
            } catch (Exception var4) {
                if(VideoPlayer.this.isDebug()) {
                    SmartLog.e("VideoPlayer", "Error while parsing list Event response : " + var4.getMessage());
                }
            }

        }
    }

    private static enum VideoPlayerInternalEvents {
        streamcompleted,
        currentplaytime,
        totalduration,
        bufferingstart,
        bufferingprogress,
        bufferingcomplete;

        private VideoPlayerInternalEvents() {
        }
    }

    private static enum VideoPlayerAttributes {
        title,
        thumbnailUrl;

        private VideoPlayerAttributes() {
        }
    }
}
