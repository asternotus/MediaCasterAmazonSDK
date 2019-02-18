//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.http.client;

import com.samsung.multiscreen.net.http.client.Response;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpSyncClient {
    private static final Logger LOG = Logger.getLogger(HttpSyncClient.class.getName());
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_PARTIAL = 206;
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_SEE_OTHER = 303;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_USE_PROXY = 305;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;
    public static final int HTTP_INTERNAL_ERROR = 500;
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_VERSION = 505;
    private int readTimeout = -1;
    String lastErrorMessage;

    public HttpSyncClient() {
    }

    public void setReadTimeout(int timeout) {
        this.readTimeout = timeout;
    }

    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    public Response get(URL url, Map<String, List<String>> headers) {
        return this.doGet(url, headers);
    }

    public Response post(URL url, Map<String, List<String>> headers, byte[] body) {
        return this.doPost(url, headers, body);
    }

    public Response delete(URL url, Map<String, List<String>> headers) {
        return this.doDelete(url, headers);
    }

    private Response doGet(URL url, Map<String, List<String>> headers) {
        this.lastErrorMessage = null;
        HttpURLConnection connection = null;
        Response response = null;

        try {
            connection = (HttpURLConnection)url.openConnection();
            this.setHeaders(headers, connection);
            connection.setRequestProperty("Connection", "close");
            if(this.readTimeout > 0) {
                connection.setReadTimeout(this.readTimeout);
            }

            int e = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            if(e / 100 != 2) {
                Object in = connection.getHeaderFields();
                if(in == null) {
                    in = new Hashtable();
                }

                response = new Response(connection.getResponseCode(), (Map)in, connection.getResponseMessage(), connection.getResponseMessage().getBytes("UTF-8"));
            }

            if(response == null) {
                BufferedInputStream in1 = new BufferedInputStream(connection.getInputStream());
                byte[] body = readStream(in1);
                response = new Response(connection.getResponseCode(), connection.getHeaderFields(), responseMessage, body);
            }
        } catch (IOException var12) {
            this.lastErrorMessage = "Error reading response: " + var12.getLocalizedMessage();
            LOG.info(this.lastErrorMessage);
        } finally {
            if(connection != null) {
                connection.disconnect();
            }

        }

        return response;
    }

    private Response doPost(URL url, Map<String, List<String>> headers, byte[] payload) {
        this.lastErrorMessage = null;
        HttpURLConnection connection = null;
        Response response = null;

        try {
            connection = (HttpURLConnection)url.openConnection();
            this.setHeaders(headers, connection);
            connection.setRequestProperty("Connection", "close");
            if(this.readTimeout > 0) {
                connection.setReadTimeout(this.readTimeout);
            }

            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(payload.length);
            connection.getOutputStream().write(payload);
            int e = connection.getResponseCode();
            if(e / 100 != 2) {
                Object in = connection.getHeaderFields();
                if(in == null) {
                    in = new Hashtable();
                }

                response = new Response(connection.getResponseCode(), (Map)in, connection.getResponseMessage(), connection.getResponseMessage().getBytes("UTF-8"));
            }

            if(response == null) {
                BufferedInputStream in1 = new BufferedInputStream(connection.getInputStream());
                byte[] body = readStream(in1);
                response = new Response(connection.getResponseCode(), connection.getHeaderFields(), connection.getResponseMessage(), body);
            }
        } catch (IOException var12) {
            this.lastErrorMessage = "Error sending POST: " + var12.getLocalizedMessage();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }

        }

        return response;
    }

    private Response doDelete(URL url, Map<String, List<String>> headers) {
        this.lastErrorMessage = null;
        HttpURLConnection connection = null;
        Response response = null;

        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("DELETE");
            this.setHeaders(headers, connection);
            connection.setRequestProperty("Connection", "close");
            if(this.readTimeout > 0) {
                connection.setReadTimeout(this.readTimeout);
            }

            int e = connection.getResponseCode();
            if(e / 100 != 2) {
                Object in = connection.getHeaderFields();
                if(in == null) {
                    in = new Hashtable();
                }

                response = new Response(connection.getResponseCode(), (Map)in, connection.getResponseMessage(), connection.getResponseMessage().getBytes("UTF-8"));
            }

            if(response == null) {
                BufferedInputStream in1 = new BufferedInputStream(connection.getInputStream());
                byte[] body = readStream(in1);
                response = new Response(connection.getResponseCode(), connection.getHeaderFields(), connection.getResponseMessage(), body);
            }
        } catch (IOException var11) {
            this.lastErrorMessage = "Error reading response: " + var11.getLocalizedMessage();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }

        }

        return response;
    }

    private void setHeaders(Map<String, List<String>> headers, URLConnection connection) {
        if(headers != null) {
            Iterator i$ = headers.entrySet().iterator();

            while(i$.hasNext()) {
                Entry entry = (Entry)i$.next();
                Iterator i$1 = ((List)entry.getValue()).iterator();

                while(i$1.hasNext()) {
                    String value = (String)i$1.next();
                    connection.addRequestProperty((String)entry.getKey(), value);
                }
            }
        }

    }

    private static byte[] readStream(InputStream in) throws IOException {
        byte[] buf = new byte[1024];
        boolean count = false;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        int count1;
        while((count1 = in.read(buf)) != -1) {
            out.write(buf, 0, count1);
        }

        return out.toByteArray();
    }

    public static Map<String, List<String>> initGetHeaders(URL uri) {
        String host = uri.getHost();
        int port = uri.getPort();
        String hostHeader = host + ":" + (port == -1?80:port);
        Hashtable headers = new Hashtable();
        headers.put("Host", Arrays.asList(new String[]{hostHeader}));
        return headers;
    }

    public static Map<String, List<String>> initDeleteHeaders(URL uri) {
        String host = uri.getHost();
        int port = uri.getPort();
        String hostHeader = host + ":" + (port == -1?80:port);
        Hashtable headers = new Hashtable();
        headers.put("Host", Arrays.asList(new String[]{hostHeader}));
        return headers;
    }

    public static Map<String, List<String>> initJSONGetHeaders(URL uri) {
        Map headers = initGetHeaders(uri);
        headers.put("Content-Type", Arrays.asList(new String[]{"application/json"}));
        headers.put("Connection", Arrays.asList(new String[]{"close"}));
        return headers;
    }

    public static Map<String, List<String>> initPOSTHeaders(URL uri) {
        String host = uri.getHost();
        int port = uri.getPort();
        String hostHeader = host + ":" + (port == -1?80:port);
        Hashtable headers = new Hashtable();
        headers.put("Host", Arrays.asList(new String[]{hostHeader}));
        return headers;
    }

    public static Map<String, List<String>> initJSONPostHeaders(URL uri) {
        Map headers = initPOSTHeaders(uri);
        headers.put("Content-Type", Arrays.asList(new String[]{"application/json"}));
        return headers;
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
