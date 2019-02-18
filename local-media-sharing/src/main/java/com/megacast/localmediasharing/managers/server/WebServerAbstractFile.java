package com.megacast.localmediasharing.managers.server;

import android.content.Context;

import com.mega.cast.utils.log.SmartLog;
import com.megacast.castsdk.model.AbstractFile;

import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by Dmitry on 25.07.17.
 */

public class WebServerAbstractFile extends CastServer {

    private static final String LOG_TAG = NanoHTTPD.class.getSimpleName() + WebServerAbstractFile.class.getSimpleName();

    private AbstractFile serverFile;
    private String mimeType;
    private DataTransferListener curDataTransferListener;

    public WebServerAbstractFile(Context context) {
        super(0, context);
    }

    @Override
    public void start() throws IOException {
        resetPort();
        super.start();
    }

    public void setFile(AbstractFile serverFile, String mimeType) {
        this.serverFile = serverFile;
        this.mimeType = mimeType;
    }

    @Override
    public String getRemoteUrl() {
        String name = serverFile.getName();
        return super.getRemoteUrl() + "/" + getValidLink(name) + name.substring(name.lastIndexOf('.'));
    }

    @Override
    protected void setup() {

    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> params, Map<String, String> files) {
        SmartLog.i(LOG_TAG, "serve  " + uri.toString());

        String range = null;

        boolean dlna_info = false;

        SmartLog.d(LOG_TAG, "Request headers:");
        for (String key : headers.keySet()) {
            SmartLog.d(LOG_TAG, "  " + key + ":" + headers.get(key));
            if ("range".equals(key)) {
                range = headers.get(key);
            }
            if ("getcontentfeatures.dlna.org".equals(key)){
                dlna_info = true;
            }
        }

        SmartLog.d(LOG_TAG, "Request params:");
        for (String key : params.keySet()) {
            SmartLog.d(LOG_TAG, "  " + key + ":" + params.get(key));
        }

        try {
            if (range == null) {

                if (method == Method.HEAD && dlna_info){
                    final long fileLength = serverFile.length();
                    Response response = Response.newFixedLengthResponse(Status.OK, mimeType, "");
                    response.addHeader("Content-Length", fileLength + "");
                    response.addHeader("Content-Type", mimeType);
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    return response;
                }

                return getFullResponse(mimeType);
            } else {
                return getPartialResponse(mimeType, range);
            }
        } catch (IOException e) {
            SmartLog.e(LOG_TAG, "Exception serving file", e);
        }

        return Response.newFixedLengthResponse(Status.NOT_FOUND, mimeType, "File not found");
    }

    @Override
    public void stop() {
        SmartLog.i(LOG_TAG, "Stop was Called: ");
        super.stop();
    }

    private Response getFullResponse(String mimeType) throws FileNotFoundException {
        SmartLog.i(LOG_TAG, "getFullResponse " + mimeType);

        InputStream fileInputStream = null;
        final long fileLength;
        try {

            final long startBytes = 0;
            fileLength = serverFile.length();

            serverConnection.update(startBytes, fileLength);

            curDataTransferListener = new DataTransferListener(){
                @Override
                public void onDataTransfer(long bytesPushed, long bytesTotal) {
                    if (curDataTransferListener == this){
                        serverConnection.update(startBytes + bytesPushed, fileLength);
                    }
                }

                @Override
                public void onDataTransferStarted() {
                    if (curDataTransferListener == this){
                        serverConnection.onMonitoringStarted();
                    }
                }
            };

            fileInputStream = serverFile.getInputSteam();
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileNotFoundException(e.getMessage());
        }

        Response response = Response.newChunkedResponse(Status.OK, mimeType, fileInputStream);
        response.setDataTransferListener(curDataTransferListener);
        return response;
    }

    private Response getPartialResponse(String mimeType, String rangeHeader) throws IOException {
        SmartLog.i(LOG_TAG, "getPartialResponse " + mimeType + " " + rangeHeader);
        String rangeValue = rangeHeader.trim().substring("bytes=".length());
        final long fileLength = serverFile.length();
        SmartLog.d(LOG_TAG, "server file length " + fileLength);
        long start, end;
        if (rangeValue.startsWith("-")) {
            end = fileLength - 1;
            start = fileLength - 1
                    - Long.parseLong(rangeValue.substring("-".length()));

        } else {
            String[] range = rangeValue.split("-");
            start = Long.parseLong(range[0]);
            end = range.length > 1 ? Long.parseLong(range[1])
                    : fileLength - 1;
        }

        if (end > fileLength - 1) {
            end = fileLength - 1;
        }

        SmartLog.d(LOG_TAG, "serve from " + start + " to " + end);

        if (start <= end) {

            final long startBytes = start;
            serverConnection.update(startBytes, fileLength);

            curDataTransferListener = new DataTransferListener(){
                @Override
                public void onDataTransfer(long bytesPushed, long bytesTotal) {
                    if (curDataTransferListener == this){
                        serverConnection.update(startBytes + bytesPushed, fileLength);
                    }
                }

                @Override
                public void onDataTransferStarted() {
                    if (curDataTransferListener == this) {
                        serverConnection.onMonitoringStarted();
                    }
                }
            };

            long contentLength = end - start + 1;

            //cleanupStreams();
            InputStream fileInputStream = serverFile.getInputSteam();
            //noinspection ResultOfMethodCallIgnored
            long skipped = fileInputStream.skip(start);

            SmartLog.d(LOG_TAG, "skipping  " + skipped + " from " + start);
            SmartLog.d(LOG_TAG, "content length  " + contentLength);

            Response response = Response.newFixedLengthResponse(Status.PARTIAL_CONTENT, mimeType, fileInputStream, contentLength);
            response.setDataTransferListener(curDataTransferListener);

            final String contentRange = "bytes " + start + "-" + end + "/" + fileLength;
            SmartLog.d(LOG_TAG, "contentLength " + contentLength);
            SmartLog.i(LOG_TAG, "contentRange " + contentRange);
            SmartLog.i(LOG_TAG, "mime type " + mimeType);

            response.addHeader("Content-Length", contentLength + "");
            response.addHeader("Content-Range", contentRange);
            response.addHeader("Content-Type", mimeType);
            response.addHeader("Access-Control-Allow-Origin", "*");
            return response;
        } else {
            SmartLog.e(LOG_TAG, "Range not satisfiable");
            return Response.newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, "html", rangeHeader);
        }
    }

}


