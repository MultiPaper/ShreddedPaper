package io.multipaper.shreddedpaper.threading;

import io.papermc.paper.chunk.system.RegionizedPlayerChunkLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class ShreddedPaperPlayerTicker {

    public static void tickPlayer(ServerPlayer serverPlayer) {
        serverPlayer.connection.connection.tick();
        final RegionizedPlayerChunkLoader.PlayerChunkLoaderData loader = serverPlayer.chunkLoader;
        if (loader != null) {
            loader.update(); // can't invoke plugin logic
            loader.updateQueues(System.nanoTime());
        }
    }

}
