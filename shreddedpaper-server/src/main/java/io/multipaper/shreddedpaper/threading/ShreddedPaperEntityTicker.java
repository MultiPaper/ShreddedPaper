package io.multipaper.shreddedpaper.threading;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

public class ShreddedPaperEntityTicker {

    public static void tickEntity(Entity entity) {
        ProfilerFiller profilerFiller = Profiler.get();
        ServerLevel level = (ServerLevel) entity.level();

        if (!entity.isRemoved()) {
            if (!level.tickRateManager().isEntityFrozen(entity)) {
                profilerFiller.push("checkDespawn");
                entity.checkDespawn();
                profilerFiller.pop();
                if (true) { // Paper - rewrite chunk system
                    Entity vehicle = entity.getVehicle();
                    if (vehicle != null) {
                        if (!vehicle.isRemoved() && vehicle.hasPassenger(entity)) {
                            return;
                        }

                        entity.stopRiding();
                    }

                    profilerFiller.push("tick");
                    level.guardEntityTick(level::tickNonPassenger, entity);
                    profilerFiller.pop();
                }
            }
        }
    }

    /** processTrackQueue has been renamed to newTrackerTick */
    public static void processTrackQueue(Entity entity) {
        ChunkMap.TrackedEntity tracker = Objects.requireNonNull(entity.moonrise$getTrackedEntity());
        ((ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerTrackedEntity)tracker).moonrise$tick(((ca.spottedleaf.moonrise.patches.chunk_system.entity.ChunkSystemEntity)entity).moonrise$getChunkData().nearbyPlayers);
        if (((ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerTrackedEntity)tracker).moonrise$hasPlayers()
                || ((ca.spottedleaf.moonrise.patches.chunk_system.entity.ChunkSystemEntity)entity).moonrise$getChunkStatus().isOrAfter(FullChunkStatus.ENTITY_TICKING)) {
            tracker.serverEntity.sendChanges();
        }
    }
}
