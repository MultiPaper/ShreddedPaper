package io.multipaper.shreddedpaper.util;

import java.util.Objects;
import java.util.function.Function;

public class ObjectHolder <T> {

    T value;

    public ObjectHolder() {
        this(null);
    }

    public ObjectHolder(T object) {
        this.value = object;
    }

    public T get() {
        return value;
    }

    public T set(T value) {
        return this.value = value;
    }

    public T map(Function<T, T> mapper) {
        return this.set(mapper.apply(this.get()));
    }

    @Override
    public String toString() {
        return "ObjectHolder{value=%s}".formatted(this.value);
    }

    @Override
    public int hashCode() {
        return this.value == null ? 0 : this.value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof ObjectHolder<?> that && Objects.equals(this.value, that.value));
    }


}
