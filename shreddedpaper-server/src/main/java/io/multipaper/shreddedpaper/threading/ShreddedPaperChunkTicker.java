package io.multipaper.shreddedpaper.threading;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.multipaper.shreddedpaper.config.ShreddedPaperConfiguration;
import io.multipaper.shreddedpaper.region.RegionPos;
import io.papermc.paper.util.player.NearbyPlayers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import io.multipaper.shreddedpaper.region.LevelChunkRegion;
import org.bukkit.craftbukkit.entity.CraftEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ShreddedPaperChunkTicker {

    private static final ThreadLocal<LevelChunkRegion> currentlyTickingRegion = new ThreadLocal<>();

    private final ServerChunkCache serverChunkCache;

    private final List<Entity> trackedEntitiesWorkerList = new ArrayList<>(); // Re-usable list for processing tracked entities in parallel

    public ShreddedPaperChunkTicker(ServerChunkCache serverChunkCache) {
        this.serverChunkCache = serverChunkCache;
    }

    public CompletableFuture<Void> tickChunks(NaturalSpawner.SpawnState spawnercreature_d) {
        ServerLevel level = this.serverChunkCache.chunkMap.level;
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        level.chunkSource.tickingRegions.forEach(
                region -> futures.add(this.tickRegion(level, region, spawnercreature_d))
        );

        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));

        if (ShreddedPaperConfiguration.get().optimizations.processTrackQueueInParallel) future = future.thenCompose(v -> this.processTrackQueueInParallel(level));

        if (ShreddedPaperConfiguration.get().optimizations.flushQueueInParallel) future = future.thenCompose(v -> this.flushQueueInParallel(level));

        return future;
    }

    private CompletableFuture<Void> processTrackQueueInParallel(ServerLevel level) {
        level.getChunkSource().mainThreadProcessor.managedBlock(() -> level.chunkScheduler.getRegionLocker().globalLock().tryWriteLock() != 0);
        CompletableFuture<Void> allFuture = CompletableFuture.completedFuture(null);
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            trackedEntitiesWorkerList.clear();
            level.chunkSource.tickingRegions.forEach(
                    region -> region.forEachTrackedEntity(trackedEntitiesWorkerList::add)
            );

            List<List<Entity>> trackedEntitiesTasks = Lists.partition(trackedEntitiesWorkerList, Math.max(1, trackedEntitiesWorkerList.size() / ShreddedPaperTickThread.THREAD_COUNT / 3));
            for (List<Entity> trackedEntities : trackedEntitiesTasks) {
                if (trackedEntities.isEmpty()) continue;
                futures.add(CompletableFuture.runAsync(() -> trackedEntities.forEach(ShreddedPaperEntityTicker::processTrackQueue), ShreddedPaperTickThread.getExecutor()));
            }

            allFuture = allFuture.thenCompose(v -> CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)));
            return allFuture;
        } finally {
            allFuture.whenComplete((v, e) -> level.chunkScheduler.getRegionLocker().globalLock().tryUnlockWrite());
        }
    }

    private CompletableFuture<Void> flushQueueInParallel(ServerLevel level) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        List<List<ServerPlayer>> playersTasks = Lists.partition(new ArrayList<>(level.players()), Math.max(1, level.players().size() / ShreddedPaperTickThread.THREAD_COUNT / 3));
        for (List<ServerPlayer> players : playersTasks) {
            if (players.isEmpty()) continue;
            futures.add(CompletableFuture.runAsync(() -> players.forEach(player -> player.connection.connection.flushQueue()), ShreddedPaperTickThread.getExecutor()));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<Void> tickRegion(ServerLevel level, LevelChunkRegion region, NaturalSpawner.SpawnState spawnercreature_d) {
        return level.chunkScheduler.schedule(region.getRegionPos(), () -> this._tickRegion(level, region, spawnercreature_d)).exceptionally(e -> {
            LogUtils.getClassLogger().error("Exception ticking region {}", region.getRegionPos(), e);
            MinecraftServer.chunkSystemCrash = new RuntimeException("Ticking thread crash while ticking region " + region.getRegionPos(), e);
            return null;
        });
    }

    public static boolean isCurrentlyTickingRegion(Level level, RegionPos regionPos) {
        LevelChunkRegion region = currentlyTickingRegion.get();
        return region != null && level.equals(region.getLevel()) && regionPos.equals(region.getRegionPos());
    }

    private void _tickRegion(ServerLevel level, LevelChunkRegion region, NaturalSpawner.SpawnState spawnercreature_d) {
        try {
            currentlyTickingRegion.set(region);

            if (!(ShreddedPaperTickThread.isShreddedPaperTickThread())) {
                throw new IllegalStateException("Ticking region " + level.convertable.getLevelId() + " " + region.getRegionPos() + " outside of ShreddedPaperTickThread!");
            }

            ShreddedPaperChangesBroadcaster.setAsWorkerThread();

            while (region.getInternalTaskQueue().executeTask()) ;

            level.chunkTaskScheduler.chunkHolderManager.processUnloads(region);

            region.forEachTickingEntity(entity -> {
                CraftEntity bukkitEntity = entity.getBukkitEntityRaw();
                if (bukkitEntity != null && !entity.isRemoved()) { // Entity could have been removed by another entity's task
                    bukkitEntity.taskScheduler.executeTick();
                }
            });

            region.tickTasks();

            if (level.tickRateManager().runsNormally()) {
                level.handlingTickThreadLocal.set(true);

                level.blockTicks.tick(region.getRegionPos(), level.getGameTime(), level.paperConfig().environment.maxBlockTicks, level::tickBlock);
                level.fluidTicks.tick(region.getRegionPos(), level.getGameTime(), level.paperConfig().environment.maxBlockTicks, level::tickFluid);

                region.forEach(chunk -> this._tickChunk(level, chunk, spawnercreature_d));

                level.runBlockEvents(region);

                level.handlingTickThreadLocal.set(false);
            }

            region.forEachTickingEntity(ShreddedPaperEntityTicker::tickEntity);

            if (!ShreddedPaperConfiguration.get().optimizations.processTrackQueueInParallel) region.forEachTrackedEntity(ShreddedPaperEntityTicker::processTrackQueue);

            level.tickBlockEntities(region.tickingBlockEntities, region.pendingBlockEntityTickers);

            region.getPlayers().forEach(ShreddedPaperPlayerTicker::tickPlayer);

            while (region.getInternalTaskQueue().executeTask()) ;

            ShreddedPaperChangesBroadcaster.broadcastChanges();

            if (region.isEmpty()) {
                level.chunkSource.tickingRegions.remove(region.getRegionPos());
            }
        } finally {
            currentlyTickingRegion.remove();
        }
    }

    private void _tickChunk(ServerLevel level, LevelChunk chunk1, NaturalSpawner.SpawnState spawnercreature_d) {
        if (chunk1.getChunkHolder().vanillaChunkHolder.needsBroadcastChanges()) ShreddedPaperChangesBroadcaster.add(chunk1.getChunkHolder().vanillaChunkHolder); // ShreddedPaper

        // ShreddedPaper start - clear chunk packet cache
        if (chunk1.cachedChunkPacket != null && chunk1.cachedChunkPacketLastAccessed < level.getGameTime() - ShreddedPaperConfiguration.get().optimizations.chunkPacketCaching.expireAfter) {
            chunk1.cachedChunkPacket = null;
        }
        // ShreddedPaper end - clear chunk packet cache

        // Start - Import the same variables as the original chunk ticking method to make copying new changes easier
        int j = 1; // Inhabited time increment in ticks
        boolean flag = level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && !level.players().isEmpty(); // Should run mob spawning code
        NearbyPlayers nearbyPlayers = level.chunkSource.chunkMap.getNearbyPlayers();
        ChunkPos chunkcoordintpair = chunk1.getPos();
        boolean flag1 = level.ticksPerSpawnCategory.getLong(org.bukkit.entity.SpawnCategory.ANIMAL) != 0L && level.getLevelData().getGameTime() % level.ticksPerSpawnCategory.getLong(org.bukkit.entity.SpawnCategory.ANIMAL) == 0L;
        int l = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        // End

        // Paper start - optimise chunk tick iteration
        com.destroystokyo.paper.util.maplist.ReferenceList<ServerPlayer> playersNearby
                = nearbyPlayers.getPlayers(chunkcoordintpair, io.papermc.paper.util.player.NearbyPlayers.NearbyMapType.SPAWN_RANGE);
        if (playersNearby == null) {
            return;
        }
        Object[] rawData = playersNearby.getRawData();
        boolean spawn = false;
        boolean tick = false;
        for (int itr = 0, len = playersNearby.size(); itr < len; ++itr) {
            try { // ShreddedPaper - concurrent modification
            ServerPlayer player = (ServerPlayer)rawData[itr];
            if (player == null) continue; // ShreddedPaper - concurrent modification
            if (player.isSpectator()) {
                continue;
            }

            double distance = ChunkMap.euclideanDistanceSquared(chunkcoordintpair, player);
            spawn |= player.lastEntitySpawnRadiusSquared >= distance;
            tick |= ((double)io.papermc.paper.util.player.NearbyPlayers.SPAWN_RANGE_VIEW_DISTANCE_BLOCKS) * ((double)io.papermc.paper.util.player.NearbyPlayers.SPAWN_RANGE_VIEW_DISTANCE_BLOCKS) >= distance;
            if (spawn & tick) {
                break;
            }
            } catch (IndexOutOfBoundsException ignored) {} // ShreddedPaper - concurrent modification
        }
        if (tick && chunk1.chunkStatus.isOrAfter(net.minecraft.server.level.FullChunkStatus.ENTITY_TICKING)) {
            // Paper end - optimise chunk tick iteration
            chunk1.incrementInhabitedTime(j);
            // Pufferfish Code:
            if (spawn && flag && (!gg.pufferfish.pufferfish.PufferfishConfig.enableAsyncMobSpawning || level.getChunkSource()._pufferfish_spawnCountsReady.get()) && (level.chunkSource.spawnEnemies || level.chunkSource.spawnFriendlies) && level.getWorldBorder().isWithinBounds(chunkcoordintpair)) { // Spigot // Paper - optimise chunk tick iteration // Pufferfish
                NaturalSpawner.spawnForChunk(level, chunk1, spawnercreature_d, level.chunkSource.spawnFriendlies, level.chunkSource.spawnEnemies, flag1); // Pufferfish
            // Non-Pufferfish code:
            // if (spawn && flag && (level.chunkSource.spawnEnemies || level.chunkSource.spawnFriendlies) && level.getWorldBorder().isWithinBounds(chunkcoordintpair)) { // Spigot // Paper - optimise chunk tick iteration
            //     NaturalSpawner.spawnForChunk(level, chunk1, spawnercreature_d, level.chunkSource.spawnFriendlies, level.chunkSource.spawnEnemies, flag1);
            }

            if (true || level.shouldTickBlocksAt(chunkcoordintpair.toLong())) { // Paper - optimise chunk tick iteration
                level.tickChunk(chunk1, l);
                // if ((chunksTicked++ & 1) == 0) net.minecraft.server.MinecraftServer.getServer().executeMidTickTasks(); // Paper // ShreddedPaper - does this need to be implemented??
            }
        }
    }

}
