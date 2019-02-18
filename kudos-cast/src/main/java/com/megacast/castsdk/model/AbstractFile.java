package com.megacast.castsdk.model;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Dmitry on 25.07.17.
 */

public interface AbstractFile {

    String getName();

    InputStream getInputSteam() throws IOException;

    long length() throws IOException;
}
