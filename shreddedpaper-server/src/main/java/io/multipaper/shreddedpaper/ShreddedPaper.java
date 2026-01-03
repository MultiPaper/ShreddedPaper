package io.multipaper.shreddedpaper;

import io.multipaper.shreddedpaper.threading.ShreddedPaperRegionScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import io.multipaper.shreddedpaper.region.RegionPos;

import java.util.function.Consumer;

public class ShreddedPaper {

    public static void runSync(Location location, Runnable runnable) {
        runSync(((CraftWorld) location.getWorld()).getHandle(), new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), runnable);
    }

    public static void runSync(Entity entity, Runnable runnable) {
        entity.getBukkitEntity().taskScheduler.schedule(e -> runnable.run(), null, 1);
    }

    public static void runSync(ServerLevel serverLevel, BlockPos blockPos, Runnable runnable) {
        runSync(serverLevel, new ChunkPos(blockPos), runnable);
    }

    public static void runSync(ServerLevel serverLevel, ChunkPos chunkPos, Runnable runnable) {
        serverLevel.getChunkSource().tickingRegions.scheduleTask(RegionPos.forChunk(chunkPos), runnable);
    }

    public static void runSync(ServerLevel serverLevel1, ChunkPos chunkPos1, ServerLevel serverLevel2, ChunkPos chunkPos2, Runnable runnable) {
        ShreddedPaperRegionScheduler.scheduleAcrossLevels(serverLevel1, RegionPos.forChunk(chunkPos1), serverLevel2, RegionPos.forChunk(chunkPos2), runnable);
    }

    public static void ensureSync(Location location, Runnable runnable) {
        ensureSync(((CraftWorld) location.getWorld()).getHandle(), new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), runnable);
    }

    public static void ensureSync(Entity entity, Runnable runnable) {
        if (!isSync((ServerLevel) entity.level(), entity.chunkPosition())) {
            runSync(entity, runnable);
        } else {
            runnable.run();
        }
    }

    public static void ensureSync(Entity entity, Consumer<Entity> consumer) {
        if (!isSync((ServerLevel) entity.level(), entity.chunkPosition())) {
            entity.getBukkitEntity().taskScheduler.schedule(consumer, null, 1);
        } else {
            consumer.accept(entity);
        }
    }

    public static void ensureSync(ServerLevel serverLevel, BlockPos blockPos, Runnable runnable) {
        ensureSync(serverLevel, new ChunkPos(blockPos), runnable);
    }

    public static void ensureSync(ServerLevel serverLevel, ChunkPos chunkPos, Runnable runnable) {
        if (!isSync(serverLevel, chunkPos)) {
            runSync(serverLevel, chunkPos, runnable);
        } else {
            runnable.run();
        }
    }

    public static void ensureSync(Entity entity1, ServerLevel serverLevel2, ChunkPos chunkPos2, Runnable runnable) {
        if (!isSync((ServerLevel) entity1.level(), entity1.chunkPosition()) || !isSync(serverLevel2, chunkPos2)) {
            runSync((ServerLevel) entity1.level(), entity1.chunkPosition(), serverLevel2, chunkPos2, () -> ensureSync(entity1, serverLevel2, chunkPos2, runnable)); // Entity may have moved since, ensure still sync
        } else {
            runnable.run();
        }
    }

    public static void ensureSync(Entity entity1, Entity entity2, Runnable runnable) {
        if (!isSync((ServerLevel) entity1.level(), entity1.chunkPosition()) || !isSync((ServerLevel) entity2.level(), entity2.chunkPosition())) {
            runSync((ServerLevel) entity1.level(), entity1.chunkPosition(), (ServerLevel) entity2.level(), entity2.chunkPosition(), () -> ensureSync(entity1, entity2, runnable)); // Entity may have moved since, ensure still sync
        } else {
            runnable.run();
        }
    }

    public static void ensureSync(ServerLevel serverLevel1, ChunkPos chunkPos1, ServerLevel serverLevel2, ChunkPos chunkPos2, Runnable runnable) {
        if (!isSync(serverLevel1, chunkPos1) || !isSync(serverLevel2, chunkPos2)) {
            runSync(serverLevel1, chunkPos1, serverLevel2, chunkPos2, runnable);
        } else {
            runnable.run();
        }
    }

    public static boolean isSync(ServerLevel serverLevel, ChunkPos chunkPos) {
        return serverLevel.chunkScheduler.getRegionLocker().hasWriteLock(RegionPos.forChunk(chunkPos));
    }

}
