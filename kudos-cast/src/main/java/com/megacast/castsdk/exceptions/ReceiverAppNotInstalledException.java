package com.megacast.castsdk.exceptions;

/**
 * Created by Dmitry on 21.04.17.
 */

public class ReceiverAppNotInstalledException extends Throwable {

    public ReceiverAppNotInstalledException() {
    }

    public ReceiverAppNotInstalledException(String detailMessage) {
        super(detailMessage);
    }

    public ReceiverAppNotInstalledException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }

    public ReceiverAppNotInstalledException(Throwable cause) {
        super(cause);
    }

    public ReceiverAppNotInstalledException(String detailMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(detailMessage, cause, enableSuppression, writableStackTrace);
    }
}