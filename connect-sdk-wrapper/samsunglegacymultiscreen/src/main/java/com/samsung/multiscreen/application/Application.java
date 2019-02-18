//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.application;

import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.application.requests.GetApplicationStateRequest;
import com.samsung.multiscreen.application.requests.InstallApplicationRequest;
import com.samsung.multiscreen.application.requests.LaunchApplicationRequest;
import com.samsung.multiscreen.application.requests.TerminateApplicationRequest;
import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.impl.Service;
import com.samsung.multiscreen.net.dial.DialClient;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {
    private static final Logger LOG = Logger.getLogger(Application.class.getName());
    private String runTitle;
    private Device device;
    private URI dialURI;
    private Application.Status lastKnownStatus;
    private String link;
    private String installURL;

    public Application(Device device, URI appURI, String runTitle, Application.Status initialStatus, String link, String installURL) {
        this.device = device;
        this.dialURI = appURI;
        this.runTitle = runTitle;
        this.lastKnownStatus = initialStatus;
        this.link = link;
        this.installURL = installURL;
    }

    public Device getDevice() {
        return this.device;
    }

    public String getRunTitle() {
        return this.runTitle;
    }

    public Application.Status getLastKnownStatus() {
        return this.lastKnownStatus;
    }

    public void updateStatus(final ApplicationAsyncResult<Application.Status> callback) {
        DialClient dialClient = new DialClient(this.dialURI.toString());
        GetApplicationStateRequest getApplicationStateRequest = new GetApplicationStateRequest(this.runTitle, dialClient, new ApplicationAsyncResult<Application.Status>() {
            public void onResult(Application.Status result) {
                Application.this.lastKnownStatus = result;
                callback.onResult(result);
            }

            public void onError(ApplicationError e) {
                callback.onError(e);
            }
        });
        Service.getInstance().getExecutorService().execute(getApplicationStateRequest);
    }

    public void launch(ApplicationAsyncResult<Boolean> callback) {
        HashMap parameters = new HashMap();
        this.launch(parameters, callback);
    }

    public void launch(Map<String, String> parameters, final ApplicationAsyncResult<Boolean> callback) {
        LaunchApplicationRequest launchApplicationRequest = new LaunchApplicationRequest(this.runTitle, parameters, this.dialURI, new ApplicationAsyncResult<Boolean>() {
            public void onResult(Boolean result) {
                if(result.booleanValue()) {
                    Application.this.lastKnownStatus = Application.Status.RUNNING;
                } else if(Application.this.lastKnownStatus != Application.Status.INSTALLABLE) {
                    Application.this.lastKnownStatus = Application.Status.STOPPED;
                }

                callback.onResult(result);
            }

            public void onError(ApplicationError e) {
                callback.onError(e);
            }
        });
        Service.getInstance().getExecutorService().execute(launchApplicationRequest);
    }

    public void terminate(ApplicationAsyncResult<Boolean> callback) {
        String actionLink = this.link == null?"run":this.link;
        TerminateApplicationRequest terminateApplicationRequest = new TerminateApplicationRequest(this.runTitle, actionLink, this.dialURI, callback);
        Service.getInstance().getExecutorService().execute(terminateApplicationRequest);
    }

    public void install(ApplicationAsyncResult<Boolean> callback) {
        if(this.installURL != null && !this.installURL.isEmpty()) {
            URI installURI = URI.create(this.installURL);
            if(installURI != null && !installURI.toString().isEmpty()) {
                InstallApplicationRequest installApplicationRequest = new InstallApplicationRequest(installURI, callback);
                Service.getInstance().getExecutorService().execute(installApplicationRequest);
            } else {
                callback.onError(new ApplicationError("Invalid install URL"));
            }
        } else {
            callback.onError(new ApplicationError("Invalid install URL"));
        }
    }

    static {
        LOG.setLevel(Level.OFF);
    }

    public static enum Status {
        STOPPED,
        RUNNING,
        INSTALLABLE;

        private static final String STATE_NOT_RUNNING = "Not running";
        private static final String STATE_NOT_STARTED = "not started";
        private static final String STATE_STARTING = "Starting";
        private static final String STATE_RUNNING = "running";
        private static final String STATE_STOPPED = "stopped";
        private static final String STATE_INSTALLABLE = "installable";

        private Status() {
        }

        public static Application.Status statusFromString(String state) {
            Application.Status status;
            if(state == null) {
                status = STOPPED;
            } else if(state.equalsIgnoreCase("Not running")) {
                status = STOPPED;
            } else if(state.equalsIgnoreCase("not started")) {
                status = STOPPED;
            } else if(state.equalsIgnoreCase("stopped")) {
                status = STOPPED;
            } else if(state.equalsIgnoreCase("running")) {
                status = RUNNING;
            } else if(state.equalsIgnoreCase("Starting")) {
                status = RUNNING;
            } else if(state.contains("installable".toLowerCase())) {
                status = INSTALLABLE;
            } else {
                status = STOPPED;
            }

            return status;
        }
    }
}
