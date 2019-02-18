package com.connectsdk.service.roku.model;

/**
 * Contains the statically defined meta keys for {@link CommandRequestMessage} and {@link EventMessage} data object<br>
 * Created by Bojan Cvetojevic on 11/28/2017.
 */

public class MetaDataKeys {
    public static final String URL_EXTRA_KEY = "url";
    public static final String TITLE_EXTRA_KEY = "title";
    /**
     * Corresponds to the cast media type
     */
    public static final String CONTENT_EXTRA_KEY = "content";
    public static final String STATE_EXTRA_KEY = "state";
    public static final String POSITION_EXTRA_KEY = "position";
    public static final String ICON_URL_EXTRA_KEY = "iconUrl";
    public static final String MSG_EXTRA_KEY = "msg";
    public static final String ERROR_MSG_EXTRA_KEY = "errorMsg";
    public static final String ERROR_CODE_EXTRA_KEY = "errorCode";
    public static final String CLIENT_VERSION_EXTRA_KEY = "clientVersion";

    public static final String CAST_TYPE_AUDIO = "audio";
    public static final String CAST_TYPE_IMAGE = "image";
    public static final String CAST_TYPE_VIDEO = "video";

    public static final String SUBTITLE_URL_EXTRA_KEY = "subtitleUrl";

    public static final String VALUE_EXTRA_KEY = "value";
    public static final String WIDTH_EXTRA_KEY = "width";
    public static final String HEIGHT_EXTRA_KEY = "height";
    public static final String CLIENT_APP_TYPE_EXTRA_KEY = "app_type";
}
