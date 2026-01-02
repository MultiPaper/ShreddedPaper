package io.multipaper.shreddedpaper.region;

import ca.spottedleaf.concurrentutil.executor.queue.PrioritisedTaskQueue;
import ca.spottedleaf.moonrise.common.list.IteratorSafeOrderedReferenceSet;
import ca.spottedleaf.moonrise.common.util.TickThread;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LevelChunkRegion {

    private final ServerLevel level;
    private final RegionPos regionPos;
    private final List<LevelChunk> levelChunks = new ArrayList<>(RegionPos.REGION_SIZE * RegionPos.REGION_SIZE);
    private final LongOpenHashSet playerTickingChunkRequests = new LongOpenHashSet(); // ChunkPos.longKey
    private final IteratorSafeOrderedReferenceSet<Entity> tickingEntities = new IteratorSafeOrderedReferenceSet<>(); // Use IteratorSafeOrderedReferenceSet to maintain entity tick order
    private final Set<Entity> trackedEntities = new ObjectOpenHashSet<>();
    private final ConcurrentLinkedQueue<DelayedTask> scheduledTasks = new ConcurrentLinkedQueue<>(); // Writable tasks
    private final PrioritisedTaskQueue internalTasks = new PrioritisedTaskQueue(); // Read-only tasks
    private final ObjectOpenHashSet<ServerPlayer> players = new ObjectOpenHashSet<>();
    public final LongLinkedOpenHashSet unloadQueue = new LongLinkedOpenHashSet();
    public final List<TickingBlockEntity> tickingBlockEntities = new ReferenceArrayList<>();
    public final List<TickingBlockEntity> pendingBlockEntityTickers = new ReferenceArrayList<>();
    private final ObjectOpenHashSet<Mob> navigatingMobs = new ObjectOpenHashSet<>();
    private final ObjectLinkedOpenHashSet<BlockEventData> blockEvents = new ObjectLinkedOpenHashSet<>();
    public ArrayDeque<RedstoneTorchBlock.Toggle> redstoneUpdateInfos;

    public LevelChunkRegion(ServerLevel level, RegionPos regionPos) {
        this.level = level;
        this.regionPos = regionPos;
    }

    public synchronized void add(LevelChunk levelChunk) {
        this.levelChunks.add(levelChunk);
    }

    public synchronized void remove(LevelChunk levelChunk) {
        if (!this.levelChunks.remove(levelChunk)) {
            throw new IllegalStateException("Tried to remove a chunk that wasn't in the region: " + levelChunk.getPos());
        }
    }

    public void addPlayerTickingRequest(final ChunkPos chunkPos) {
        TickThread.ensureTickThread(this.level, chunkPos, "Cannot add player ticking request async");
        this.playerTickingChunkRequests.add(chunkPos.toLong());
    }

    public void removePlayerTickingRequest(final ChunkPos chunkPos) {
        TickThread.ensureTickThread(this.level, chunkPos, "Cannot remove player ticking request async");
        this.playerTickingChunkRequests.remove(chunkPos.toLong());
    }

    public boolean isPlayerTickingRequested(final ChunkPos chunkPos) {
        if (chunkPos.getRegionPos().toLong() != this.regionPos.toLong()) {
            throw new IllegalStateException("Chunk %s is not in region %s".formatted(chunkPos, this.regionPos));
        }

        return this.playerTickingChunkRequests.contains(chunkPos.toLong());
    }

    public synchronized void addTickingEntity(Entity entity) {
        if (!this.tickingEntities.add(entity)) {
            throw new IllegalStateException("Tried to add an entity that was already in the ticking list: " + entity);
        }
    }

    public synchronized void removeTickingEntity(Entity entity) {
        if (!this.tickingEntities.remove(entity)) {
            throw new IllegalStateException("Tried to remove an entity that wasn't in the ticking list: " + entity);
        }
    }

    public void forEachTickingEntity(Consumer<Entity> action) {
        IteratorSafeOrderedReferenceSet.Iterator<Entity> iterator = this.tickingEntities.iterator();
        try {
            while (iterator.hasNext()) {
                action.accept(iterator.next());
            }
        } finally {
            iterator.finishedIterating();
        }
    }

    public synchronized void addTrackedEntity(Entity entity) {
        if (!this.trackedEntities.add(entity)) {
            throw new IllegalStateException("Tried to add an entity that was already tracked: " + entity);
        }
    }

    public synchronized void removeTrackedEntity(Entity entity) {
        if (!this.trackedEntities.remove(entity)) {
            throw new IllegalStateException("Tried to remove an entity that wasn't already tracked: " + entity);
        }
    }

    public synchronized void forEachTrackedEntity(Consumer<Entity> action) {
        this.trackedEntities.forEach(action);
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public void scheduleTask(Runnable task, long delay) {
        this.scheduledTasks.add(new DelayedTask(task, delay));
    }

    public PrioritisedTaskQueue getInternalTaskQueue() {
        return this.internalTasks;
    }

    public synchronized void addPlayer(ServerPlayer player) {
        if (!this.players.add(player)) {
            throw new IllegalStateException("Tried to add a player that was already in the region: " + player.getUUID());
        }
    }

    public synchronized void removePlayer(ServerPlayer player) {
        if (!this.players.remove(player)) {
            throw new IllegalStateException("Tried to remove a player that wasn't in the region: " + player.getUUID());
        }
    }

    public synchronized List<ServerPlayer> getPlayers() {
        return this.players.isEmpty() ? List.of() : new ObjectArrayList<>(this.players);
    }

    public synchronized void addNavigationMob(Mob mob) {
        this.navigatingMobs.add(mob);
    }

    public synchronized void removeNavigationMob(Mob mob) {
        this.navigatingMobs.remove(mob);
    }

    public synchronized void collectNavigatingMobs(List<Mob> collection) {
        collection.addAll(this.navigatingMobs);
    }

    public RegionPos getRegionPos() {
        return regionPos;
    }

    public void forEach(Consumer<LevelChunk> consumer) {
        // This method has the chance of skipping a chunk if a chunk is removed via another thread during this iteration
        for (int i = 0; i < this.levelChunks.size(); i++) {
            try {
                LevelChunk levelChunk = this.levelChunks.get(i);
                if (levelChunk != null) {
                    consumer.accept(levelChunk);
                }
            } catch (IndexOutOfBoundsException e) {
                // Ignore - multithreaded modification
            }
        }
    }

    public void tickTasks() {
        if (this.scheduledTasks.isEmpty()) return;

        List<DelayedTask> toRun = new ArrayList<>();
        for (DelayedTask task : this.scheduledTasks) {
            // Check if a task should run before executing the tasks, as tasks may add more tasks while they are running
            if (task.shouldRun()) {
                toRun.add(task);
            }
        }

        this.scheduledTasks.removeAll(toRun);
        toRun.forEach(DelayedTask::run);
    }

    public synchronized void addBlockEvent(BlockEventData blockEvent) {
        this.blockEvents.add(blockEvent);
    }

    public synchronized void addAllBlockEvents(Collection<BlockEventData> blockEvents) {
        this.blockEvents.addAll(blockEvents);
    }

    public boolean hasBlockEvents() {
        return !this.blockEvents.isEmpty();
    }

    public synchronized BlockEventData removeFirstBlockEvent() {
        return this.blockEvents.removeFirst();
    }

    public synchronized void removeBlockEventsIf(Predicate<BlockEventData> predicate) {
        this.blockEvents.removeIf(predicate);
    }

    public boolean isEmpty() {
        return levelChunks.isEmpty()
                && playerTickingChunkRequests.isEmpty()
                && tickingEntities.size() == 0
                && scheduledTasks.isEmpty()
                && internalTasks.getTotalTasksExecuted() >= internalTasks.getTotalTasksScheduled()
                && players.isEmpty()
                && unloadQueue.isEmpty()
                && tickingBlockEntities.isEmpty()
                && pendingBlockEntityTickers.isEmpty()
                && trackedEntities.isEmpty()
                && navigatingMobs.isEmpty()
                && blockEvents.isEmpty()
                ;
    }
}
