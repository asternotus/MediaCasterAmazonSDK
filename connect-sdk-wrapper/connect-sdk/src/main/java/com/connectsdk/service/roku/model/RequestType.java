package com.connectsdk.service.roku.model;

/**
 * Created by Bojan Cvetojevic on 11/24/2017.
 */
// TODO: 11/24/2017 convert this to enums maybe
public final class RequestType {
    public static final String STATUS_REQUEST = "STATUS_REQUEST";
    public static final String PLAY_REQUEST = "PLAY_REQUEST";
    public static final String RESUME_REQUEST = "RESUME_REQUEST";
    public static final String PAUSE_REQUEST = "PAUSE_REQUEST";
    public static final String STOP_REQUEST = "STOP_REQUEST";
    public static final String SET_POSITION_REQUEST = "SET_POSITION_REQUEST";
    public static final String GET_POSITION_REQUEST = "GET_POSITION_REQUEST";
    public static final String GET_DURATION_REQUEST = "GET_DURATION_REQUEST";
    public static final String SHOW_PROGRESS_SCREEN_REQUEST = "SHOW_PROGRESS_SCREEN_REQUEST";
    public static final String HIDE_PROGRESS_SCREEN_REQUEST = "HIDE_PROGRESS_SCREEN_REQUEST";
    public static final String CLIENT_INFO_MSG = "CLIENT_INFO_MSG";
    public static final String DISCONNECT_REQUEST = "DISCONNECT_REQUEST";
    public static final String CLIENT_APP_TYPE_MSG = "CLIENT_APP_TYPE_MSG";
}
