package com.megacast.castsdk.model;

/**
 * Created by Dmitry on 16.11.16.
 */

public class ReceiverInfo {

    boolean running;

    public ReceiverInfo(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
