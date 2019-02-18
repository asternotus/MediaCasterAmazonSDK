//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.application.requests;

import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.net.dial.DialClient;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TerminateApplicationRequest implements ApplicationAsyncResult<Boolean>, Runnable {
    private static final Logger LOG = Logger.getLogger(TerminateApplicationRequest.class.getName());
    private String runTitle;
    private String link;
    private URI dialURI;
    private ApplicationAsyncResult<Boolean> callback;

    public TerminateApplicationRequest(String runTitle, String link, URI dialURI, ApplicationAsyncResult<Boolean> callback) {
        this.runTitle = runTitle;
        this.link = link;
        this.dialURI = dialURI;
        this.callback = callback;
    }

    public void onResult(Boolean result) {
        this.callback.onResult(result);
    }

    public void onError(ApplicationError e) {
        LOG.info("TerminateApplicationpRequest: exception: " + e);
        this.callback.onError(e);
    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        LOG.info("TerminateApplicationRequest: runTitle: " + this.runTitle + " link: " + this.link);
        DialClient dialClient = new DialClient(this.dialURI.toString());
        dialClient.stopApplication(this.runTitle, this.link, this);
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
