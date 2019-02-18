package com.connectsdk.service.roku.model;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Bojan Cvetojevic on 11/24/2017.
 */

public class CommandRequestMessage extends AbstractRokuMessage {

    private String requestType;

    public CommandRequestMessage(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

}
