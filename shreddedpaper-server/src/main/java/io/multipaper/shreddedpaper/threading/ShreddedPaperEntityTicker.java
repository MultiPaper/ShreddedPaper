package io.multipaper.shreddedpaper.threading;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

public class ShreddedPaperEntityTicker {

    public static void tickEntity(Entity entity) {
        ServerLevel level = (ServerLevel) entity.level();

        entity.activatedPriorityReset = false; // Pufferfish - DAB
        if (!entity.isRemoved()) {
            if (false && level.shouldDiscardEntity(entity)) { // CraftBukkit - We prevent spawning in general, so this butchering is not needed
                entity.discard();
            } else if (!level.tickRateManager().isEntityFrozen(entity)) {
                //gameprofilerfiller.push("checkDespawn"); // Purpur
                entity.checkDespawn();
                //gameprofilerfiller.pop(); // Purpur
                if (true || level.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(entity.chunkPosition().toLong())) { // Paper - now always true if in the ticking list
                    Entity entity1 = entity.getVehicle();

                    if (entity1 != null) {
                        if (!entity1.isRemoved() && entity1.hasPassenger(entity)) {
                            return;
                        }

                        entity.stopRiding();
                    }

                    //gameprofilerfiller.push("tick"); // Purpur
                    level.guardEntityTick(level::tickNonPassenger, entity);
                    //gameprofilerfiller.pop(); // Purpur
                }
            }
        }
    }

    public static void processTrackQueue(Entity entity) {
        ChunkMap.TrackedEntity tracker = Objects.requireNonNull(entity.tracker);
        tracker.updatePlayers(entity.getPlayersInTrackRange());
        tracker.serverEntity.sendChanges();
    }
}
