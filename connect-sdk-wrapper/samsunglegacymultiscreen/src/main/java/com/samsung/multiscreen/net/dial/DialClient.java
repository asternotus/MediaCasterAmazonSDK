//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.dial;

import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.net.dial.DialApplication;
import com.samsung.multiscreen.net.dial.DialResponseHandler;
import com.samsung.multiscreen.net.http.client.HttpSyncClient;
import com.samsung.multiscreen.net.http.client.Response;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DialClient {
    public static final Logger LOG = Logger.getLogger(DialClient.class.getName());
    public static final String URN = "urn:dial-multiscreen-org:device:dialreceiver:1";
    private String END_POINT;
    DialResponseHandler handler;

    public DialClient(String endPoint) {
        this.END_POINT = endPoint;
        this.handler = new DialResponseHandler();
    }

    public void launchApplication(String id, String params, ApplicationAsyncResult<Boolean> callback) {
        try {
            URL e = URI.create(this.END_POINT + id).toURL();
            HttpSyncClient client = new HttpSyncClient();
            client.setReadTimeout(10000);
            Map headers = HttpSyncClient.initPOSTHeaders(e);
            headers.put("Content-Type", Arrays.asList(new String[]{"text/plain"}));
            Response response = client.post(e, headers, params.getBytes("UTF-8"));
            if(response == null) {
                callback.onResult(Boolean.FALSE);
                return;
            }

            this.handler.handleLaunchResponse(response, callback);
        } catch (MalformedURLException var8) {
            callback.onError(ApplicationError.createWithException(var8));
        } catch (UnsupportedEncodingException var9) {
            callback.onError(ApplicationError.createWithException(var9));
        }

    }

    public void stopApplication(String id, String link, ApplicationAsyncResult<Boolean> callback) {
        try {
            String e = this.END_POINT + id + (link != null && !link.isEmpty()?"/" + link:"");
            URL url = URI.create(e).toURL();
            HttpSyncClient client = new HttpSyncClient();
            Map headers = HttpSyncClient.initDeleteHeaders(url);
            client.setReadTimeout(10000);
            Response response = client.delete(url, headers);
            if(response == null) {
                callback.onError(new ApplicationError("Not found"));
                return;
            }

            this.handler.handleStopResponse(response, callback);
        } catch (MalformedURLException var9) {
            callback.onError(ApplicationError.createWithException(var9));
        }

    }

    public void getApplication(String id, ApplicationAsyncResult<DialApplication> callback) {
        try {
            URL e = URI.create(this.END_POINT + id).toURL();
            HttpSyncClient client = new HttpSyncClient();
            client.setReadTimeout(10000);
            Map headers = HttpSyncClient.initGetHeaders(e);
            Response response = client.get(e, headers);
            if(response == null) {
                callback.onError(new ApplicationError("Not found"));
                return;
            }

            this.handler.handleGetApplicationResponse(response, callback);
        } catch (MalformedURLException var7) {
            callback.onError(ApplicationError.createWithException(var7));
        }

    }

    public static void main(String[] args) {
        String endPoint = "http://192.168.1.94:8001/ws/apps/";
        String appId = "ChatDemo";
        String params = "%7B%22launcher%22%3A%22android%22%7D";
        DialClient dc = new DialClient(endPoint);
        LOG.info("-----------------------");
        LOG.info("TEST: getApplication()");
        LOG.info("-----------------------");
        dc.getApplication(appId, new ApplicationAsyncResult<DialApplication>() {
            public void onResult(DialApplication result) {
                DialClient.LOG.info("DIAL Application: " + result);
            }

            public void onError(ApplicationError e) {
                DialClient.LOG.info("DIAL Application: Error: " + e);
            }
        });

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException var9) {
            var9.printStackTrace();
        }

        LOG.info("-----------------------");
        LOG.info("TEST: stopApplication()");
        LOG.info("-----------------------");
        dc.stopApplication(appId, "run", new ApplicationAsyncResult<Boolean>() {
            public void onResult(Boolean result) {
                DialClient.LOG.info("Dial Stop: " + result);
            }

            public void onError(ApplicationError e) {
                DialClient.LOG.info("DIAL Stop Error: " + e);
            }
        });

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException var8) {
            var8.printStackTrace();
        }

        LOG.info("-----------------------");
        LOG.info("TEST(Should fail): launchApplication()");
        LOG.info("-----------------------");
        dc.launchApplication("zzcxz", params, new ApplicationAsyncResult<Boolean>() {
            public void onResult(Boolean result) {
                DialClient.LOG.info("Dial Launch: " + result);
            }

            public void onError(ApplicationError e) {
                DialClient.LOG.info("DIAL Launch Error: " + e);
            }
        });

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException var7) {
            var7.printStackTrace();
        }

        LOG.info("-----------------------");
        LOG.info("TEST: launchApplication()");
        LOG.info("-----------------------");
        dc.launchApplication(appId, params, new ApplicationAsyncResult<Boolean>() {
            public void onResult(Boolean result) {
                DialClient.LOG.info("Dial Launch: " + result);
            }

            public void onError(ApplicationError e) {
                DialClient.LOG.info("DIAL Launch Error: " + e);
            }
        });

        try {
            Thread.sleep(20000L);
        } catch (InterruptedException var6) {
            var6.printStackTrace();
        }

        LOG.info("-----------------------");
        LOG.info("TEST: getApplication()");
        LOG.info("-----------------------");
        dc.getApplication(appId, new ApplicationAsyncResult<DialApplication>() {
            public void onResult(DialApplication result) {
                DialClient.LOG.info("DIAL Application: " + result);
            }

            public void onError(ApplicationError e) {
                DialClient.LOG.info("DIAL Application: Error: " + e);
            }
        });
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
