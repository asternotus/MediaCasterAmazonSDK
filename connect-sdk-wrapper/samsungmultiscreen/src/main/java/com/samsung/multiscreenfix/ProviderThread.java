//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

abstract class ProviderThread extends Thread {
    ProviderThread(Runnable runnable) {
        super(runnable);
    }

    abstract void terminate();

    boolean isTerminate() {
        return false;
    }
}
