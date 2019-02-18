package com.megacast.localmediasharing.managers.server;

import android.content.Context;
import android.text.TextUtils;

import com.mega.cast.utils.log.SmartLog;
import com.megacast.castsdk.model.RemoteDataReceiverController;
import com.megacast.castsdk.model.constants.MediaTypes;

import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class WebServerLocalPlaylist extends CastServer {

    public static final String LOG_TAG = NanoHTTPD.class.getSimpleName() + WebServerLocalPlaylist.class.getSimpleName();

    private static final long MIN_AVAILABLE_SPACE_THRESHOLD_BYTES = 10 * 1024 * 1024;

    private static final String playlistMimeType = MediaTypes.MIME_TYPE_PLAYLIST_X_MPEGURL;

    private static final String PLAYLIST_EXTENSION = MediaTypes.EXTENSION_M3U8;
    private static final String STREAM_DEFAULT_NAME = "stream." + PLAYLIST_EXTENSION;

    private static int PLAYLIST_BUFFER_SIZE = 1;
//    private static int PLAYLIST_BUFFER_SIZE = 5;

    private static final long AUTO_RESUME_DELAY_MILLISEC = 1000;

    private static final int RESUME_MIN_BUFFER = 1;
    private static final int AUTO_DELETION_SEGMENTS_BUFFER = 4;

    private PlaylistFile playlistFile = null;
    private File videoFile = null;

    private String fileNameWithoutExtension = null;

    private RemoteDataReceiverController remoteDataReceiverController;

    private boolean autoPause;

    private int currentItem = 0;

    private Timer autoResumeTimer;
    private long videoDuration;
    private File playlist;

    private boolean removePlaylistItemsOnServe = false;

    private List<String> servedPaths = new ArrayList<>();

    public WebServerLocalPlaylist(Context context) {
        super(0/*HttpPort.TRANSCODED_MEDIA_PORT*/, context);

        this.autoPause = false;

        this.autoResumeTimer = new Timer();
    }

    @Override
    public void start() throws IOException {
        resetPort();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        servedPaths = new ArrayList<>();
    }

    public void setRemovePlaylistItemsOnServe(boolean removePlaylistItemsOnServe) {
        this.removePlaylistItemsOnServe = removePlaylistItemsOnServe;
    }

    public void prepare(File videoFile, File playlistFile, long videoDuration, RemoteDataReceiverController remoteDataReceiverController) {
        this.videoFile = videoFile;
        this.remoteDataReceiverController = remoteDataReceiverController;
        this.playlist = playlistFile;
        this.videoDuration = videoDuration;

        resetAutoResumeTimer();
    }

    @Override
    protected void setup() {
        try {
            SmartLog.d(LOG_TAG, "updating playlist...");
            this.playlistFile = updatePlayList(playlist, videoFile, videoDuration);

            SmartLog.d(LOG_TAG, "playlist updated! playlistFile.getItemsNum " + playlistFile.getItemsNum());
        } catch (IOException ex) {
            ex.printStackTrace();
            SmartLog.e(LOG_TAG, "Could not create playlist file " + ex.getMessage());
            onBoundException();
        }
    }

    @Override
    public String getRemoteUrl() {
        String name = playlistFile.getFile().getName();
        return super.getRemoteUrl() + "/" + getValidLink(name) + name.substring(name.lastIndexOf('.'));
    }

    @Override
    @SuppressWarnings("deprecation")
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> params, Map<String, String> files) {
        FileInputStream fileInputStream = null;

        File file = null;
        try {
            SmartLog.d(LOG_TAG, String.format("----- serve: %s -----", uri));

            if (uri.length() > 2 && !uri.equals("/" + playlistFile.getFile().getName())) {
                SmartLog.d(LOG_TAG, "serving playlist item...");

                file = new File(playlistFile.getFile().getParentFile(), uri.substring(1));


                if (!file.exists()) {
                    SmartLog.e(LOG_TAG, "item file doesn't exists! " + file.getAbsolutePath());

                    return Response.newFixedLengthResponse(Status.NOT_FOUND, playlistMimeType, "File not found");
                }

                String name = file.getName();
                fileNameWithoutExtension = name.replaceFirst("[.][^.]+$", "");
                currentItem = lastInteger(fileNameWithoutExtension);

                SmartLog.d(LOG_TAG, String.format("serving %d/%d", currentItem, playlistFile.getItemsNum()));

                //sometimes Android returns invalid file duration and we just don't have a last item
                //in this case we will not pause playback and just return "not found" on the last item
                if ((currentItem + 1) < playlistFile.getItemsNum()) {
                    final boolean hasBuffer = hasBuffer(currentItem, PLAYLIST_BUFFER_SIZE);
                    if (!hasBuffer) {
                        SmartLog.e(LOG_TAG, "cannot serve item without buffer!");
                        if (autoPause) {
                            pauseDataReceiving();
                        } else {
                            //wait for the next item
                            SmartLog.d(LOG_TAG, "wait for a next item ");
                            try {
                                Thread.sleep(AUTO_RESUME_DELAY_MILLISEC);
                            } catch (InterruptedException e) {
                                SmartLog.e(LOG_TAG, "waiting for the next item was interrupted ");
                                e.printStackTrace();
                            }
                            return serve(uri, method, headers, params, files);
                        }
                    }
                }

                if (removePlaylistItemsOnServe) {
                    if (!servedPaths.isEmpty() && servedPaths.size() >= AUTO_DELETION_SEGMENTS_BUFFER) {
                        String pathToDelete = servedPaths.get(0);
                        if (!TextUtils.isEmpty(pathToDelete)) {
                            final File firstSegment = new File(pathToDelete);
                            if (firstSegment.exists()) {
                                deleteSegment(firstSegment)
                                        .subscribeOn(Schedulers.io())
                                        .subscribe(new Action1<Boolean>() {
                                            @Override
                                            public void call(Boolean aBoolean) {
                                                if (aBoolean) {
                                                    SmartLog.d(LOG_TAG, "deleted previous fragment " + firstSegment.getName());
                                                }
                                            }
                                        });
                            }
                        }
                        servedPaths.remove(pathToDelete);
                    }
                }


                servedPaths.add(file.getAbsolutePath());

            } else {

                SmartLog.d(LOG_TAG, "serving playlist file...");
                file = playlistFile.getFile();

                if (!file.exists()) {
                    SmartLog.e(LOG_TAG, "playlist file doesn't exists!");

                    return Response.newFixedLengthResponse(Status.NOT_FOUND, playlistMimeType, "File not found");
                }

            }

            SmartLog.d(LOG_TAG, "servingFile size: " + file.length());
            SmartLog.d(LOG_TAG, "servingFile path: " + file.getAbsolutePath());

            fileInputStream = new FileInputStream(file);
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

    private Observable<Boolean> deleteSegment(final File segment) {
        return Observable.create(
                new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(final Subscriber<? super Boolean> sub) {
                        sub.onNext(segment.delete());
                        sub.onCompleted();
                    }
                }
        );

    }

    private void pauseDataReceiving() {
        SmartLog.e(LOG_TAG, "Not enough buffered files! Pause data receiving immediately!");
        remoteDataReceiverController.pause()
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        SmartLog.d(LOG_TAG, "Data receiving paused!");
                        if (autoPause) {
                            resumePlaybackOnBufferFilled();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        SmartLog.e(LOG_TAG, "Can not pause data receiving! " + e.getMessage());
                    }

                    @Override
                    public void onNext(Void aVoid) {
                    }
                });
    }

    private void resumeDataReceiving() {
        SmartLog.e(LOG_TAG, "Resume playback automatically");
        remoteDataReceiverController.resume()
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        SmartLog.d(LOG_TAG, "Data receiving resumed!");
                    }

                    @Override
                    public void onError(Throwable e) {
                        SmartLog.e(LOG_TAG, "Can not resume data receiving! " + e.getMessage());
                    }

                    @Override
                    public void onNext(Void aVoid) {
                    }
                });
    }

    private void resetAutoResumeTimer() {
        autoResumeTimer.cancel();
        autoResumeTimer = new Timer();
    }

    private void resumePlaybackOnBufferFilled() {
        autoResumeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (autoPause) {
                    if (hasBuffer(currentItem, RESUME_MIN_BUFFER)) {
                        resumeDataReceiving();
                    } else {
                        resumePlaybackOnBufferFilled();
                    }
                }
            }
        }, AUTO_RESUME_DELAY_MILLISEC);
    }

    private int lastInteger(String s) {
        int i = s.length();
        while (i > 0 && Character.isDigit(s.charAt(i - 1))) {
            i--;
        }
        return Integer.parseInt(s.substring(i));
    }

    private boolean hasBuffer(int currentNum, int bufferSize) {
        for (int i = currentNum + 1; i <= (currentNum + bufferSize) && i <= playlistFile.getItemsNum(); i++) {
            File fileItem = playlistFile.getItemFileByIndex(i);
            if (!fileItem.exists()) {
                SmartLog.e(LOG_TAG, "Doesn't exists! " + fileItem.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    private PlaylistFile updatePlayList(File playlistFile, File videoFile, long videoDuration) throws IOException {
        SmartLog.d(LOG_TAG, String.format("updatePlayList" +
                        "\n playlist file: %s" +
                        "\n video file: %s" +
                        "\n video duration: %d",
                playlistFile.getAbsolutePath(),
                videoFile != null ? videoFile.getName() : null,
                videoDuration));

        if (videoDuration <= 0) {
            throw new IOException("Cannot get video duration!");
        }

        String itemUrl = super.getRemoteUrl(); //get url without name

        final String serverFileName = videoFile != null ? fileNameWithExtension(videoFile.getName()) : STREAM_DEFAULT_NAME;

        File serverFile = new File(playlistFile.getParentFile(), serverFileName);

        SmartLog.d(LOG_TAG, "Server file: " + serverFile.getAbsolutePath());

        if (!serverFile.exists()) {
            boolean created = serverFile.createNewFile();
            if (!created) {
                throw new IOException("Could not create playlist file! " + serverFile.getAbsolutePath());
            }
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(playlistFile)));

        Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(serverFile), "UTF8"));

        boolean smallFile = false;

        String myExtInf = null;
        String fileItemName = null;
        double firstSegment = 0;

        String line = null;

        int extinfCounter = 0;
        int playlistItemCounter = 0;
        while ((line = reader.readLine()) != null) {
            SmartLog.d(LOG_TAG, " " + line);
            if (line.startsWith("#")) {
                if (line.contains("EXTINF")) {
                    SmartLog.d(LOG_TAG, "found exInfo " + line);
                    extinfCounter++;
                    if (extinfCounter == 1) {
                        String time = line.substring(line.lastIndexOf(":") + 1, line.lastIndexOf(",") - 1);
                        firstSegment = Double.parseDouble(time) * 1000;
                    } else if (extinfCounter == 2) {
                        myExtInf = line;
                    }
                }
                if (line.contains("EXT-X-ENDLIST")) {
                    smallFile = true;
                }
                writer.append(line);
            } else if (!line.equals("\n")) {
                String currentFileItemName = line;
                if (fileItemName == null) {
                    fileItemName = currentFileItemName.substring(0, currentFileItemName.lastIndexOf('_'));
                    SmartLog.d(LOG_TAG, "found fileItem name! " + fileItemName);
                }
                writer.append(itemUrl + "/" + fileItemName + "_" + playlistItemCounter + ".ts");
                playlistItemCounter++;
            }
            writer.append("\n");
            if (playlistItemCounter == 2) {
                break;
            }
        }
        reader.close();

        if (smallFile) {
            //playlist contains less than 2 items
            writer.close();
            return new PlaylistFile(serverFile, fileItemName, playlistItemCounter);
        }

        if (myExtInf == null || fileItemName == null) {
            //file has no _1.ts
            SmartLog.e(LOG_TAG, "failed to retrieve ExtInfo! ");
            writer.close();
            throw new IOException("failed to retrieve ExtInfo! ");
        }

        String time = myExtInf.substring(myExtInf.lastIndexOf(":") + 1, myExtInf.lastIndexOf(",") - 1);
        double segmentTime = Double.parseDouble(time) * 1000;

        long cycles = 1;

        double durationWithoutFirstSegment = (double) videoDuration - firstSegment;

        if (durationWithoutFirstSegment > segmentTime) {
            long lastCycle = (long) (durationWithoutFirstSegment % segmentTime);

            long remainingCycles = (long) ((durationWithoutFirstSegment - lastCycle) / segmentTime);


            SmartLog.d(LOG_TAG, String.format("video duration: %d" +
                    "\n first segment: %f" +
                    "\n segmentTime: %f" +
                    "\n remainingCycles: %d" +
                    "\n lastCycle: %d", videoDuration, firstSegment, segmentTime, remainingCycles, lastCycle));

            for (int i = 2; i < remainingCycles + 1; i++) {
                writer.append(myExtInf);
                writer.append("\n");
                writer.append(itemUrl + "/" + fileItemName + "_" + i + ".ts");
                writer.append("\n");
            }


            cycles += remainingCycles;
            lastCycle = lastCycle / 1000;

            writer.append("#EXTINF:" + lastCycle + ".000000,");
            writer.append("\n");
            writer.append(itemUrl + "/" + fileItemName + "_" + cycles + ".ts");
            writer.append("\n");
        }

        writer.append("#EXT-X-ENDLIST");
        writer.append("\n");
        writer.close();

        return new PlaylistFile(serverFile, fileItemName, cycles);
    }

    private String fileNameWithExtension(String loadedFileName) {
        return getValidLink(loadedFileName) + "." + PLAYLIST_EXTENSION;
    }

    public void setAutoPause(boolean autoPause) {
        this.autoPause = autoPause;
    }

    private static String getFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public class PlaylistFile {
        private File file;
        private String itemName;
        private long itemsNum;

        public PlaylistFile(File file, String itemName, long itemsNum) {
            this.file = file;
            this.itemName = itemName;
            this.itemsNum = itemsNum;
        }

        public File getItemFileByIndex(int index) {
            return new File(file.getParentFile(), itemName + "_" + index + ".ts");
        }

        public File getFile() {
            return file;
        }

        public long getItemsNum() {
            return itemsNum;
        }
    }
}
