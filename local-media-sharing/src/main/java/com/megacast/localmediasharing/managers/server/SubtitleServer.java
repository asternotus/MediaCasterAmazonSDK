package com.megacast.localmediasharing.managers.server;

import android.content.Context;

import com.mega.cast.utils.log.SmartLog;

import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class SubtitleServer extends CastServer {

    private static final String LOG_TAG = "SubtitleServer";
    private File localFile;
    private int index = 0;
    private String mimeType;

    public SubtitleServer(Context context) {
        super(0/*HttpPort.PORT_VIDEO_SUBTITLES*/, context);
    }

    @Override
    public void start() throws IOException {
        resetPort();
        super.start();
    }

    public void setFile(File localFile, String mimeType) {
        this.localFile = localFile;
        this.mimeType = mimeType;

        try {
            SmartLog.d(LOG_TAG, "subtitle file: \n" + getStringFromFile(localFile.getAbsolutePath()));
        } catch (Exception e) {
            SmartLog.e(LOG_TAG, "Could not read file " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getRemoteUrl() {
        String name = localFile.getName();
        return super.getRemoteUrl() + "/" + getValidLink(name) + name.substring(name.lastIndexOf('.'));
    }

    @Override
    protected void setup() {

    }

    @Override
    @SuppressWarnings("deprecation")
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> params, Map<String, String> files) {
        SmartLog.d(LOG_TAG, "serve called " + uri);
        if (localFile == null) {
            SmartLog.d(LOG_TAG, "File not found");
            return Response.newFixedLengthResponse(Status.NOT_FOUND, mimeType, "File not found");
        }
        SmartLog.d(LOG_TAG, "serving file " + localFile.getName());
        try {
            FileInputStream fileInputStream = new FileInputStream(localFile);
            Response response = Response.newChunkedResponse(Status.OK, mimeType, fileInputStream);
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("content-type", "text/vtt");
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            SmartLog.d(LOG_TAG, e.getMessage());
        }
        SmartLog.d(LOG_TAG, "File not found");
        return Response.newFixedLengthResponse(Status.NOT_FOUND, mimeType, "File not found");
    }


    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }
}
