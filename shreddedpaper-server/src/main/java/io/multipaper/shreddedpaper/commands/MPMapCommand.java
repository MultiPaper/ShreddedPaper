package io.multipaper.shreddedpaper.commands;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class MPMapCommand extends MapCommandBase {

    public MPMapCommand(String command) {
        super(command);
        setPermission("shreddedpaper.command.mpmap");
    }

    @Override
    protected ChunkStatus getStatus(Player player, ChunkPos chunkPos) {
        NewChunkHolder newChunkHolder = ((ServerLevel) ((CraftPlayer) player).getHandle().level()).moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(chunkPos.x, chunkPos.z);

        NamedTextColor color;
        String name;

        if (newChunkHolder == null) {
            color = NamedTextColor.DARK_GRAY;
            name = "Unloaded";
        } else if (newChunkHolder.getChunkStatus() == FullChunkStatus.INACCESSIBLE) {
            color = NamedTextColor.GRAY;
            name = "Inaccessible";
        } else if (newChunkHolder.getChunkStatus() == FullChunkStatus.FULL) {
            color = NamedTextColor.WHITE;
            name = "Full";
        } else if (newChunkHolder.getChunkStatus() == FullChunkStatus.BLOCK_TICKING) {
            color = NamedTextColor.BLUE;
            name = "Block Ticking";
        } else if (newChunkHolder.getChunkStatus() == FullChunkStatus.ENTITY_TICKING) {
            color = NamedTextColor.AQUA;
            name = "Entity Ticking";
        } else {
            color = NamedTextColor.RED;
            name = "Unknown";
        }

        return new ChunkStatus(color, name);
    }
}
