package io.multipaper.shreddedpaper.threading;

import io.papermc.paper.util.TickThread;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.server.level.ChunkHolder;

public class ShreddedPaperChangesBroadcaster {

    private static final ThreadLocal<ReferenceOpenHashSet<ChunkHolder>> needsChangeBroadcastingThreadLocal = new ThreadLocal<>();

    public static void setAsWorkerThread() {
        if (needsChangeBroadcastingThreadLocal.get() == null) {
            needsChangeBroadcastingThreadLocal.set(new ReferenceOpenHashSet<>());
        }
    }

    public static void add(ChunkHolder chunkHolder) {
        ReferenceOpenHashSet<ChunkHolder> needsChangeBroadcasting = needsChangeBroadcastingThreadLocal.get();
        if (needsChangeBroadcasting != null) {
            needsChangeBroadcasting.add(chunkHolder);
        }
    }

    public static void remove(ChunkHolder chunkHolder) {
        ReferenceOpenHashSet<ChunkHolder> needsChangeBroadcasting = needsChangeBroadcastingThreadLocal.get();
        if (needsChangeBroadcasting != null) {
            needsChangeBroadcasting.remove(chunkHolder);
        }
    }

    public static void broadcastChanges() {
        broadcastChanges(needsChangeBroadcastingThreadLocal.get());
    }

    public static void broadcastChanges(ReferenceOpenHashSet<ChunkHolder> needsChangeBroadcasting) {
        if (!needsChangeBroadcasting.isEmpty()) {
            ReferenceOpenHashSet<ChunkHolder> copy = needsChangeBroadcasting.clone();
            needsChangeBroadcasting.clear();
            for (ChunkHolder holder : copy) {
                if (!TickThread.isTickThreadFor(holder.newChunkHolder.world, holder.pos)) {
                    // The changes will get picked up by the correct thread when it is ticked
                    continue;
                }

                holder.broadcastChanges(holder.getFullChunkNowUnchecked()); // LevelChunks are NEVER unloaded
                if (holder.needsBroadcastChanges()) {
                    // I DON'T want to KNOW what DUMB plugins might be doing.
                    needsChangeBroadcasting.add(holder);
                }
            }
        }
    }
}
