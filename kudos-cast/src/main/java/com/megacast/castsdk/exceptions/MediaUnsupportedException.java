package com.megacast.castsdk.exceptions;

/**
 * Created by Dmitry on 01.02.17.
 */

public class MediaUnsupportedException extends Throwable {

    public MediaUnsupportedException() {
    }

    public MediaUnsupportedException(String detailMessage) {
        super(detailMessage);
    }

    public MediaUnsupportedException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }

    public MediaUnsupportedException(Throwable cause) {
        super(cause);
    }

    public MediaUnsupportedException(String detailMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(detailMessage, cause, enableSuppression, writableStackTrace);
    }
}
