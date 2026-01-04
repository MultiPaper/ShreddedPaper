package io.multipaper.shreddedpaper.threading;

import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader;
import net.minecraft.server.level.ServerPlayer;

public class ShreddedPaperPlayerTicker {

    public static void tickPlayer(ServerPlayer serverPlayer) {
        serverPlayer.connection.connection.tick();
        final RegionizedPlayerChunkLoader.PlayerChunkLoaderData loader = serverPlayer.moonrise$getChunkLoader();
        if (loader != null) {
            loader.update(); // can't invoke plugin logic
            loader.updateQueues(System.nanoTime());
        }
    }

}
