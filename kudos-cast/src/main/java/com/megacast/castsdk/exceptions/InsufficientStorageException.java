package com.megacast.castsdk.exceptions;

/**
 * Created by Dmitry on 27.02.17.
 */

public class InsufficientStorageException extends Throwable {

    private long requiredSpace;
    private long availableSpace;

    public InsufficientStorageException() {
    }

    public InsufficientStorageException(String detailMessage) {
        super(detailMessage);
    }

    public InsufficientStorageException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }

    public InsufficientStorageException(Throwable cause) {
        super(cause);
    }

    public InsufficientStorageException(String detailMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(detailMessage, cause, enableSuppression, writableStackTrace);
    }

    public long getRequiredSpace() {
        return requiredSpace;
    }

    public void setRequiredSpace(long requiredSpace) {
        this.requiredSpace = requiredSpace;
    }

    public long getAvailableSpace() {
        return availableSpace;
    }

    public void setAvailableSpace(long availableSpace) {
        this.availableSpace = availableSpace;
    }
}
