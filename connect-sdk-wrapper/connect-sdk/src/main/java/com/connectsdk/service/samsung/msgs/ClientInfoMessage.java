package com.connectsdk.service.samsung.msgs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Dmitry on 17.01.17.
 */

public class ClientInfoMessage extends MessageBase {

    private static final String MESSAGE_KEY = "version";

    private String version = "";

    /**
     * Gets simple String message.
     *
     * @return String with simple message
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets simple message.
     *
     * @param message
     *            Simple String message
     */
    public void setVersion(String message) {
        version = message;
    }

    /**
     * @see com.connectsdk.service.samsung.msgs.MessageBase#getJSON()
     */
    @Override
    public JSONObject getJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(MESSAGE_KEY, version);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Fill message properties using String or JSONObject passed in argument.<BR>
     * JSON should have one key "message" with simple text message.<BR>
     * Example of JSONObject: {"message":"Simple text message"}
     *
     * @see com.connectsdk.service.samsung.msgs.MessageBase#fillData(java.lang.Object)
     *
     * @param data
     *            String with simple text message or <BR>
     *            JSONObject: {"message":"Simple text message"}
     */
    @Override
    void fillData(Object data) throws JSONException {
        if (data instanceof String) {
            version = (String) data;
        } else if (data instanceof JSONObject) {
            JSONObject json = (JSONObject) data;
            if (json.opt(MESSAGE_KEY) != null) {
                version = json.getString(MESSAGE_KEY);
            } else {
                version = json.toString();
            }
        }
    }

}
