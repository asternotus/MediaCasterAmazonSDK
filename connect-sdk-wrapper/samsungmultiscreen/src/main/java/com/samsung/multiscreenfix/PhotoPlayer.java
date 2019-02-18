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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class PhotoPlayer extends Player {
    private static final String TAG = "PhotoPlayer";
    private static final String PHOTO_PLAYER_CONTROL_RESPONSE = "state";
    private static final String PHOTO_PLAYER_QUEUE_EVENT_RESPONSE = "queue";
    private PhotoPlayer.OnPhotoPlayerListener mPhotoPlayerListener = null;
    private List<Map<String, String>> mList = null;

    PhotoPlayer(Service service, Uri uri, String appName) {
        super(service, uri, appName);
    }

    public void playContent(Uri contentUrl, Result<Boolean> result) {
        this.playContent(contentUrl, (String)null, result);
    }

    public void playContent(Uri contentUrl, String title, Result<Boolean> result) {
        JSONObject contentInfo = new JSONObject();

        try {
            if(contentUrl == null || contentUrl.toString().isEmpty()) {
                ErrorCode e = new ErrorCode("PLAYER_ERROR_INVALID_URI");
                if(this.isDebug()) {
                    SmartLog.e("PhotoPlayer", "There\'s no media url to launch!");
                }

                if(result != null) {
                    result.onError(Error.create((long)e.value(), e.name(), e.name()));
                }

                return;
            }

            contentInfo.put("uri", contentUrl);
            if(title != null) {
                contentInfo.put(PhotoPlayer.PhotoPlayerAttributes.title.name(), title);
            }
        } catch (Exception var7) {
            ErrorCode error = new ErrorCode("PLAYER_ERROR_UNKNOWN");
            if(this.isDebug()) {
                SmartLog.e("PhotoPlayer", "Unable to create JSONObject!");
            }

            if(result != null) {
                result.onError(Error.create((long)error.value(), error.name(), error.name()));
            }
        }

        super.playContent(contentInfo, ContentType.photo, result);
    }

    public void setBackgroundMusic(Uri uri) {
        mApplication.publish("playerControl", PlayerControlEvents.playMusic.name() + ":" + uri);
    }

    public void stopBackgroundMusic() {
        mApplication.publish("playerControl", PlayerControlEvents.stopMusic.name());
    }

    public void getList() {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.fetch.name());
            data.put("playerType", ContentType.photo.name());
        } catch (Exception var3) {
            SmartLog.w("PhotoPlayer", "Error in parsing JSON object: " + var3.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void clearList() {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.clear.name());
            data.put("playerType", ContentType.photo.name());
        } catch (Exception var3) {
            SmartLog.w("PhotoPlayer", "Error in parsing JSON object: " + var3.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void removeFromList(Uri uri) {
        JSONObject data = new JSONObject();

        try {
            data.put("subEvent", PlayerQueueSubEvents.dequeue.name());
            data.put("playerType", ContentType.photo.name());
            data.put("uri", uri.toString());
        } catch (Exception var4) {
            SmartLog.w("PhotoPlayer", "Error in parsing JSON object: " + var4.toString());
        }

        mApplication.publish("playerQueueEvent", data);
    }

    public void addToList(Uri uri) {
        this.addToList(uri, (String)null);
    }

    public void addToList(Uri uri, String title) {
        HashMap item = new HashMap();
        item.put("uri", uri.toString());
        item.put(PhotoPlayer.PhotoPlayerAttributes.title.name(), title);
        ArrayList list = new ArrayList();
        list.add(item);
        this.addToList((List)list);
    }

    public void addToList(final List<Map<String, String>> photoList) {
        if(photoList != null && !photoList.isEmpty()) {
            if(this.isConnected()) {
                this.getDMPStatus(new Result<DMPStatus>() {
                    public void onSuccess(DMPStatus status) {
                        if(status == null) {
                            SmartLog.d("PhotoPlayer", "Error : Something went wrong with Node server!");
                        } else {
                            if(status.mDMPRunning.booleanValue() && status.mRunning.booleanValue()) {
                                for(int it = 0; it < photoList.size(); ++it) {
                                    String contentTitle = null;
                                    Map item = (Map)photoList.get(it);
                                    if(!item.containsKey("uri")) {
                                        if(PhotoPlayer.this.isDebug()) {
                                            SmartLog.d("PhotoPlayer", "enQueue(): ContentUrl can not be Optional.");
                                        }

                                        return;
                                    }

                                    Uri contentUri = Uri.parse((String)item.get("uri"));
                                    if(item.containsKey(PhotoPlayer.PhotoPlayerAttributes.title.name())) {
                                        contentTitle = (String)item.get(PhotoPlayer.PhotoPlayerAttributes.title.name());
                                    }

                                    JSONObject data = new JSONObject();

                                    try {
                                        data.put("subEvent", PlayerQueueSubEvents.enqueue.name());
                                        data.put("playerType", ContentType.photo.name());
                                        data.put("uri", contentUri.toString());
                                        data.put(PhotoPlayer.PhotoPlayerAttributes.title.name(), contentTitle);
                                    } catch (Exception var8) {
                                        SmartLog.w("PhotoPlayer", "enQueue(): Error in parsing JSON object: " + var8.toString());
                                    }

                                    mApplication.publish("playerQueueEvent", data);
                                }
                            } else if(PhotoPlayer.this.isDebug()) {
                                SmartLog.e("PhotoPlayer", "enQueue() Error: DMP Un-Initialized!");
                            }

                        }
                    }

                    public void onError(Error error) {
                        if(PhotoPlayer.this.isDebug()) {
                            SmartLog.e("PhotoPlayer", "enQueue() Error: " + error.toString());
                        }

                    }
                });
            } else if(this.mPhotoPlayerListener != null) {
                ErrorCode errorCode = new ErrorCode("PLAYER_ERROR_PLAYER_NOT_LOADED");
                this.mPhotoPlayerListener.onError(Error.create((long)errorCode.value(), errorCode.name(), errorCode.name()));
            }

        } else {
            if(this.isDebug()) {
                SmartLog.d("PhotoPlayer", "enQueue(): photoList is NULL.");
            }

        }
    }

    public void addOnMessageListener(PhotoPlayer.OnPhotoPlayerListener listener) {
        this.mPhotoPlayerListener = listener;
        mApplication.addOnMessageListener("playerNotice", new PhotoPlayer.OnPhotoPlayerMessageListener());
    }

    public interface OnPhotoPlayerListener {
        void onPlayerInitialized();

        void onPlayerChange(String var1);

        void onPlay();

        void onPause();

        void onStop();

        void onMute();

        void onUnMute();

        void onNext();

        void onPrevious();

        void onControlStatus(int var1, Boolean var2);

        void onVolumeChange(int var1);

        void onAddToList(JSONObject var1);

        void onRemoveFromList(JSONObject var1);

        void onClearList();

        void onGetList(JSONArray var1);

        void onCurrentPlaying(JSONObject var1, String var2);

        void onApplicationResume();

        void onApplicationSuspend();

        void onError(Error var1);
    }

    private class OnPhotoPlayerMessageListener implements OnMessageListener {
        private OnPhotoPlayerMessageListener() {
        }

        public void onMessage(Message message) {
            if(PhotoPlayer.this.isDebug()) {
                SmartLog.d("PhotoPlayer", "onMessage : " + message);
            }

            if(PhotoPlayer.this.mPhotoPlayerListener == null) {
                if(PhotoPlayer.this.isDebug()) {
                    SmartLog.e("PhotoPlayer", "Unregistered PlayerListener.");
                }

            } else if(!message.getEvent().equals("playerNotice")) {
                if(PhotoPlayer.this.isDebug()) {
                    SmartLog.w("PhotoPlayer", "In-Valid Player Message");
                }

            } else {
                try {
                    JSONObject response = (JSONObject)(new JSONTokener((String)message.getData())).nextValue();
                    if(response == null) {
                        if(PhotoPlayer.this.isDebug()) {
                            SmartLog.e("PhotoPlayer", "NULL Response - Unable to parse JSON string.");
                        }

                        return;
                    }

                    String e;
                    if(response.has("subEvent")) {
                        e = response.getString("subEvent");
                        if(e.equals("playerReady")) {
                            if(PhotoPlayer.this.mAdditionalData != null) {
                                PhotoPlayer.this.mAdditionalData.put("playerType", ContentType.photo.name());
                                PhotoPlayer.this.mAdditionalData.put("subEvent", PlayerContentSubEvents.ADDITIONALMEDIAINFO.name());
                                mApplication.publish("playerContentChange", PhotoPlayer.this.mAdditionalData);
                            }

                            PhotoPlayer.this.mPhotoPlayerListener.onPlayerInitialized();
                        } else if(e.equals("playerChange")) {
                            PhotoPlayer.this.mPhotoPlayerListener.onPlayerChange(ContentType.photo.name());
                        }
                    } else {
                        int volumeStr;
                        if(response.has("playerType")) {
                            e = response.getString("playerType");
                            if(e.equalsIgnoreCase(ContentType.photo.name())) {
                                if(response.has("state")) {
                                    this.handlePlayerControlResponse(response.getString("state"));
                                } else if(response.has("queue")) {
                                    this.handlePlayerQueueEventResponse(response.getString("queue"));
                                } else if(response.has("currentPlaying")) {
                                    PhotoPlayer.this.mPhotoPlayerListener.onCurrentPlaying(response.getJSONObject("currentPlaying"), e);
                                } else if(response.has("error")) {
                                    String index = response.getString("error");

                                    try {
                                        volumeStr = Integer.parseInt(index);
                                        ErrorCode volumeLevel = new ErrorCode(volumeStr);
                                        PhotoPlayer.this.mPhotoPlayerListener.onError(Error.create((long)volumeLevel.value(), volumeLevel.name(), volumeLevel.name()));
                                    } catch (NumberFormatException var7) {
                                        PhotoPlayer.this.mPhotoPlayerListener.onError(Error.create(index));
                                    }
                                }
                            }
                        } else if(response.has("state")) {
                            e = response.toString();
                            if(e.contains(PlayerControlEvents.getControlStatus.name())) {
                                JSONObject index1 = (JSONObject)(new JSONTokener(e)).nextValue();
                                volumeStr = 0;
                                Boolean volumeLevel1 = Boolean.valueOf(false);
                                if(index1.has(PlayerControlStatus.volume.name())) {
                                    volumeStr = index1.getInt(PlayerControlStatus.volume.name());
                                }

                                if(index1.has(PlayerControlStatus.mute.name())) {
                                    volumeLevel1 = Boolean.valueOf(index1.getBoolean(PlayerControlStatus.mute.name()));
                                }

                                PhotoPlayer.this.mPhotoPlayerListener.onControlStatus(volumeStr, volumeLevel1);
                            } else if(e.contains(PlayerControlEvents.mute.name())) {
                                PhotoPlayer.this.mPhotoPlayerListener.onMute();
                            } else if(e.contains(PlayerControlEvents.unMute.name())) {
                                PhotoPlayer.this.mPhotoPlayerListener.onUnMute();
                            } else if(e.contains(PlayerControlEvents.getVolume.name())) {
                                int index2 = e.lastIndexOf(":");
                                String volumeStr1 = e.substring(index2 + 1, e.length() - 2).trim();
                                int volumeLevel2 = Integer.parseInt(volumeStr1);
                                PhotoPlayer.this.mPhotoPlayerListener.onVolumeChange(volumeLevel2);
                            }
                        } else if(response.has("appStatus")) {
                            e = response.toString();
                            if(e.contains(PlayerApplicationStatusEvents.suspend.name())) {
                                PhotoPlayer.this.mPhotoPlayerListener.onApplicationSuspend();
                            } else if(e.contains(PlayerApplicationStatusEvents.resume.name())) {
                                PhotoPlayer.this.mPhotoPlayerListener.onApplicationResume();
                            }
                        }
                    }
                } catch (Exception var8) {
                    if(PhotoPlayer.this.isDebug()) {
                        SmartLog.e("PhotoPlayer", "Error while parsing response : " + var8.getMessage());
                    }
                }

            }
        }

        private void handlePlayerControlResponse(String response) {
            try {
                if(response.contains(PlayerControlEvents.play.name())) {
                    PhotoPlayer.this.mPhotoPlayerListener.onPlay();
                } else if(response.contains(PlayerControlEvents.pause.name())) {
                    PhotoPlayer.this.mPhotoPlayerListener.onPause();
                } else if(response.contains(PlayerControlEvents.stop.name())) {
                    PhotoPlayer.this.mPhotoPlayerListener.onStop();
                } else if(response.contains(PlayerControlEvents.next.name())) {
                    PhotoPlayer.this.mPhotoPlayerListener.onNext();
                } else if(response.contains(PlayerControlEvents.previous.name())) {
                    PhotoPlayer.this.mPhotoPlayerListener.onPrevious();
                }
            } catch (Exception var3) {
                if(PhotoPlayer.this.isDebug()) {
                    SmartLog.e("PhotoPlayer", "Error while parsing control response : " + var3.getMessage());
                }
            }

        }

        private void handlePlayerQueueEventResponse(String response) {
            try {
                JSONObject e = new JSONObject(response);
                String event = e.getString("subEvent");
                e.remove("subEvent");
                if(event == null) {
                    if(PhotoPlayer.this.isDebug()) {
                        SmartLog.e("PhotoPlayer", "Sub-Event key missing from message.");
                    }

                    return;
                }

                if(event.equals(PlayerQueueSubEvents.enqueue.name())) {
                    PhotoPlayer.this.mPhotoPlayerListener.onAddToList(e);
                } else if(event.equals(PlayerQueueSubEvents.dequeue.name())) {
                    PhotoPlayer.this.mPhotoPlayerListener.onRemoveFromList(e);
                } else if(event.equals(PlayerQueueSubEvents.clear.name())) {
                    PhotoPlayer.this.mPhotoPlayerListener.onClearList();
                } else if(event.equals(PlayerQueueSubEvents.fetch.name()) && e.has("data")) {
                    PhotoPlayer.this.mPhotoPlayerListener.onGetList(e.getJSONArray("data"));
                }
            } catch (Exception var4) {
                if(PhotoPlayer.this.isDebug()) {
                    SmartLog.e("PhotoPlayer", "Error while parsing list Event response : " + var4.getMessage());
                }
            }

        }
    }

    private static enum PhotoPlayerAttributes {
        title;

        private PhotoPlayerAttributes() {
        }
    }
}
