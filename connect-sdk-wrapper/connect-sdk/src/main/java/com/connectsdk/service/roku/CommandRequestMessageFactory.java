package com.connectsdk.service.roku;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.service.roku.model.CommandRequestMessage;
import com.connectsdk.service.roku.model.MetaDataKeys;
import com.connectsdk.service.roku.model.RequestType;

import org.json.JSONException;

import java.util.HashMap;

/**
 * <p>Factory methods for roku controller command request: {@link RokuReceiverController#sendCommand(CommandRequestMessage)}.</p>
 * Created by Bojan Cvetojevic on 11/27/2017.
 */

public class CommandRequestMessageFactory {

    public static CommandRequestMessage createPlayRequest(MediaInfo mediaInfo) {
        HashMap<String, Object> params = new HashMap<>();
        String url = mediaInfo.getUrl();
        params.put(MetaDataKeys.URL_EXTRA_KEY, url);

        final String castType;
        if (mediaInfo.getMimeType().contains("audio")) {
            castType = MetaDataKeys.CAST_TYPE_AUDIO;
            if (mediaInfo.getImages() != null
                    && !mediaInfo.getImages().isEmpty()) {
                params.put(MetaDataKeys.ICON_URL_EXTRA_KEY, mediaInfo.getImages().get(0).getUrl());
            }
        } else if (mediaInfo.getMimeType().contains("image")) {
            castType = MetaDataKeys.CAST_TYPE_IMAGE;
        } else {
            castType = MetaDataKeys.CAST_TYPE_VIDEO;
            if (mediaInfo.getExtra() != null && mediaInfo.getExtra().has(MetaDataKeys.WIDTH_EXTRA_KEY)
                    && mediaInfo.getExtra().has(MetaDataKeys.HEIGHT_EXTRA_KEY)) {
                try {
                    params.put(MetaDataKeys.WIDTH_EXTRA_KEY, mediaInfo.getExtra().getInt(MetaDataKeys.WIDTH_EXTRA_KEY));
                    params.put(MetaDataKeys.HEIGHT_EXTRA_KEY, mediaInfo.getExtra().getInt(MetaDataKeys.HEIGHT_EXTRA_KEY));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        params.put(MetaDataKeys.CONTENT_EXTRA_KEY, castType);

        String title = mediaInfo.getTitle();
        params.put(MetaDataKeys.TITLE_EXTRA_KEY, title);

        SubtitleInfo subtitleInfo = mediaInfo.getSubtitleInfo();
        if (subtitleInfo != null) {
            String subUrl = subtitleInfo.getUrl();
            params.put(MetaDataKeys.SUBTITLE_URL_EXTRA_KEY, subUrl);
        }

        CommandRequestMessage commandRequestMessage = new CommandRequestMessage(RequestType.PLAY_REQUEST);
        commandRequestMessage.setData(params);

        return commandRequestMessage;
    }

    public static CommandRequestMessage createStatusRequest() {
        return new CommandRequestMessage(RequestType.STATUS_REQUEST);
    }

    public static CommandRequestMessage createGetDurationRequest() {
        return new CommandRequestMessage(RequestType.GET_DURATION_REQUEST);
    }

    public static CommandRequestMessage createGetPositionRequest() {
        return new CommandRequestMessage(RequestType.GET_POSITION_REQUEST);
    }

    public static CommandRequestMessage createResumeRequest() {
        return new CommandRequestMessage(RequestType.RESUME_REQUEST);
    }

    public static CommandRequestMessage createPauseRequest() {
        return new CommandRequestMessage(RequestType.PAUSE_REQUEST);
    }

    public static CommandRequestMessage createStopRequest() {
        return new CommandRequestMessage(RequestType.STOP_REQUEST);
    }

    public static CommandRequestMessage createShowProgressScreenRequest() {
        return new CommandRequestMessage(RequestType.SHOW_PROGRESS_SCREEN_REQUEST);
    }

    public static CommandRequestMessage createHideProgressScreenRequest() {
        return new CommandRequestMessage(RequestType.HIDE_PROGRESS_SCREEN_REQUEST);
    }

    /**
     *
     * @param position in ms
     * @return
     */
    public static CommandRequestMessage createSeekRequest(long position) {
        CommandRequestMessage commandRequestMessage = new CommandRequestMessage(RequestType.SET_POSITION_REQUEST);
        commandRequestMessage.getData().put(MetaDataKeys.POSITION_EXTRA_KEY, position);
        return commandRequestMessage;
    }

    public static CommandRequestMessage createClientInfoMsg(String clientVersion) {
        CommandRequestMessage commandRequestMessage = new CommandRequestMessage(RequestType.CLIENT_INFO_MSG);
        commandRequestMessage.getData().put(MetaDataKeys.CLIENT_VERSION_EXTRA_KEY, clientVersion);
        return commandRequestMessage;
    }

    public static CommandRequestMessage createClientAppTypeMsg(String clientAppType) {
        CommandRequestMessage commandRequestMessage = new CommandRequestMessage(RequestType.CLIENT_APP_TYPE_MSG);
        commandRequestMessage.getData().put(MetaDataKeys.CLIENT_APP_TYPE_EXTRA_KEY, clientAppType);
        return commandRequestMessage;
    }
}
