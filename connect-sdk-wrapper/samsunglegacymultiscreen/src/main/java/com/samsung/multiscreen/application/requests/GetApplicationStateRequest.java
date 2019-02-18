//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.application.requests;

import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.application.Application.Status;
import com.samsung.multiscreen.net.dial.DialApplication;
import com.samsung.multiscreen.net.dial.DialClient;

public class GetApplicationStateRequest implements ApplicationAsyncResult<DialApplication>, Runnable {
    private String runTitle;
    private DialClient dialClient;
    private ApplicationAsyncResult<Status> callback;

    public GetApplicationStateRequest(String runTitle, DialClient dialClient, ApplicationAsyncResult<Status> callback) {
        this.runTitle = runTitle;
        this.dialClient = dialClient;
        this.callback = callback;
    }

    public void onResult(DialApplication dialApplication) {
        if(dialApplication != null) {
            this.callback.onResult(Status.statusFromString(dialApplication.getState()));
        } else {
            this.callback.onResult(Status.STOPPED);
        }

    }

    public void onError(ApplicationError error) {
        this.callback.onError(error);
    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        this.dialClient.getApplication(this.runTitle, this);
    }
}
