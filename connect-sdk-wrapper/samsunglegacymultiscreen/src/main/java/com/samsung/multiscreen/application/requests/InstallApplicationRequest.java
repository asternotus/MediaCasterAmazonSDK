//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.application.requests;

import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.net.http.client.HttpSyncClient;
import com.samsung.multiscreen.net.http.client.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InstallApplicationRequest implements Runnable {
    private static final Logger LOG = Logger.getLogger(InstallApplicationRequest.class.getName());
    private URI installURI;
    private ApplicationAsyncResult<Boolean> callback;

    public InstallApplicationRequest(URI installURI, ApplicationAsyncResult<Boolean> callback) {
        this.installURI = installURI;
        this.callback = callback;
    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        LOG.info("InstallApplicationRequest URI: " + this.installURI);

        try {
            URL e = this.installURI.toURL();
            HttpSyncClient client = new HttpSyncClient();
            Map headers = HttpSyncClient.initGetHeaders(e);
            client.setReadTimeout(25000);
            Response response = client.get(e, headers);
            if(response == null) {
                this.callback.onResult(Boolean.FALSE);
                return;
            }

            if(response.status == 200) {
                this.callback.onResult(Boolean.TRUE);
            } else {
                this.callback.onResult(Boolean.FALSE);
            }
        } catch (MalformedURLException var5) {
            this.callback.onError(ApplicationError.createWithException(var5));
        }

    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
