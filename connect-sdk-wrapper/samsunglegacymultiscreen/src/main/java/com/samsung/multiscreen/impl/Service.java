//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.impl;

import com.samsung.multiscreen.impl.RunnableScheduler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Service {
    private static Service instance = null;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private RunnableScheduler runnableScheduler = new RunnableScheduler(2);

    protected Service() {
    }

    public static synchronized Service getInstance() {
        if(instance == null) {
            instance = new Service();
        }

        return instance;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public RunnableScheduler getRunnableScheduler() {
        return this.runnableScheduler;
    }
}
