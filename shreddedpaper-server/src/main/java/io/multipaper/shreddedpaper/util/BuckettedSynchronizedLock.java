package io.multipaper.shreddedpaper.util;

import java.util.function.Supplier;

public class BuckettedSynchronizedLock<T> {

    private final int capacity;
    private final int bitMask;
    private final Object[] buckets;

    public BuckettedSynchronizedLock(final int capacity) {
        this.capacity = capacity;
        this.bitMask = capacity - 1;
        this.buckets = new Object[this.capacity];
        for (int i = 0; i < this.capacity; i++) {
            this.buckets[i] = new Object();
        }

        if ((this.capacity & this.bitMask) != 0) {
            throw new IllegalArgumentException("Capacity must be a power of 2, got: " + capacity);
        }
    }

    public void write(T key, Runnable runnable) {
        synchronized (this.buckets[key.hashCode() & this.bitMask]) {
            runnable.run();
        }
    }

    public <V> V write(T key, Supplier<V> supplier) {
        synchronized (this.buckets[key.hashCode() & this.bitMask]) {
            return supplier.get();
        }
    }

}
