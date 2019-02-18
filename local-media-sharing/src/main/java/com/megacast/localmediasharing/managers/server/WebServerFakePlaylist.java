package com.megacast.localmediasharing.managers.server;

import android.content.Context;
import android.os.Environment;

import com.mega.cast.utils.log.SmartLog;

import com.megacast.castsdk.model.constants.MediaTypes;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

/**
 * Created by Dmitry on 23.02.17.
 */

public class WebServerFakePlaylist extends CastServer {

    private static final String LOG_TAG = NanoHTTPD.class.getSimpleName() + WebServerFakePlaylist.class.getSimpleName();
    public static final String playlistMimeType = "application/x-mpegURL";
    public static final String PLAYLIST_EXTENSION = MediaTypes.EXTENSION_M3U8;

    private static final long VIDEO_FRAGMENT_DURATION_MILISEC = 7 * 1000;

    private File playlistFile = null;
    private File videoFile;
    private long videoDuration;
    private Context context;

    public WebServerFakePlaylist(Context context) {
        super(0, context);
    }

    @Override
    public void start() throws IOException {
        resetPort();
        super.start();
    }

    public void prepare(File videoFile, long videoDuration) {
        this.videoFile = videoFile;
        this.videoDuration = videoDuration;
    }

    @Override
    protected void setup() {
        try {
            SmartLog.d(LOG_TAG, "updating playlist...");
            this.playlistFile = createPlayList(videoFile, videoDuration);
            SmartLog.d(LOG_TAG, "playlist updated! ");
        } catch (IOException ex) {
            ex.printStackTrace();
            SmartLog.e(LOG_TAG, "Could not create playlist file " + ex.getMessage());
            onBoundException();
        }
    }

    @Override
    public String getRemoteUrl() {
        return super.getRemoteUrl() + "/" + playlistFile.getName();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> params, Map<String, String> files) {
        FileInputStream fileInputStream = null;
        File file = null;

        try {
            SmartLog.d(LOG_TAG, String.format("----- serve: %s -----", uri));

            if (uri.length() > 2 && !uri.equals("/" + playlistFile.getName())) {
                SmartLog.d(LOG_TAG, "serving playlist item...");

                file = new File(playlistFile.getParentFile(), uri.substring(1));

                if (!file.exists()) {
                    SmartLog.e(LOG_TAG, "item file doesn't exists! " + file.getAbsolutePath());
                    return Response.newFixedLengthResponse(Status.NOT_FOUND, playlistMimeType, "File not found");
                }

                String name = file.getName();
                String fileNameWithoutExtension = name.replaceFirst("[.][^.]+$", "");
                int currentItemNum = lastInteger(fileNameWithoutExtension);

                SmartLog.d(LOG_TAG, "current item: " + currentItemNum);
            } else {
                SmartLog.d(LOG_TAG, "serving playlist file...");
                file = playlistFile;
                if (!file.exists()) {
                    SmartLog.e(LOG_TAG, "playlist file doesn't exists!");

                    return Response.newFixedLengthResponse(Status.NOT_FOUND, playlistMimeType, "File not found");
                }
            }

            fileInputStream = new FileInputStream(file);

            SmartLog.d(LOG_TAG, "servingFile size: " + file.length());
            SmartLog.d(LOG_TAG, "servingFile path: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            SmartLog.e(LOG_TAG, "Exception serving file: " + file.toString() + "e = " + e.getLocalizedMessage());

            return Response.newFixedLengthResponse(Status.NOT_FOUND, playlistMimeType, "File not found");
        }

        serverConnection.setAvailable(false);

        Response response = Response.newChunkedResponse(Status.OK, playlistMimeType, fileInputStream);

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Content-Type", playlistMimeType);
        return response;
    }

    private File createPlayList(File videoFile, long videoDuration) throws IOException {

        SmartLog.d(LOG_TAG, String.format("create playlist\n media file: %s\n video duration: %d", videoFile, videoDuration));

        final long videoFileLength = videoFile.length();
        SmartLog.d(LOG_TAG, "video file videoFileLength " + videoFileLength);

        long videoFragmentsCount = videoDuration / VIDEO_FRAGMENT_DURATION_MILISEC;
        long lastVideoPartDuration = videoDuration % VIDEO_FRAGMENT_DURATION_MILISEC;

        float lastVideoPartProportion = (float) lastVideoPartDuration / videoDuration;

        long videoFragmentLength = Math.round(videoFileLength * (1 - lastVideoPartProportion)) / videoFragmentsCount;
        long lastVideoFragmentLength = videoFileLength - Math.round(videoFileLength * (1 - lastVideoPartProportion));

        SmartLog.d(LOG_TAG, "videoPartCount " + videoFragmentsCount);
        SmartLog.d(LOG_TAG, "lastVideoPartDuration " + lastVideoPartDuration);

        SmartLog.d(LOG_TAG, "videoFragmentLength " + videoFragmentLength);
        SmartLog.d(LOG_TAG, "lastVideoFragmentLength " + lastVideoFragmentLength);

        File serverFile = new File(getTempFileDir(), fileNameWithExtension(videoFile.getName()));

        SmartLog.d(LOG_TAG, "Server file: " + serverFile.getAbsolutePath());

        if (!serverFile.exists()) {
            boolean created = serverFile.createNewFile();
            if (!created) {
                throw new IOException("Could not create playlist file! " + serverFile.getAbsolutePath());
            }
        }

        FileInputStream videoReader = new FileInputStream(videoFile);

        Writer playlistWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(serverFile), "UTF8"));

        playlistWriter.append("#EXTM3U");
        playlistWriter.append("\n");
        playlistWriter.append("#EXT-X-VERSION:3");
        playlistWriter.append("\n");
        playlistWriter.append("#EXT-X-MEDIA-SEQUENCE:0");
        playlistWriter.append("\n");
        playlistWriter.append("#EXT-X-ALLOW-CACHE:YES");
        playlistWriter.append("\n");
        playlistWriter.append("#EXT-X-TARGETDURATION:" + (VIDEO_FRAGMENT_DURATION_MILISEC / 1000));
        playlistWriter.append("\n");

        SmartLog.d(LOG_TAG, "videoFile.getName() " + videoFile.getName());

        long pendingTotal = 0;
        long readTotal = 0;

        for (int i = 0; i < videoFragmentsCount; i++) {
            double fragmentDuration = (double) VIDEO_FRAGMENT_DURATION_MILISEC / 1000;
            String[] nameParts = videoFile.getName().split("\\.");
            String fragmentName = nameParts[0] + "_" + i + "." + nameParts[1];

            playlistWriter.append("#EXTINF:" + fragmentDuration + ",");
            playlistWriter.append("\n");
            playlistWriter.append(fragmentName);
            playlistWriter.append("\n");

            File fragmentFile = new File(getTempFileDir(), fragmentName);
            if (!fragmentFile.exists()) {
                boolean created = fragmentFile.createNewFile();
                if (!created) {
                    throw new IOException("Could not create fragment file! " + fragmentFile.getAbsolutePath());
                }
            }

            SmartLog.i(LOG_TAG, "writing... " + fragmentFile.getAbsolutePath());

            FileOutputStream fragmentFileWriter = new FileOutputStream(fragmentFile);

            byte[] buffer = new byte[16 * 1024];
            long curVideoFragmentLength = videoFragmentLength;

            while (curVideoFragmentLength > 0) {
                int pending = Math.min(buffer.length, (int) curVideoFragmentLength);
                int read = videoReader.read(buffer, 0, pending);

                pendingTotal += pending;
                readTotal += read;

                SmartLog.d(LOG_TAG, String.format("write %d from %d", read, pending));
                SmartLog.d(LOG_TAG, String.format("pendingTotal %f", ((float) pendingTotal / videoFileLength)));
                SmartLog.d(LOG_TAG, String.format("readTotal %f", ((float) readTotal / videoFileLength)));

                fragmentFileWriter.write(buffer, 0, read);
                curVideoFragmentLength -= pending;
            }
            fragmentFileWriter.close();
        }

        double fragmentDuration = (double) lastVideoPartDuration / 1000;
        String[] nameParts = videoFile.getName().split("\\.");

        String fragmentName = nameParts[0] + "_" + videoFragmentsCount + "." + nameParts[1];

        playlistWriter.append("#EXTINF:" + fragmentDuration + ",");
        playlistWriter.append("\n");
        playlistWriter.append(fragmentName);
        playlistWriter.append("\n");

        playlistWriter.append("#EXT-X-ENDLIST");
        playlistWriter.append("\n");
        playlistWriter.close();

        File fragmentFile = new File(getTempFileDir(), fragmentName);
        if (!fragmentFile.exists()) {
            boolean created = fragmentFile.createNewFile();
            if (!created) {
                throw new IOException("Could not create fragment file! " + fragmentFile.getAbsolutePath());
            }
        }

        SmartLog.i(LOG_TAG, "writing... " + fragmentFile.getAbsolutePath());

        FileOutputStream fragmentFileWriter = new FileOutputStream(fragmentFile);

        byte[] buffer = new byte[16 * 1024];
        long curVideoFragmentLength = lastVideoFragmentLength;

        while (curVideoFragmentLength > 0) {
            int pending = Math.min(buffer.length, (int) curVideoFragmentLength);
            int read = videoReader.read(buffer, 0, pending);

            pendingTotal += pending;
            readTotal += read;

            SmartLog.d(LOG_TAG, String.format("write %d from %d", read, pending));
            SmartLog.d(LOG_TAG, String.format("pendingTotal %f", ((float) pendingTotal / videoFileLength)));
            SmartLog.d(LOG_TAG, String.format("readTotal %f", ((float) readTotal / videoFileLength)));

            fragmentFileWriter.write(buffer, 0, read);
            curVideoFragmentLength -= pending;
        }
        fragmentFileWriter.close();

        videoReader.close();

        return serverFile;
    }

    private File getTempFileDir() {
        File filesDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filesDir = getContext().getExternalFilesDir(null);
            if (filesDir == null) {
                filesDir = Environment.getExternalStorageDirectory();
            }
            if (filesDir == null) {
                filesDir = getContext().getFilesDir();
            }
        } else
            filesDir = getContext().getFilesDir();

        return filesDir;
    }

    private String fileNameWithExtension(String loadedFileName) {
        return getValidLink(loadedFileName) + "." + PLAYLIST_EXTENSION;
    }

    private int lastInteger(String s) {
        int i = s.length();
        while (i > 0 && Character.isDigit(s.charAt(i - 1))) {
            i--;
        }
        return Integer.parseInt(s.substring(i));
    }

}
