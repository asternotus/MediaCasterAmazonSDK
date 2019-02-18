//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix.util;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RunUtil {
    private static Handler handler;
    private static Executor executor = Executors.newFixedThreadPool(10);

    public RunUtil() {
    }

    public static void runOnUI(Runnable runnable) {
        if(handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }

        handler.post(runnable);
    }

    public static void runOnUiDelayed(final Runnable runnable, long delayTime) {
        if(handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }

        handler.postDelayed(new Runnable() {
            public void run() {
                RunUtil.runOnUI(runnable);
            }
        }, delayTime);
    }

    public static void runInBackground(Runnable runnable) {
        if(isMain()) {
            executor.execute(runnable);
        } else {
            runnable.run();
        }

    }

    private static boolean isMain() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
