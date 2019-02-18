package com.megacast.localmediasharing.managers.server;

/**
 * Created by Дима on 05.02.2018.
 */

public interface DataTransferListener {

    void onDataTransfer(long bytesPushed, long bytesTotal);

    void onDataTransferStarted();
}
