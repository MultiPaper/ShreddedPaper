package io.multipaper.shreddedpaper.region;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.slf4j.Logger;
import io.multipaper.shreddedpaper.util.SimpleStampedLock;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

public class LevelTicksRegionProxy<T> extends LevelTicks<T> {

    private static final Logger LOGGER = LogUtils.getClassLogger();

    private final LongPredicate tickingFutureReadyPredicate;
    private final Long2ObjectMap<LevelTicks<T>> regions = new Long2ObjectOpenHashMap<>();
    private final SimpleStampedLock regionsLock = new SimpleStampedLock();

    public LevelTicksRegionProxy(LongPredicate tickingFutureReadyPredicate) {
        super(tickingFutureReadyPredicate);
        this.tickingFutureReadyPredicate = tickingFutureReadyPredicate;
    }

    private LevelTicks<T> createRegionLevelTicks() {
        return new LevelTicks<>(tickingFutureReadyPredicate);
    }

    public Optional<LevelTicks<T>> get(BlockPos pos) {
        return get(new ChunkPos(pos));
    }

    public Optional<LevelTicks<T>> get(ChunkPos pos) {
        return get(RegionPos.forChunk(pos));
    }

    public Optional<LevelTicks<T>> get(RegionPos pos) {
        return Optional.ofNullable(regionsLock.optimisticRead(() -> regions.get(pos.longKey)));
    }

    public void addContainer(ChunkPos pos, LevelChunkTicks<T> scheduler) {
        get(pos).orElseGet(() -> {
            return regionsLock.write(() -> regions.computeIfAbsent(RegionPos.forChunk(pos).longKey, k -> createRegionLevelTicks()));
        }).addContainer(pos, scheduler);
    }

    public void removeContainer(ChunkPos pos) {
        get(pos).ifPresent(v -> {
            v.removeContainer(pos);

            if (v.isEmpty()) {
                regionsLock.write(() -> regions.remove(RegionPos.forChunk(pos).longKey));
            }
        });
    }

    @Override
    public void schedule(ScheduledTick<T> orderedTick) {
        get(orderedTick.pos()).orElseThrow(() -> new IllegalArgumentException("Chunk not loaded: " + orderedTick.pos())).schedule(orderedTick);
    }

    public void tick(RegionPos regionPos, long time, int maxTicks, BiConsumer<BlockPos, T> ticker) {
        get(regionPos).ifPresent(v -> {
            v.tick(time, maxTicks, ticker);
        });
    }

    @Override
    public void tick(long time, int maxTicks, BiConsumer<BlockPos, T> ticker) {
        // Do nothing
    }

    @Override
    public boolean hasScheduledTick(BlockPos pos, T type) {
        return get(pos).map(v -> v.hasScheduledTick(pos, type)).orElse(false);
    }

    @Override
    public boolean willTickThisTick(BlockPos pos, T type) {
        return get(pos).map(v -> v.willTickThisTick(pos, type)).orElse(false);
    }

    @Override
    public void clearArea(BoundingBox box) {
        // Surely no one will miss this
    }

    @Override
    public void copyArea(BoundingBox box, Vec3i offset) {
        // Surely no one will miss this
    }

    @Override
    public void copyAreaFrom(LevelTicks<T> scheduler, BoundingBox box, Vec3i offset) {
        // Surely no one will miss this
    }

    @Override
    public int count() {
        return -1;
    }
}
