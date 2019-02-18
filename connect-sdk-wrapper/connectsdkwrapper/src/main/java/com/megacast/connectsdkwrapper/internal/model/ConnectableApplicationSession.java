package com.megacast.connectsdkwrapper.internal.model;

import com.connectsdk.service.sessions.LaunchSession;
import com.megacast.castsdk.model.ApplicationSession;

/**
 * Created by Dmitry on 16.09.16.
 */
public class ConnectableApplicationSession implements ApplicationSession {

    private LaunchSession launchSession;

    public ConnectableApplicationSession(LaunchSession launchSession) {
        this.launchSession = launchSession;
    }

    public LaunchSession getLaunchSession() {
        return launchSession;
    }
}
