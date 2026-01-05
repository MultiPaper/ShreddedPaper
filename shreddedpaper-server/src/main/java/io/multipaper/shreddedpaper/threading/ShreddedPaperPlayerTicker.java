package io.multipaper.shreddedpaper.threading;

import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class ShreddedPaperPlayerTicker {

    public static void tickPlayer(ServerPlayer serverPlayer) {
        // ShreddedPaper - Execute scheduled tasks on the player's task scheduler (e.g., packet handlers)
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        if (craftPlayer != null) {
            craftPlayer.taskScheduler.executeTick();
        }

        serverPlayer.connection.connection.tick();
        final RegionizedPlayerChunkLoader.PlayerChunkLoaderData loader = serverPlayer.moonrise$getChunkLoader();
        if (loader != null) {
            loader.update(); // can't invoke plugin logic
            loader.updateQueues(System.nanoTime());
        }
    }

}
