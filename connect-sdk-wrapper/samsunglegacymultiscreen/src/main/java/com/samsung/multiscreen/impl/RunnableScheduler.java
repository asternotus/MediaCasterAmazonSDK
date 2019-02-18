//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.impl;

import com.samsung.multiscreen.impl.SchedulerKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RunnableScheduler {
    private final ScheduledExecutorService executorService;
    private final Map<SchedulerKey, Future<?>> scheduledFutures = new ConcurrentHashMap();

    public RunnableScheduler(int threadPoolSize) {
        this.executorService = Executors.newScheduledThreadPool(threadPoolSize);
    }

    public void cancel(SchedulerKey key) {
        Future future = (Future)this.scheduledFutures.remove(key);
        if(future != null) {
            future.cancel(true);
        }

    }

    public void schedule(final SchedulerKey key, final Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        ScheduledFuture future = this.executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    runnable.run();
                } finally {
                    RunnableScheduler.this.scheduledFutures.remove(key);
                }

            }
        }, initialDelay, delay, unit);
        this.scheduledFutures.put(key, future);
    }

    public void scheduleOnce(final SchedulerKey key, final Runnable runnable, long delay, TimeUnit unit) {
        ScheduledFuture future = this.executorService.schedule(new Runnable() {
            public void run() {
                try {
                    runnable.run();
                } finally {
                    RunnableScheduler.this.scheduledFutures.remove(key);
                }

            }
        }, delay, unit);
        this.scheduledFutures.put(key, future);
    }

    public void shutdown() {
        this.executorService.shutdownNow();
    }
}
