//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.application;

public class ApplicationError {
    private long code;
    private String message;

    public ApplicationError() {
        this.code = -1L;
        this.message = "error";
    }

    public ApplicationError(String message) {
        this(-1L, message);
    }

    public ApplicationError(long code, String message) {
        this.code = -1L;
        this.message = "error";
        this.code = code;
        this.message = message;
    }

    public String toString() {
        String s = "[ApplicationError]";
        s = s + " code: " + this.code;
        s = s + ", message: " + this.message;
        return s;
    }

    public long getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public static ApplicationError createWithException(Exception e) {
        return new ApplicationError(e.getLocalizedMessage());
    }
}
