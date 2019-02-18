package com.mega.cast.utils.log;

import android.text.TextUtils;

/**
 * Created by Дима on 22.09.2017.
 */

public class ApplicationTracker {

    private static ApplicationTracker instance = new ApplicationTracker();

    private MessageSender sender;

    public static ApplicationTracker getInstance() {
        return instance;
    }

    private ApplicationTracker() {
    }

    public void init(MessageSender sender) {
        this.sender = sender;
    }

    public void sendMessage(String message) {
        if (!TextUtils.isEmpty(message)) {
            sender.sendMessage(message);
        }
    }

}
