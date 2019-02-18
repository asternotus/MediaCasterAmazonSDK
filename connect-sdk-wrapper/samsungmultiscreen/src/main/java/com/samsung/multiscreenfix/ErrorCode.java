//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;

class ErrorCode {
    private static Map<Integer, String> mErrorCode = new HashMap();
    private int mError;

    private void initErrorCode() {
        mErrorCode.put(Integer.valueOf(100), "ERROR_UNKNOWN");
        mErrorCode.put(Integer.valueOf(101), "PLAYER_ERROR_GENEREIC");
        mErrorCode.put(Integer.valueOf(102), "PLAYER_ERROR_CONNECTION_FAILED");
        mErrorCode.put(Integer.valueOf(103), "PLAYER_ERROR_AUDIO_CODEC_NOT_SUPPORTED");
        mErrorCode.put(Integer.valueOf(104), "PLAYER_ERROR_NOT_SUPPORTED_FILE");
        mErrorCode.put(Integer.valueOf(105), "PLAYER_ERROR_VIDEO_CODEC_NOT_SUPPORTED");
        mErrorCode.put(Integer.valueOf(106), "PLAYER_ERROR_PLAYER_NOT_LOADED");
        mErrorCode.put(Integer.valueOf(107), "PLAYER_ERROR_INVALID_OPERATION");
        mErrorCode.put(Integer.valueOf(108), "PLAYER_ERROR_INVALID_PARAMETER");
        mErrorCode.put(Integer.valueOf(109), "PLAYER_ERROR_NO_SUCH_FILE");
        mErrorCode.put(Integer.valueOf(110), "PLAYER_ERROR_SEEK_FAILED");
        mErrorCode.put(Integer.valueOf(111), "PLAYER_ERROR_REWIND");
        mErrorCode.put(Integer.valueOf(112), "PLAYER_ERROR_FORWARD");
        mErrorCode.put(Integer.valueOf(113), "PLAYER_ERROR_RESTORE");
        mErrorCode.put(Integer.valueOf(114), "PLAYER_ERROR_RESOURCE_LIMIT");
        mErrorCode.put(Integer.valueOf(115), "PLAYER_ERROR_INVALID_STATE");
        mErrorCode.put(Integer.valueOf(116), "PLAYER_ERROR_NO_AUTH");
        mErrorCode.put(Integer.valueOf(117), "PLAYER_ERROR_LAST_CONTENT");
        mErrorCode.put(Integer.valueOf(118), "PLAYER_ERROR_CURRENT_CONTENT");
        mErrorCode.put(Integer.valueOf(401), "PLAYER_ERROR_INVALID_URI");
        mErrorCode.put(Integer.valueOf(500), "PLAYER_ERROR_INTERNAL_SERVER");
        mErrorCode.put(Integer.valueOf(300), "PLAYER_ERROR_INVALID_TV_RESPONSE");
        mErrorCode.put(Integer.valueOf(310), "ERROR_CONNECT_FAILED");
        mErrorCode.put(Integer.valueOf(311), "ERROR_ALREADY_CONNECTED");
        mErrorCode.put(Integer.valueOf(312), "ERROR_HOST_UNREACHABLE");
        mErrorCode.put(Integer.valueOf(313), "ERROR_WEBSOCKET_DISCONNECTED");
    }

    ErrorCode(String name) {
        this.initErrorCode();
        if(name != null && !name.isEmpty()) {
            Iterator var2 = mErrorCode.entrySet().iterator();

            while(var2.hasNext()) {
                Entry entry = (Entry)var2.next();
                if(name.equals(entry.getValue())) {
                    this.mError = ((Integer)entry.getKey()).intValue();
                }
            }
        } else {
            this.mError = 100;
        }

    }

    ErrorCode(int value) {
        this.initErrorCode();
        if(mErrorCode.containsKey(Integer.valueOf(value))) {
            this.mError = value;
            mErrorCode.get(Integer.valueOf(value));
        } else {
            this.mError = 100;
        }

    }

    public int value() {
        return this.mError;
    }

    public String name() {
        return (String)mErrorCode.get(Integer.valueOf(this.mError));
    }
}
