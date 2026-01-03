package io.multipaper.shreddedpaper.util;

import javax.annotation.Nullable;

public class ObjectHolder <T> {

    @Nullable T value;

    public ObjectHolder() {
        this(null);
    }

    public ObjectHolder(@Nullable T object) {
        this.value = object;
    }

    public @Nullable T value() {
        return value;
    }

    public void value(@Nullable T value) {
        this.value = value;
    }
}
