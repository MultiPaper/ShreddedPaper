package io.multipaper.shreddedpaper.util;

import javax.annotation.Nullable;
import java.util.function.Function;

public class ObjectHolder <T> {

    @Nullable T value;

    public ObjectHolder() {
        this(null);
    }

    public ObjectHolder(@Nullable T object) {
        this.value = object;
    }

    public @Nullable T get() {
        return value;
    }

    public void set(@Nullable T value) {
        this.value = value;
    }

    public void map(Function<T, T> mapper) {
        this.set(mapper.apply(this.get()));
    }
}
