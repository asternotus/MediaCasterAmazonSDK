package com.megacast.castsdk.model;

/**
 * Created by Dmitry on 16.09.16.
 */
public class ApplicationState {

    /** Whether the app is currently running. */
    public boolean running;
    /** Whether the app is currently visible. */
    public boolean visible;

    public ApplicationState(boolean running, boolean visible) {
        this.running = running;
        this.visible = visible;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isVisible() {
        return visible;
    }
}
