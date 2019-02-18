package com.megacast.localmediasharing.managers.server;

import android.content.Context;

import com.connectsdk.service.DLNAService;
import com.connectsdk.service.DeviceService;
import com.mega.cast.utils.log.SmartLog;
import com.megacast.connectsdkwrapper.internal.model.DlnaDevice;


import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class WebServerLocalMedia extends CastServer {

    private static final String LOG_TAG = NanoHTTPD.class.getSimpleName() + WebServerLocalMedia.class.getSimpleName();
    private static final long BUFFER_SIZE = 10000;

    private File serverFile;
    private String mimeType;

    private DataTransferListener curDataTransferListener;
    private boolean DLNAFixEnabled = false;

    public WebServerLocalMedia(Context context) {
        super(0/*HttpPort.NON_TRANSCODED_MEDIA_PORT*/, context);
//        setApplyRangeRequestHeaderFix(true); // TODO implement
    }

    @Override
    public void start() throws IOException {
        resetPort();
        super.start();
    }

    public void setFile(File serverFile, String mimeType) {
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
    @SuppressWarnings("deprecation")
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> params, Map<String, String> files) {
        SmartLog.i(LOG_TAG, "serve  " + method.name() + " " + uri.toString());

        String range = null;

        boolean dlna_info = false;

        SmartLog.i(LOG_TAG, "Request headers:");
        for (String key : headers.keySet()) {
            SmartLog.i(LOG_TAG, "  " + key + ":" + headers.get(key));
            if ("range".equals(key)) {
                range = headers.get(key);
            }
            if ("getcontentfeatures.dlna.org".equals(key)) {
                // samsung platform doest not work without it
                dlna_info = DLNAFixEnabled;
            }
        }

        SmartLog.i(LOG_TAG, "Request params:");
        for (String key : params.keySet()) {
            SmartLog.i(LOG_TAG, "  " + key + ":" + params.get(key));
        }

        try {
            if (range == null) {
                if (method == Method.HEAD && dlna_info) {
                    final long fileLength = serverFile.length();

                    Response response = getFullResponse(mimeType);
                    response.addHeader("Content-Length", fileLength + "");
                    response.addHeader("Content-Type", mimeType);

                    String protocolInfo = "http-get:*:" + mimeType + ":*";
                    if (getDevice() instanceof DlnaDevice) {
                        Collection<DeviceService> services = ((DlnaDevice) getDevice()).getConnectableDevice().getServices();
                        DLNAService dlnaService = null;
                        for (DeviceService service : services) {
                            if (service instanceof DLNAService) {
                                dlnaService = (DLNAService) service;
                            }
                        }
                        if (dlnaService != null) {
                            String serviceProtocolInfo = dlnaService.getProtocolInfo(mimeType);
                            String[] split = serviceProtocolInfo.split(":");
                            protocolInfo = split[split.length - 1];
                        }
                    }
                    SmartLog.i(LOG_TAG, "Protocol info: " + protocolInfo);

                    response.addHeader("contentFeatures.dlna.org", protocolInfo);
//                    response.addHeader("contentFeatures.dlna.org", "DLNA.ORG_PN=PNG_LRG;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000");
                    response.addHeader("transfermode.dlna.org", "Streaming");
                    response.addHeader("Connection", "close");
                    response.addHeader("Cache-Control", "no-cache");
                    response.addHeader("Accept-Ranges", "bytes");

                    return response;
//                    Response response = Response.newFixedLengthResponse(Status.OK, mimeType, "");
//                    response.addHeader("Content-Length", fileLength + "");
//                    response.addHeader("Content-Type", mimeType);
//                    response.addHeader("Access-Control-Allow-Origin", "*");
//                    return response;
                }

                return getFullResponse(mimeType);
            } else {
                return getPartialResponse(mimeType, range, headers);
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

        final long startBytes = 0;
        final long fileLength = serverFile.length();

        serverConnection.update(startBytes, fileLength);

        curDataTransferListener = new DataTransferListener() {
            @Override
            public void onDataTransfer(long bytesPushed, long bytesTotal) {
                SmartLog.d(LOG_TAG, String.format("onDataTransfer %d / %d", startBytes + bytesPushed, fileLength));
                if (curDataTransferListener == this) {
                    serverConnection.update(startBytes + bytesPushed, fileLength);
                }
            }

            @Override
            public void onDataTransferStarted() {
                SmartLog.d(LOG_TAG, "onDataTransferStarted");
                if (curDataTransferListener == this) {
                    serverConnection.onMonitoringStarted();
                }
            }
        };

        FileInputStream fileInputStream = new FileInputStream(serverFile);

        Response response = Response.newChunkedResponse(Status.OK, mimeType, fileInputStream);
        response.setDataTransferListener(curDataTransferListener);
        return response;
    }

    private Response getPartialResponse(String mimeType, String rangeHeader, Map<String, String> headers) throws IOException {
        SmartLog.i(LOG_TAG, "getPartialResponse " + mimeType + " " + rangeHeader);
        String rangeValue = rangeHeader.trim().substring("bytes=".length());
        final long fileLength = serverFile.length();
        SmartLog.i(LOG_TAG, "server file length " + fileLength);
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
        if (start <= end) {

            final long startBytes = start;
            serverConnection.update(startBytes, fileLength);

            curDataTransferListener = new DataTransferListener() {
                @Override
                public void onDataTransfer(long bytesPushed, long bytesTotal) {
                    SmartLog.d(LOG_TAG, String.format("onDataTransfer %d / %d", startBytes + bytesPushed, fileLength));
                    if (curDataTransferListener == this) {
                        serverConnection.update(startBytes + bytesPushed, fileLength);
                    }
                }

                @Override
                public void onDataTransferStarted() {
                    SmartLog.d(LOG_TAG, "onDataTransferStarted");
                    if (curDataTransferListener == this) {
                        serverConnection.onMonitoringStarted();
                    }
                }
            };

            long contentLength = end - start + 1;

            //cleanupStreams();
            FileInputStream fileInputStream = new FileInputStream(serverFile);
            //noinspection ResultOfMethodCallIgnored
            long skipped = fileInputStream.skip(start);

            SmartLog.i(LOG_TAG, "skipping  " + skipped + " from " + start);
            SmartLog.i(LOG_TAG, "content length  " + contentLength);

            Response response = Response.newFixedLengthResponse(Status.PARTIAL_CONTENT, mimeType, fileInputStream, contentLength);
            response.setDataTransferListener(curDataTransferListener);

            final String contentRange = "bytes " + start + "-" + end + "/" + fileLength;
            SmartLog.i(LOG_TAG, "contentLength " + contentLength);
            SmartLog.i(LOG_TAG, "contentRange " + contentRange);
            SmartLog.i(LOG_TAG, "mime type " + mimeType);

            response.addHeader("Content-Length", contentLength + "");
            response.addHeader("Content-Range", contentRange);
            response.addHeader("Content-Type", mimeType);
            response.addHeader("Access-Control-Allow-Origin", "*");
            return response;
        } else {
            return Response.newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, "html", rangeHeader);
        }
    }

    public void setDLNAFixEnabled(boolean DLNAFixEnabled) {
        this.DLNAFixEnabled = DLNAFixEnabled;
    }
}
