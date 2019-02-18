package com.megacast.localmediasharing.managers.server;


import android.content.Context;

import com.connectsdk.service.DLNAService;
import com.connectsdk.service.DeviceService;
import com.mega.cast.utils.log.SmartLog;
import com.megacast.localmediasharing.managers.server.original.NanoHTTPD;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.megacast.connectsdkwrapper.internal.model.DlnaDevice;

public class WebServerImage extends CastServer {

    private static final String TAG = NanoHTTPD.class.getSimpleName() + WebServerImage.class.getSimpleName();
    private static final long BUFFER_SIZE = 1000000;
    private static final String LOG_TAG = WebServerImage.class.getSimpleName();

    private File mFile = null;
    private String mMimeType;
    private DataTransferListener curDataTransferListener;

    public WebServerImage(Context context) {
        super(0/*HttpPort.BASE_PORT*/, context);
    }

    @Override
    public void start() throws IOException {
        resetPort();
        super.start();
    }

    public void setFile(File file, String mimeType) {
        mFile = file;
        mMimeType = mimeType;
    }

    @Override
    public String getRemoteUrl() {
        String name = mFile.getName();
        return super.getRemoteUrl() + "/" + getValidLink(name) + name.substring(name.lastIndexOf('.'));
    }

    @Override
    protected void setup() {

    }

    @Override
    protected Response serve(IHTTPSession session) {
        return super.serve(session);
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> params, Map<String, String> files) {
        SmartLog.d(TAG, "servingFile: " + mFile.getAbsolutePath());
        String range = null;
        SmartLog.d(TAG, "Request headers:");
        boolean dlna_info = false;
        for (String key : headers.keySet()) {
            SmartLog.d(TAG, "  " + key + ":" + headers.get(key));
            if ("range".equals(key)) {
                range = headers.get(key);
            }
            if ("getcontentfeatures.dlna.org".equals(key)) {
                // samsung platform doest not work without it
                dlna_info = true;
            }
        }
        try {
            if (range == null) {
                if (method == Method.HEAD && dlna_info) {
                    Response response = getFullResponse(mMimeType);
                    final long fileLength = mFile.length();
                    response.addHeader("Content-Length", fileLength + "");
                    response.addHeader("Content-Type", mMimeType);

                    String protocolInfo = "http-get:*:" + mMimeType + ":*";
                    if (getDevice() instanceof DlnaDevice) {
                        Collection<DeviceService> services = ((DlnaDevice) getDevice()).getConnectableDevice().getServices();
                        DLNAService dlnaService = null;
                        for (DeviceService service : services) {
                            if (service instanceof DLNAService) {
                                dlnaService = (DLNAService) service;
                            }
                        }
                        if (dlnaService != null) {
                            String serviceProtocolInfo = dlnaService.getProtocolInfo(mMimeType);
                            String[] split = serviceProtocolInfo.split(":");
                            protocolInfo = split[split.length - 1];
                        }
                    }
                    SmartLog.d(TAG, "Protocol info: " + protocolInfo);

                    response.addHeader("contentFeatures.dlna.org", protocolInfo);
//                    response.addHeader("contentFeatures.dlna.org", "DLNA.ORG_PN=PNG_LRG;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000");
                    response.addHeader("transfermode.dlna.org", "Streaming");
                    response.addHeader("Connection", "close");
                    response.addHeader("Cache-Control", "no-cache");
                    response.addHeader("Accept-Ranges", "bytes");

                    return response;
                }

                return getFullResponse(mMimeType);
            } else {
                return getPartialResponse(mMimeType, range);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        SmartLog.e(LOG_TAG, "File was not found! ");

        return Response.newFixedLengthResponse(Status.NOT_FOUND, mMimeType, "File not found");
    }

    private Response getFullResponse(String mimeType) throws FileNotFoundException {
        SmartLog.d(LOG_TAG, "getFullResponse ");

        final long startBytes = 0;
        final long fileLength = mFile.length();

        serverConnection.update(startBytes, fileLength);

        curDataTransferListener = new DataTransferListener() {
            @Override
            public void onDataTransfer(long bytesPushed, long bytesTotal) {
                if (curDataTransferListener == this) {
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

        FileInputStream fileInputStream = new FileInputStream(mFile);
        Response response = Response.newChunkedResponse(Status.OK, mimeType, fileInputStream);
        response.setDataTransferListener(curDataTransferListener);
        return response;
    }

    private Response getPartialResponse(String mimeType, String rangeHeader) throws IOException {

        String rangeValue = rangeHeader.trim().substring("bytes=".length());
        final long fileLength = mFile.length();
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
        if (end - start > BUFFER_SIZE) {
            end = start + BUFFER_SIZE;
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
                    if (curDataTransferListener == this) {
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

            FileInputStream fileInputStream = new FileInputStream(mFile);
            fileInputStream.skip(start);
            Response response = Response.newFixedLengthResponse(Status.PARTIAL_CONTENT, mimeType, fileInputStream, contentLength);
            response.setDataTransferListener(curDataTransferListener);

            response.addHeader("Content-Length", contentLength + "");
            response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.addHeader("Content-Type", mimeType);
            response.addHeader("Access-Control-Allow-Origin", "*");

            SmartLog.e(LOG_TAG, "getPartialResponse " + contentLength);

            return response;
        } else {
            SmartLog.e(LOG_TAG, "RANGE_NOT_SATISFIABLE ");
            return Response.newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, "html", rangeHeader);
        }
    }

}
