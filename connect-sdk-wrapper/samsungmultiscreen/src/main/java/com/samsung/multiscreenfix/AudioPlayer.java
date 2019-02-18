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

public class AudioPlayer extends Player {
    private static final String TAG = "AudioPlayer";
    private static final String AUDIO_PLAYER_CONTROL_RESPONSE = "state";
    private static final String AUDIO_PLAYER_INTERNAL_RESPONSE = "Audio State";
    private static final String AUDIO_PLAYER_QUEUE_EVENT_RESPONSE = "queue";
    private AudioPlayer.OnAudioPlayerListener mAudioPlayerListener = null;
    private List<Map<String, String>> mList = null;

    AudioPlayer(Service service, Uri uri, String appName) {
        super(service, uri, appName);
    }

    public void playContent(Uri contentUrl, Result<Boolean> result) {
        this.playContent(contentUrl, (String)null, (String)null, (Uri)null, result);
    }

    public void playContent(Uri contentUrl, String title, String albumName, Uri albumArtUrl, Result<Boolean> result) {
        JSONObject contentInfo = new JSONObject();

        try {
            if(contentUrl == null || contentUrl.toString().isEmpty()) {
                ErrorCode e = new ErrorCode("PLAYER_ERROR_INVALID_URI");
                if(this.isDebug()) {
                    SmartLog.e("AudioPlayer", "There\'s no media url to launch!");
                }

                if(result != null) {
                    result.onError(Error.create((long)e.value(), e.name(), e.name()));
                }

                return;
            }

            contentInfo.put("uri", contentUrl);
            if(title != null) {
                contentInfo.put(AudioPlayer.AudioPlayerAttributes.title.name(), title);
            }

            if(albumName != null) {
                contentInfo.put(AudioPlayer.AudioPlayerAttributes.albumName.name(), albumName);
            }

            if(albumArtUrl != null) {
                contentInfo.put(AudioPlayer.AudioPlayerAttributes.albumArt.name(), albumArtUrl);
            }
        } catch (Exception var9) {
            ErrorCode error = new ErrorCode("PLAYER_ERROR_UNKNOWN");
            if(this.isDebug()) {
                SmartLog.e("AudioPlayer", "Unable to create JSONObject!");
            }

            if(result != null) {
                result.onError(Error.create((long)error.value(), error.name(), error.name()));
            }
        }

        super.playContent(contentInfo, ContentType.audio, result);
    }

    public void seekTo(int time, TimeUnit timeUnit) {
        long seconds = TimeUnit.SECONDS.convert((long)time, timeUnit);
        if(this.isDebug()) {
            SmartLog.d("AudioPlayer", "Send SeekTo : " + seconds + " seconds");
        }

        mApplication.publish("playerControl", PlayerControlEvents.seekTo.name() + ":" + seconds);
    }

    public void repeat() {
        if(this.isDebug()) {
            SmartLog.d("AudioPlayer", "Send repeat");
        }

        mApplication.publish("playerControl", PlayerControlEvents.repeat.name());
    }

    public void setRepeat(RepeatMode mode) {
        if(this.isDebug()) {
            SmartLog.d("AudioPlayer", "Send setRepeat");
        }

        mApplication.publish("playerControl", PlayerControlEvents.setRepeat.name() + ":" + mode.name());
    }

    public void shuffle() {
        if(this.isDebug()) {
            SmartLog.d("AudioPlayer", "Send shuffle");
        }

        mApplication.publish("playerControl", PlayerControlEvents.shuffle.name());
    }

    public void setShuffle(boolean mode) {
        if(this.isDebug()) {
            SmartLog.d("AudioPlayer", "Send setShuffle");
        }

        mApplication.publish("playerControl", PlayerControlEvents.setShuffle.name() + ":" + mode);
    }

    public void getList() {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.fetch.name());
            data.put("playerType", ContentType.audio.name());
        } catch (Exception var3) {
            SmartLog.w("AudioPlayer", "fetchQueue(): Error in parsing JSON object: " + var3.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void clearList() {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.clear.name());
            data.put("playerType", ContentType.audio.name());
        } catch (Exception var3) {
            SmartLog.w("AudioPlayer", "clearQueue(): Error in parsing JSON object: " + var3.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void removeFromList(Uri uri) {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.dequeue.name());
            data.put("playerType", ContentType.audio.name());
            data.put("uri", uri.toString());
        } catch (Exception var4) {
            SmartLog.w("AudioPlayer", "deQueue(): Error in parsing JSON object: " + var4.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void addToList(Uri ContentUrl) {
        this.addToList(ContentUrl, (String)null, (String)null, (Uri)null);
    }

    public void addToList(Uri ContentUrl, String title, String albumName, Uri albumArt) {
        HashMap item = new HashMap();
        item.put("uri", ContentUrl.toString());
        item.put(AudioPlayer.AudioPlayerAttributes.title.name(), title);
        item.put(AudioPlayer.AudioPlayerAttributes.albumName.name(), albumName);
        item.put(AudioPlayer.AudioPlayerAttributes.albumArt.name(), albumArt.toString());
        ArrayList list = new ArrayList();
        list.add(item);
        this.addToList((List)list);
    }

    public void addToList(final List<Map<String, String>> audioList) {
        if(audioList != null && !audioList.isEmpty()) {
            if(this.isConnected()) {
                this.getDMPStatus(new Result<DMPStatus>() {
                    public void onSuccess(DMPStatus status) {
                        if(status == null) {
                            SmartLog.d("AudioPlayer", "Error : Something went wrong with Node server!");
                        } else {
                            if(status.mDMPRunning.booleanValue() && status.mRunning.booleanValue()) {
                                for(int it = 0; it < audioList.size(); ++it) {
                                    Uri albumArt = null;
                                    String albumName = null;
                                    String contentTitle = null;
                                    String endTime = null;
                                    Map item = (Map)audioList.get(it);
                                    if(!item.containsKey("uri")) {
                                        if(AudioPlayer.this.isDebug()) {
                                            SmartLog.d("AudioPlayer", "enQueue(): ContentUrl can not be Optional.");
                                        }

                                        return;
                                    }

                                    Uri contentUri = Uri.parse((String)item.get("uri"));
                                    if(item.containsKey(AudioPlayer.AudioPlayerAttributes.title.name())) {
                                        contentTitle = (String)item.get(AudioPlayer.AudioPlayerAttributes.title.name());
                                    }

                                    if(item.containsKey(AudioPlayer.AudioPlayerAttributes.albumName.name())) {
                                        albumName = (String)item.get(AudioPlayer.AudioPlayerAttributes.albumName.name());
                                    }

                                    if(item.containsKey(AudioPlayer.AudioPlayerAttributes.albumArt.name())) {
                                        albumArt = Uri.parse((String)item.get(AudioPlayer.AudioPlayerAttributes.albumArt.name()));
                                    }

                                    if(item.containsKey(AudioPlayer.AudioPlayerAttributes.endTime.name())) {
                                        endTime = (String)item.get(item.get(AudioPlayer.AudioPlayerAttributes.endTime.name()));
                                    }

                                    JSONObject data = new JSONObject();

                                    try {
                                        data.put("subEvent", PlayerQueueSubEvents.enqueue.name());
                                        data.put("playerType", ContentType.audio.name());
                                        data.put("uri", contentUri.toString());
                                        data.put(AudioPlayer.AudioPlayerAttributes.title.name(), contentTitle);
                                        data.put(AudioPlayer.AudioPlayerAttributes.albumName.name(), albumName);
                                        data.put(AudioPlayer.AudioPlayerAttributes.endTime.name(), endTime);
                                        if(albumArt != null) {
                                            data.put(AudioPlayer.AudioPlayerAttributes.albumArt.name(), albumArt.toString());
                                        }
                                    } catch (Exception var11) {
                                        SmartLog.w("AudioPlayer", "enQueue(): Error in parsing JSON object: " + var11.toString());
                                    }

                                    mApplication.publish("playerQueueEvent", data);
                                }
                            } else if(AudioPlayer.this.isDebug()) {
                                SmartLog.e("AudioPlayer", "enQueue() Error: DMP Un-Initialized!");
                            }

                        }
                    }

                    public void onError(Error error) {
                        if(AudioPlayer.this.isDebug()) {
                            SmartLog.e("AudioPlayer", "enQueue() Error: " + error.toString());
                        }

                    }
                });
            } else if(this.mAudioPlayerListener != null) {
                ErrorCode errorCode = new ErrorCode("PLAYER_ERROR_PLAYER_NOT_LOADED");
                this.mAudioPlayerListener.onError(Error.create((long)errorCode.value(), errorCode.name(), errorCode.name()));
            }

        } else {
            if(this.isDebug()) {
                SmartLog.d("AudioPlayer", "enQueue(): audioList is NULL.");
            }

        }
    }

    public void addOnMessageListener(AudioPlayer.OnAudioPlayerListener listener) {
        this.mAudioPlayerListener = listener;
        mApplication.addOnMessageListener("playerNotice", new AudioPlayer.OnAudioPlayerMessageListener());
    }

    public interface OnAudioPlayerListener {
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

        void onMute();

        void onUnMute();

        void onNext();

        void onPrevious();

        void onControlStatus(int var1, Boolean var2, Boolean var3, RepeatMode var4);

        void onVolumeChange(int var1);

        void onAddToList(JSONObject var1);

        void onRemoveFromList(JSONObject var1);

        void onClearList();

        void onGetList(JSONArray var1);

        void onShuffle(Boolean var1);

        void onRepeat(RepeatMode var1);

        void onCurrentPlaying(JSONObject var1, String var2);

        void onApplicationResume();

        void onApplicationSuspend();

        void onError(Error var1);
    }

    private class OnAudioPlayerMessageListener implements OnMessageListener {
        private OnAudioPlayerMessageListener() {
        }

        public void onMessage(Message message) {
            if(AudioPlayer.this.isDebug()) {
                SmartLog.d("AudioPlayer", "onMessage : " + message);
            }

            if(AudioPlayer.this.mAudioPlayerListener == null) {
                if(AudioPlayer.this.isDebug()) {
                    SmartLog.e("AudioPlayer", "Unregistered PlayerListener.");
                }

            } else if(!message.getEvent().equals("playerNotice")) {
                if(AudioPlayer.this.isDebug()) {
                    SmartLog.w("AudioPlayer", "In-Valid Player Message");
                }

            } else {
                try {
                    JSONObject response = (JSONObject)(new JSONTokener((String)message.getData())).nextValue();
                    if(response == null) {
                        if(AudioPlayer.this.isDebug()) {
                            SmartLog.e("AudioPlayer", "NULL Response - Unable to parse JSON string.");
                        }

                        return;
                    }

                    String e;
                    if(response.has("subEvent")) {
                        e = response.getString("subEvent");
                        if(e.equals("playerReady")) {
                            if(AudioPlayer.this.mAdditionalData != null) {
                                AudioPlayer.this.mAdditionalData.put("playerType", ContentType.audio.name());
                                AudioPlayer.this.mAdditionalData.put("subEvent", PlayerContentSubEvents.ADDITIONALMEDIAINFO.name());
                                mApplication.publish("playerContentChange", AudioPlayer.this.mAdditionalData);
                            }

                            AudioPlayer.this.mAudioPlayerListener.onPlayerInitialized();
                        } else if(e.equals("playerChange")) {
                            AudioPlayer.this.mAudioPlayerListener.onPlayerChange(ContentType.audio.name());
                        }
                    } else {
                        int volumeStr;
                        if(response.has("playerType")) {
                            e = response.getString("playerType");
                            if(e.equalsIgnoreCase(ContentType.audio.name())) {
                                if(response.has("state")) {
                                    this.handlePlayerControlResponse(response.getString("state"));
                                } else if(response.has("Audio State")) {
                                    this.handlePlayerInternalResponse(response.getString("Audio State"));
                                } else if(response.has("queue")) {
                                    this.handlePlayerQueueEventResponse(response.getString("queue"));
                                } else if(response.has("currentPlaying")) {
                                    AudioPlayer.this.mAudioPlayerListener.onCurrentPlaying(response.getJSONObject("currentPlaying"), e);
                                } else if(response.has("error")) {
                                    String index = response.getString("error");

                                    try {
                                        volumeStr = Integer.parseInt(index);
                                        ErrorCode volumeLevel = new ErrorCode(volumeStr);
                                        AudioPlayer.this.mAudioPlayerListener.onError(Error.create((long)volumeLevel.value(), volumeLevel.name(), volumeLevel.name()));
                                    } catch (NumberFormatException var10) {
                                        AudioPlayer.this.mAudioPlayerListener.onError(Error.create(index));
                                    }
                                }
                            }
                        } else if(response.has("state")) {
                            e = response.toString();
                            if(e.contains(PlayerControlEvents.getControlStatus.name())) {
                                JSONObject index1 = (JSONObject)(new JSONTokener(e)).nextValue();
                                volumeStr = 0;
                                Boolean volumeLevel1 = Boolean.valueOf(false);
                                Boolean shuffleStatus = Boolean.valueOf(false);
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

                                if(index1.has(PlayerControlStatus.shuffle.name())) {
                                    shuffleStatus = Boolean.valueOf(index1.getBoolean(PlayerControlStatus.shuffle.name()));
                                }

                                AudioPlayer.this.mAudioPlayerListener.onControlStatus(volumeStr, volumeLevel1, shuffleStatus, repeatMode);
                            } else if(e.contains(PlayerControlEvents.mute.name())) {
                                AudioPlayer.this.mAudioPlayerListener.onMute();
                            } else if(e.contains(PlayerControlEvents.unMute.name())) {
                                AudioPlayer.this.mAudioPlayerListener.onUnMute();
                            } else if(e.contains(PlayerControlEvents.getVolume.name())) {
                                int index2 = e.lastIndexOf(":");
                                String volumeStr1 = e.substring(index2 + 1, e.length() - 2).trim();
                                int volumeLevel2 = Integer.parseInt(volumeStr1);
                                AudioPlayer.this.mAudioPlayerListener.onVolumeChange(volumeLevel2);
                            }
                        } else if(response.has("appStatus")) {
                            e = response.toString();
                            if(e.contains(PlayerApplicationStatusEvents.suspend.name())) {
                                AudioPlayer.this.mAudioPlayerListener.onApplicationSuspend();
                            } else if(e.contains(PlayerApplicationStatusEvents.resume.name())) {
                                AudioPlayer.this.mAudioPlayerListener.onApplicationResume();
                            }
                        }
                    }
                } catch (Exception var11) {
                    if(AudioPlayer.this.isDebug()) {
                        SmartLog.e("AudioPlayer", "Error while parsing response : " + var11.getMessage());
                    }
                }

            }
        }

        private void handlePlayerControlResponse(String response) {
            try {
                if(response.contains(PlayerControlEvents.play.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onPlay();
                } else if(response.contains(PlayerControlEvents.pause.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onPause();
                } else if(response.contains(PlayerControlEvents.stop.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onStop();
                } else if(response.contains(PlayerControlEvents.next.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onNext();
                } else if(response.contains(PlayerControlEvents.previous.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onPrevious();
                } else if(response.contains(PlayerControlEvents.repeat.name())) {
                    if(response.contains(RepeatMode.repeatAll.name())) {
                        AudioPlayer.this.mAudioPlayerListener.onRepeat(RepeatMode.repeatAll);
                    } else if(response.contains(RepeatMode.repeatSingle.name())) {
                        AudioPlayer.this.mAudioPlayerListener.onRepeat(RepeatMode.repeatSingle);
                    } else if(response.contains(RepeatMode.repeatOff.name())) {
                        AudioPlayer.this.mAudioPlayerListener.onRepeat(RepeatMode.repeatOff);
                    }
                } else if(response.contains(PlayerControlEvents.shuffle.name())) {
                    Boolean e = Boolean.valueOf(response.contains("true"));
                    AudioPlayer.this.mAudioPlayerListener.onShuffle(e);
                }
            } catch (Exception var3) {
                if(AudioPlayer.this.isDebug()) {
                    SmartLog.e("AudioPlayer", "Error while parsing control response : " + var3.getMessage());
                }
            }

        }

        private void handlePlayerInternalResponse(String response) {
            try {
                if(response.equalsIgnoreCase(AudioPlayer.AudioPlayerInternalEvents.bufferingstart.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onBufferingStart();
                } else if(response.equalsIgnoreCase(AudioPlayer.AudioPlayerInternalEvents.bufferingcomplete.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onBufferingComplete();
                } else {
                    int progress;
                    int index;
                    if(response.contains(AudioPlayer.AudioPlayerInternalEvents.bufferingprogress.name())) {
                        index = response.indexOf(":");
                        progress = Integer.parseInt(response.substring(index + 1).trim());
                        AudioPlayer.this.mAudioPlayerListener.onBufferingProgress(progress);
                    } else if(response.contains(AudioPlayer.AudioPlayerInternalEvents.currentplaytime.name())) {
                        index = response.indexOf(":");
                        progress = Integer.parseInt(response.substring(index + 1).trim());
                        AudioPlayer.this.mAudioPlayerListener.onCurrentPlayTime(progress);
                    } else if(response.contains(AudioPlayer.AudioPlayerInternalEvents.streamcompleted.name())) {
                        AudioPlayer.this.mAudioPlayerListener.onStreamCompleted();
                    } else if(response.contains(AudioPlayer.AudioPlayerInternalEvents.totalduration.name())) {
                        index = response.indexOf(":");
                        progress = Integer.parseInt(response.substring(index + 1).trim());
                        AudioPlayer.this.mAudioPlayerListener.onStreamingStarted(progress);
                    }
                }
            } catch (Exception var5) {
                if(AudioPlayer.this.isDebug()) {
                    SmartLog.e("AudioPlayer", "Error while parsing Internal Event response : " + var5.getMessage());
                }
            }

        }

        private void handlePlayerQueueEventResponse(String response) {
            try {
                JSONObject e = new JSONObject(response);
                String event = e.getString("subEvent");
                e.remove("subEvent");
                if(event == null) {
                    if(AudioPlayer.this.isDebug()) {
                        SmartLog.e("AudioPlayer", "Sub-Event key missing from message.");
                    }

                    return;
                }

                if(event.equals(PlayerQueueSubEvents.enqueue.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onAddToList(e);
                } else if(event.equals(PlayerQueueSubEvents.dequeue.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onRemoveFromList(e);
                } else if(event.equals(PlayerQueueSubEvents.clear.name())) {
                    AudioPlayer.this.mAudioPlayerListener.onClearList();
                } else if(event.equals(PlayerQueueSubEvents.fetch.name()) && e.has("data")) {
                    AudioPlayer.this.mAudioPlayerListener.onGetList(e.getJSONArray("data"));
                }
            } catch (Exception var4) {
                if(AudioPlayer.this.isDebug()) {
                    SmartLog.e("AudioPlayer", "Error while parsing list Event response : " + var4.getMessage());
                }
            }

        }
    }

    private static enum AudioPlayerInternalEvents {
        streamcompleted,
        currentplaytime,
        totalduration,
        bufferingstart,
        bufferingprogress,
        bufferingcomplete;

        private AudioPlayerInternalEvents() {
        }
    }

    private static enum AudioPlayerAttributes {
        title,
        albumName,
        albumArt,
        endTime;

        private AudioPlayerAttributes() {
        }
    }
}
