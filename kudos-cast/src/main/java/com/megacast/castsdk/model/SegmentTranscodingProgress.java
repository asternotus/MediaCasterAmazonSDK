package com.megacast.castsdk.model;

import java.io.File;

/**
 * Created by Dmitry on 19.04.17.
 */

public interface SegmentTranscodingProgress extends TranscodingProgress {

    File getSegmentFile(int index);

    int getSegmentsCount();

}
