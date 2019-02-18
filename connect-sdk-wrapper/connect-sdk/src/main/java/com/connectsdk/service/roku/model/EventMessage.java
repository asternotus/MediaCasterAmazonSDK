package com.connectsdk.service.roku.model;


/**
 * Created by Bojan Cvetojevic on 11/24/2017.
 */

public class EventMessage extends AbstractRokuMessage {

    private String messageType;

    public EventMessage() {
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

}
