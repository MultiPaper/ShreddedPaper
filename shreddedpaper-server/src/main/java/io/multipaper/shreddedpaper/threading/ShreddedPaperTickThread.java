package io.multipaper.shreddedpaper.threading;

import com.mojang.logging.LogUtils;
import io.papermc.paper.util.TickThread;
import org.slf4j.Logger;
import io.multipaper.shreddedpaper.config.ShreddedPaperConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ShreddedPaperTickThread extends TickThread {

    private static final Logger LOGGER = LogUtils.getClassLogger();

    public static final int THREAD_COUNT;

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    private static final ExecutorService executor;

    static {
        int threadCount = ShreddedPaperConfiguration.get().multithreading.threadCount;

        if (threadCount <= 0) {
            threadCount = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        }

        THREAD_COUNT = threadCount;

        executor = Executors.newFixedThreadPool(THREAD_COUNT, r -> new ShreddedPaperTickThread(r, "ShreddedPaperTickThread-%d"));

        LOGGER.info("Using {} threads", THREAD_COUNT);
    }

    public ShreddedPaperTickThread(Runnable run, String name) {
        super(run, String.format(name, ID_GENERATOR.incrementAndGet()));
    }

    public static boolean isShreddedPaperTickThread() {
        // Use this method to check if it's a shreddedpaper tick thread, to ensure future potential support for VirtualThreads
        return Thread.currentThread() instanceof ShreddedPaperTickThread;
    }

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void stopServer() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!executor.isTerminated()) {
            LOGGER.warn("Failed to stop tick threads after 5 seconds. Terminating...");
            executor.shutdownNow();
        }
    }
}
