package io.multipaper.shreddedpaper.threading;

import net.minecraft.server.level.ServerLevel;
import io.multipaper.shreddedpaper.region.RegionPos;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ShreddedPaperRegionScheduler {

    private final ShreddedPaperRegionLocker locker = new ShreddedPaperRegionLocker();

    public static CompletableFuture<Void> schedule(ServerLevel level, RegionPos regionPos, Runnable runnable) {
        return level.chunkScheduler.schedule(regionPos, runnable);
    }

    /**
     * Schedule a task to run on the given region's thread.
     */
    public CompletableFuture<Void> schedule(RegionPos regionPos, Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        ShreddedPaperTickThread.getExecutor().execute(() -> {
            run(regionPos, runnable, future);
        });

        return future;
    }

    /**
     * Avoid using this often. Locking massive parts of the world can take time and will freeze these regions during the process.
     */
    public CompletableFuture<Void> scheduleOnMany(Runnable runnable, RegionPos... posArray) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        ShreddedPaperTickThread.getExecutor().execute(() -> {
            runOnMany(sortPredictably(posArray), runnable, future);
        });

        return future;
    }

    public static CompletableFuture<Void> scheduleAcrossLevels(ServerLevel level1, RegionPos regionPos1, ServerLevel level2, RegionPos regionPos2, Runnable runnable) {
        if (level1 == level2) {
            // We don't sort the regionPos in this method because we assume they're a different level, use the method that does sort them instead
            return level1.chunkScheduler.scheduleOnMany(runnable, regionPos1, regionPos2);
        }

        ServerLevel finalLevel1;
        RegionPos finalRegionPos1;
        ServerLevel finalLevel2;
        RegionPos finalRegionPos2;

        // Sort predictably to avoid deadlocks
        if (compare(level1, level2) > 0) {
            finalLevel1 = level2;
            finalRegionPos1 = regionPos2;
            finalLevel2 = level1;
            finalRegionPos2 = regionPos1;
        } else {
            finalLevel1 = level1;
            finalRegionPos1 = regionPos1;
            finalLevel2 = level2;
            finalRegionPos2 = regionPos2;
        }

        CompletableFuture<Void> future = new CompletableFuture<>();

        ShreddedPaperTickThread.getExecutor().execute(() -> {
            runAcrossLevels(finalLevel1, finalRegionPos1, finalLevel2, finalRegionPos2, runnable, future);
        });

        return future;
    }

    /**
     * Sort the region positions in a predictable order to avoid deadlocks.
     */
    private RegionPos[] sortPredictably(RegionPos[] posArray) {
        // This should only be used on very small arrays, usually just 2 elements, so this simple O(n^2) sort is fine
        for (int i = 0; i < posArray.length; i++) {
            for (int j = i + 1; j < posArray.length; j++) {
                if (compare(posArray[i], posArray[j]) > 0) {
                    RegionPos temp = posArray[i];
                    posArray[i] = posArray[j];
                    posArray[j] = temp;
                }
            }
        }
        return posArray;
    }

    private static int compare(RegionPos a, RegionPos b) {
        int x = Integer.compare(a.x, b.x);
        if (x != 0) {
            return x;
        }
        return Integer.compare(a.z, b.z);
    }

    private static int compare(ServerLevel a, ServerLevel b) {
        return a.uuid.compareTo(b.uuid);
    }

    private void run(RegionPos regionPos, Runnable runnable, CompletableFuture<Void> future) {
        ShreddedPaperRegionLocker.RegionLock lock = null;
        try {
            lock = locker.tryTakeLockNow(regionPos);
            if (lock == null) {
                // Wait for unlock, then retry
                locker.onUnlock(regionPos, () -> CompletableFuture.runAsync(() -> run(regionPos, runnable, future), ShreddedPaperTickThread.getExecutor()));
                return;
            }

            try {
                runnable.run();
            } finally {
                lock.unlock();
            }

            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private void runOnMany(RegionPos[] regionPosArray, Runnable runnable, CompletableFuture<Void> future) {
        ShreddedPaperRegionLocker.RegionLock[] locks = new ShreddedPaperRegionLocker.RegionLock[regionPosArray.length];
        try {
            try {
                for (int i = 0; i < regionPosArray.length; i++) {
                    locks[i] = locker.tryTakeLockNow(regionPosArray[i]);
                    if (locks[i] == null) {
                        locker.onUnlock(regionPosArray[i], () -> CompletableFuture.runAsync(() -> runOnMany(regionPosArray, runnable, future), ShreddedPaperTickThread.getExecutor()));
                        return;
                    }
                }

                // All locks acquired, run the task
                runnable.run();
            } finally {
                for (int i = locks.length - 1; i >= 0; i--) {
                    if (locks[i] != null) locks[i].unlock();
                }
            }

            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private static void runAcrossLevels(ServerLevel level1, RegionPos regionPos1, ServerLevel level2, RegionPos regionPos2, Runnable runnable, CompletableFuture<Void> future) {
        ShreddedPaperRegionLocker.RegionLock lock1 = null;
        ShreddedPaperRegionLocker.RegionLock lock2 = null;
        try {
            try {
                lock1 = level1.chunkScheduler.locker.tryTakeLockNow(regionPos1);
                lock2 = lock1 == null ? null : level2.chunkScheduler.locker.tryTakeLockNow(regionPos2);

                if (lock2 == null) {
                    Supplier<CompletableFuture<Void>> unlockCallback = () -> CompletableFuture.runAsync(() -> runAcrossLevels(level1, regionPos1, level2, regionPos2, runnable, future), ShreddedPaperTickThread.getExecutor());
                    if (lock1 == null) level1.chunkScheduler.locker.onUnlock(regionPos1, unlockCallback);
                    else level2.chunkScheduler.locker.onUnlock(regionPos2, unlockCallback);
                    return;
                }

                // Both locks acquired, run the task
                runnable.run();
            } finally {
                if (lock2 != null) lock2.unlock();
                if (lock1 != null) lock1.unlock();
            }

            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    public ShreddedPaperRegionLocker getRegionLocker() {
        return locker;
    }

}
