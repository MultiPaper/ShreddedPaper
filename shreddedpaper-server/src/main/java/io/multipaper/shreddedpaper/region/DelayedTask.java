package io.multipaper.shreddedpaper.region;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class DelayedTask implements Runnable {
    private static final Logger LOGGER = LogUtils.getClassLogger();

    private final Runnable task;
    private long delay;

    public DelayedTask(Runnable task, long delay) {
        this.task = task;
        this.delay = delay;
    }

    public boolean shouldRun() {
        return --delay <= 0;
    }

    public void run() {
        try {
            task.run();
        } catch (Throwable t) {
            LOGGER.error("Error when executing task", t);
        }
    }

}
