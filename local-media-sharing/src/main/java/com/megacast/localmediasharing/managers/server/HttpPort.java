package com.megacast.localmediasharing.managers.server;

public interface HttpPort {

    public static int BASE_PORT = 1300;
    public static final int PORT_VIDEO_SUBTITLES = BASE_PORT + 1000;
    public final static int NON_TRANSCODED_MEDIA_PORT = PORT_VIDEO_SUBTITLES + 2000;
    public final static int TRANSCODED_MEDIA_PORT = NON_TRANSCODED_MEDIA_PORT + 3000;
    public final static int PORT_PHOTO = TRANSCODED_MEDIA_PORT + 4000;
    public final static int PORT_SMB = PORT_PHOTO + 5000;
}
