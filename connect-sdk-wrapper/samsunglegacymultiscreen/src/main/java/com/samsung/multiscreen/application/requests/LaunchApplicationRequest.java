//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.application.requests;

import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.application.Application.Status;
import com.samsung.multiscreen.application.requests.GetApplicationStateRequest;
import com.samsung.multiscreen.impl.Service;
import com.samsung.multiscreen.net.dial.DialClient;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONValue;

public class LaunchApplicationRequest implements ApplicationAsyncResult<Boolean>, Runnable {
    private static final Logger LOG = Logger.getLogger(LaunchApplicationRequest.class.getName());
    private String runTitle;
    private Map<String, String> parameters;
    private URI dialURI;
    private ApplicationAsyncResult<Boolean> callback;
    private long timeout = 30000L;
    private long sleepPeriod = 1000L;

    public LaunchApplicationRequest(String runTitle, Map<String, String> parameters, URI dialURI, ApplicationAsyncResult<Boolean> callback) {
        this.runTitle = runTitle;
        this.parameters = parameters;
        this.dialURI = dialURI;
        this.callback = callback;
    }

    public void onResult(Boolean result) {
        if(result.booleanValue()) {
            LOG.info("Launch succeeded: start polling run state");
            this.pollApplicationState();
        } else {
            this.callback.onResult(result);
        }

    }

    public void onError(ApplicationError e) {
        this.callback.onError(e);
    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        DialClient dialClient = new DialClient(this.dialURI.toString());
        String params = encodeAppParameters(this.parameters);
        LOG.info("LaunchApplicationRequest: Launching " + this.runTitle + " with parameters: " + params);
        dialClient.launchApplication(this.runTitle, params, this);
    }

    protected void pollApplicationState() {
        final long startTime = System.currentTimeMillis();
        DialClient dialClient = new DialClient(this.dialURI.toString());
        GetApplicationStateRequest getStateRequest = new GetApplicationStateRequest(this.runTitle, dialClient, new ApplicationAsyncResult<Status>() {
            public void onResult(Status result) {
                LaunchApplicationRequest.LOG.info("Poll status result: " + result.toString());
                if(result == Status.RUNNING) {
                    LaunchApplicationRequest.this.callback.onResult(Boolean.TRUE);
                } else {
                    long endTime = System.currentTimeMillis();
                    LaunchApplicationRequest.this.timeout = endTime - startTime;
                    LaunchApplicationRequest.LOG.info("Remaining state polling timeout: " + LaunchApplicationRequest.this.timeout);
                    if(LaunchApplicationRequest.this.timeout > 0L) {
                        try {
                            Thread.sleep(LaunchApplicationRequest.this.sleepPeriod);
                            LaunchApplicationRequest.this.pollApplicationState();
                        } catch (InterruptedException var5) {
                            LaunchApplicationRequest.this.callback.onResult(Boolean.FALSE);
                        }
                    } else {
                        LaunchApplicationRequest.this.callback.onResult(Boolean.FALSE);
                    }
                }

            }

            public void onError(ApplicationError e) {
                LaunchApplicationRequest.this.callback.onError(e);
            }
        });
        Service.getInstance().getExecutorService().execute(getStateRequest);
    }

    protected static String encodeAppParameters(Map<String, String> parameters) {
        if(parameters == null) {
            return "";
        } else {
            String jsonParams = JSONValue.toJSONString(parameters);
            LOG.info("LaunchApplicationRequest: params: " + jsonParams);

            try {
                return URLEncoder.encode(jsonParams, "UTF-8");
            } catch (UnsupportedEncodingException var3) {
                return "";
            }
        }
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
