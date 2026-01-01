package io.multipaper.shreddedpaper.util;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Int2ObjectMapWrapper<V> implements Int2ObjectMap<V> {
    private final Map<Integer, V> map;
    private boolean hasPrintedDepricatedWarning = false;

    public Int2ObjectMapWrapper(Map<Integer, V> m) {
        this.map = m;
    }

    private void printDepricatedWarning() {
        if (!this.hasPrintedDepricatedWarning) {
            this.hasPrintedDepricatedWarning = true;
            LogUtils.getClassLogger().warn("Iterating on the Int2ObjectMapWrapper is inefficient. Please iterate directly on the wrapped map instead.", new Exception("Stack trace"));
        }
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    @Override
    public void putAll(@NotNull Map<? extends Integer, ? extends V> m) {
        this.map.putAll(m);
    }

    @Override
    public void defaultReturnValue(V rv) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V defaultReturnValue() {
        return null;
    }

    @Override
    public ObjectSet<Entry<V>> int2ObjectEntrySet() {
        printDepricatedWarning();
        ObjectArraySet<Entry<V>> set = new ObjectArraySet<>(this.size());
        this.map.forEach((k, v) -> set.add(new AbstractInt2ObjectMap.BasicEntry<>(k, v)));
        return set;
    }

    @NotNull
    @Override
    public IntSet keySet() {
        printDepricatedWarning();
        return new IntArraySet(this.map.keySet());
    }

    @NotNull
    @Override
    public ObjectCollection<V> values() {
        printDepricatedWarning();
        return new ObjectArrayList<>(this.map.values());
    }

    @Override
    public V get(int key) {
        return this.map.get(key);
    }

    @Override
    public boolean containsKey(int key) {
        return this.map.containsKey(key);
    }

    @Override
    public V getOrDefault(final int key, final V defaultValue) {
        return this.map.getOrDefault(key, defaultValue);
    }

    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        return this.map.getOrDefault(key, defaultValue);
    }

    @Override
    public V putIfAbsent(final int key, final V value) {
        return this.map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(final int key, final Object value) {
        return this.map.remove(key, value);
    }

    @Override
    public boolean replace(final int key, final V oldValue, final V newValue) {
        return this.map.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(final int key, final V value) {
        return containsKey(key) ? put(key, value) : defaultReturnValue();
    }

    @Override
    public V computeIfAbsent(final int key, final java.util.function.IntFunction<? extends V> mappingFunction) {
        return this.map.computeIfAbsent(key, mappingFunction::apply);
    }

    @Override
    public V computeIfAbsent(final int key, final Int2ObjectFunction<? extends V> mappingFunction) {
        return this.map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(final int key, final java.util.function.BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
        return this.map.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(final int key, final java.util.function.BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
        return this.map.compute(key, remappingFunction);
    }

    @Override
    public V merge(final int key, final V value, final java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return this.map.merge(key, value, remappingFunction);
    }

    @Override
    public V put(final int key, final V value) {
        return this.map.put(key, value);
    }

    @Override
    public V remove(final int key) {
        return this.map.remove(key);
    }

    @Override
    public V put(final Integer key, final V value) {
        return this.map.put(key, value);
    }

    @Override
    public V get(final Object key) {
        return this.map.get(key);
    }

    @Override
    public V remove(final Object key) {
        return this.map.remove(key);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @NotNull
    @Override
    public ObjectSet<Map.Entry<Integer, V>> entrySet() {
        printDepricatedWarning();
        return new ObjectArraySet<>(this.map.entrySet());
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public void forEach(BiConsumer<? super Integer, ? super V> consumer) {
        this.map.forEach(consumer);
    }

    @Override
    public void replaceAll(BiFunction<? super Integer, ? super V, ? extends V> function) {
        this.map.replaceAll(function);
    }

    @Nullable
    @Override
    public V putIfAbsent(Integer key, V value) {
        return this.map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return this.map.remove(key, value);
    }

    @Override
    public boolean replace(Integer key, V oldValue, V newValue) {
        return this.map.replace(key, oldValue, newValue);
    }

    @Nullable
    @Override
    public V replace(Integer key, V value) {
        return this.map.replace(key, value);
    }

    @Override
    public V computeIfAbsent(Integer key, @NotNull Function<? super Integer, ? extends V> mappingFunction) {
        return this.map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(Integer key, @NotNull BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
        return this.map.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(Integer key, @NotNull BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
        return this.map.compute(key, remappingFunction);
    }

    @Override
    public V merge(Integer key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return this.map.merge(key, value, remappingFunction);
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Int2ObjectMapWrapper<?> wrapper && this.map.equals(wrapper.map));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.map.toString();
    }
}
