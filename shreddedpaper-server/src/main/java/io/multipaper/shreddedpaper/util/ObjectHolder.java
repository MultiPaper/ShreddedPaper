package io.multipaper.shreddedpaper.util;

public class ObjectHolder <T> {

    T value;

    public ObjectHolder() {
        this(null);
    }

    public ObjectHolder(T object) {
        this.value = object;
    }

    public T value() {
        return value;
    }

    public void value(T value) {
        this.value = value;
    }
}
