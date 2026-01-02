package io.multipaper.shreddedpaper.threading;

import ca.spottedleaf.moonrise.common.util.TickThread;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import io.multipaper.shreddedpaper.region.RegionPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

/**
 * Read lock:
 *  - Only one thread can hold a read lock at a time.
 *  - Many servers can hold a read lock at the same time.
 *  - Good for tasks such as sending data to clients and loading chunks.
 * Write lock:
 *  - An extension of the read lock.
 *  - Only one server can hold a write lock at a time.
 *  - Important for tasks that modify the region, such as modifying blocks or entities.
 */
public class ShreddedPaperRegionLocker {

    public static final int REGION_LOCK_RADIUS = 1;

    private final ConcurrentHashMap<RegionPos, LockedRegion> lockedRegions = new ConcurrentHashMap<>();
    private final StampedLock globalLock = new StampedLock();

    private final ThreadLocal<Set<RegionPos>> localLocks = ThreadLocal.withInitial(ObjectOpenHashSet::new);
    private final ThreadLocal<Set<RegionPos>> readOnlyLocks = ThreadLocal.withInitial(ObjectOpenHashSet::new);
    private final ThreadLocal<Set<RegionPos>> writeLocks = ThreadLocal.withInitial(ObjectOpenHashSet::new);
    private final ThreadLocal<Set<RegionPos>> unmodifiableLocalLocks = ThreadLocal.withInitial(() -> Collections.unmodifiableSet(localLocks.get()));

    /**
     * Checks if the current thread holds a read lock for the given region
     */
    public boolean hasLock(RegionPos regionPos) {
        return localLocks.get().contains(regionPos) || TickThread.canBypassTickThreadCheck();
    }

    /**
     * Checks if the current thread holds a write lock for the given region.
     * This check is usually unnecessary, but ensures that there will be no
     * syncing conflicts with other servers.
     */
    public boolean hasWriteLock(RegionPos regionPos) {
        return writeLocks.get().contains(regionPos) || TickThread.canBypassTickThreadCheck();
    }

    /**
     * Returns an unmodifiable view of the locked regions for the current thread.
     */
    public Set<RegionPos> getLockedRegions() {
        // Use an unmodifiable view to ensure the underlying set doesn't get accidentally modified
        return unmodifiableLocalLocks.get();
    }

    /**
     * Returns an unmodifiable view of all locked regions across all threads.
     */
    public Set<Map.Entry<RegionPos, LockedRegion>> getAllLockedRegions() {
        return Collections.unmodifiableSet(lockedRegions.entrySet());
    }

    public RegionLock lockRegion(RegionPos regionPos) {
        int tries = 0;
        RegionLock lock;
        while ((lock = this.tryTakeLockNow(regionPos)) == null) {
            LockSupport.parkNanos(Math.min(100_000, 1_000 * (++tries)));
        }
        return lock;
    }

    /**
     * Returns whether any regions are currently locked.
     */
    public boolean areAnyLocked() {
        return !lockedRegions.isEmpty();
    }

    /**
     * Lock the region and run the runnable. If the region is already locked, wait until it is unlocked.
     * Can be called recursively and will respect existing locks created by the same thread.
     */
    public <T> T lockRegion(RegionPos regionPos, Supplier<T> runnableWithReturnValue) {
        RegionLock lock = this.lockRegion(regionPos);
        try {
            return runnableWithReturnValue.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lock the region and run the runnable. If the region is already locked, wait until it is unlocked.
     * Can be called recursively and will respect existing locks created by the same thread.
     */
    public void lockRegion(RegionPos regionPos, Runnable runnable) {
        RegionLock lock = this.lockRegion(regionPos);
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Try to acquire the region lock immediately, if successful run the runnable.
     * If unsuccessful, return false and the runnable will not be run.
     * If the region is already locked, it will return unsuccessfully immediately instead of waiting to try to acquire the lock.
     * Can be called recursively and will respect existing locks created by the same thread.
     * This method will sync the lock with other servers.
     * @return true if the lock was acquired and the runnable was run, false if the runnable was not run
     */
    public boolean tryLockNow(RegionPos centerPos, Runnable ifSuccess) {
        RegionLock lock = this.tryTakeLockNow(centerPos);
        if (lock == null) {
            return false;
        }

        try {
            ifSuccess.run();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Try to acquire the region lock immediately, if successful run the runnable.
     * If unsuccessful, return false and the runnable will not be run.
     * If the region is already locked, it will return unsuccessfully immediately instead of waiting to try to acquire the lock.
     * Can be called recursively and will respect existing locks created by the same thread.
     * The specified region must not be modified within this lock. This method will not sync the lock with other servers.
     * return true if the lock was acquired and the runnable was run, false if the runnable was not run
     */
    public boolean tryReadOnlyLockNow(RegionPos centerPos, Runnable ifSuccess) {
        final RegionLock lock = this.tryTakeReadOnlyLockNow(centerPos);
        if (lock == null) {
            return false;
        }

        try {
            ifSuccess.run();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    public RegionLock tryTakeLockNow(RegionPos centerPos) {
        return internalTryTakeLockNow(centerPos, REGION_LOCK_RADIUS);
    }

    @Nullable
    public WriteRegionLock internalTryTakeLockNow(RegionPos centerPos, int lockRadius) {
        final ReadOnlyRegionLock lock = internalTryTakeReadOnlyLockNow(centerPos, lockRadius);
        return lock == null ? null : new WriteRegionLock(lock);
    }

    @Nullable
    public RegionLock tryTakeReadOnlyLockNow(RegionPos centerPos) {
        return internalTryTakeReadOnlyLockNow(centerPos, REGION_LOCK_RADIUS);
    }

    @Nullable
    public ReadOnlyRegionLock internalTryTakeReadOnlyLockNow(RegionPos centerPos, int lockRadius) {
        final ReadOnlyRegionLock lock = new ReadOnlyRegionLock(lockRadius);

        for (int x = -lockRadius; x <= lockRadius; x++) {
            for (int z = -lockRadius; z <= lockRadius; z++) {
                RegionPos regionPos = x == 0 && z == 0 ? centerPos : new RegionPos(centerPos.x + x, centerPos.z + z);
                if (!lock.tryLockRegion(regionPos)) {
                    // Failed to take lock, abort
                    lock.unlock();
                    return null;
                }
            }
        }

        return lock;
    }

    @Nullable
    public CompletableFuture<Void> onUnlock(RegionPos centerPos, Supplier<CompletableFuture<Void>> nextTask) {
        for (int x = -REGION_LOCK_RADIUS; x <= REGION_LOCK_RADIUS; x++) {
            for (int z = -REGION_LOCK_RADIUS; z <= REGION_LOCK_RADIUS; z++) {
                RegionPos regionPos = x == 0 && z == 0 ? centerPos : new RegionPos(centerPos.x + x, centerPos.z + z);
                LockedRegion lockedRegion = this.lockedRegions.get(regionPos);
                if (lockedRegion != null) {
                    return lockedRegion.onUnlock(nextTask);
                }
            }
        }

        return nextTask.get();
    }

    /**
     * globalLock.writeLock() will claim all regions
     */
    public StampedLock globalLock() {
        return this.globalLock;
    }

    public interface RegionLock {
        Collection<RegionPos> lockedRegions();
        Thread owner();
        void unlock();
    }

    public class ReadOnlyRegionLock implements RegionLock {
        private final List<LockedRegion> readLocks;
        private final Thread thread;
        private long globalLockStamp = 0;

        private ReadOnlyRegionLock(int lockRadius) {
            this.thread = Thread.currentThread();
            this.readLocks = new ArrayList<>((lockRadius * 2 + 1) * (lockRadius * 2 + 1));
        }

        protected boolean tryLockRegion(RegionPos regionPos) {
            if (this.thread != Thread.currentThread()) {
                throw new IllegalStateException("Cannot lock a region from a different thread [expected=%s,got=%s]".formatted(thread, Thread.currentThread()));
            }

            if (globalLockStamp == 0 && (globalLockStamp = ShreddedPaperRegionLocker.this.globalLock.tryReadLock()) == 0) {
                return false;
            }

            LockedRegion lockedRegion = ShreddedPaperRegionLocker.this.lockedRegions.compute(regionPos, (k, prevValue) -> {
                if (prevValue == null) {
                    // This region is unlocked, let's lock it
                    ShreddedPaperRegionLocker.this.localLocks.get().add(regionPos);
                    ShreddedPaperRegionLocker.this.readOnlyLocks.get().add(regionPos);
                    LockedRegion newLockedRegion = new LockedRegion(regionPos, this.thread);
                    this.readLocks.add(newLockedRegion);
                    return newLockedRegion;
                } else {
                    // This region is already locked, it could be already locked by us or someone else
                    return prevValue;
                }
            });

            return lockedRegion.owner() == this.owner();
        }

        public Collection<RegionPos> lockedRegions() {
            return this.readLocks.stream().map(LockedRegion::regionPos).toList();
        }

        public Thread owner() {
            return this.thread;
        }

        public void unlock() {
            if (this.thread != Thread.currentThread()) {
                throw new IllegalStateException("Cannot unlock a lock from a different thread [expected=%s,got=%s]".formatted(thread, Thread.currentThread()));
            }

            for (LockedRegion lockedRegion : this.readLocks) {
                ShreddedPaperRegionLocker.this.lockedRegions.remove(lockedRegion.regionPos(), lockedRegion);
                ShreddedPaperRegionLocker.this.localLocks.get().remove(lockedRegion.regionPos());
                ShreddedPaperRegionLocker.this.readOnlyLocks.get().remove(lockedRegion.regionPos());
            }

            for (LockedRegion lockedRegion : this.readLocks) {
                lockedRegion.complete();
            }

            if (this.globalLockStamp != 0) {
                ShreddedPaperRegionLocker.this.globalLock.unlockRead(this.globalLockStamp);
            }
        }

    }

    public class WriteRegionLock implements RegionLock {
        private final List<RegionPos> writeLocks;
        private final ReadOnlyRegionLock superLock;

        private WriteRegionLock(ReadOnlyRegionLock superLock) {
            this.superLock = superLock;

            Collection<RegionPos> lockedRegions = superLock.lockedRegions();
            this.writeLocks = new ArrayList<>(lockedRegions);
            ShreddedPaperRegionLocker.this.writeLocks.get().addAll(lockedRegions);
            ShreddedPaperRegionLocker.this.readOnlyLocks.get().removeAll(lockedRegions);
        }

        @Override
        public Collection<RegionPos> lockedRegions() {
            return Collections.unmodifiableCollection(this.writeLocks);
        }

        @Override
        public Thread owner() {
            return this.superLock.owner();
        }

        @Override
        public void unlock() {
            ShreddedPaperRegionLocker.this.writeLocks.get().removeAll(this.writeLocks);
            ShreddedPaperRegionLocker.this.readOnlyLocks.get().addAll(this.writeLocks);
            this.superLock.unlock();
        }
    }

    public static class LockedRegion {
        private final RegionPos regionPos;
        private final Thread owner;
        private final CompletableFuture<Void> headUnlockFuture = new CompletableFuture<>();
        private CompletableFuture<Void> tailUnlockFuture = headUnlockFuture;

        private LockedRegion(RegionPos regionPos, Thread owner) {
            this.regionPos = regionPos;
            this.owner = owner;
        }

        public RegionPos regionPos() {
            return regionPos;
        }

        public Thread owner() {
            return owner;
        }

        public void complete() {
            headUnlockFuture.complete(null);
        }

        public CompletableFuture<Void> onUnlock(Supplier<CompletableFuture<Void>> nextTask) {
            return tailUnlockFuture = tailUnlockFuture.thenCompose(v -> nextTask.get());
        }
    }

}
