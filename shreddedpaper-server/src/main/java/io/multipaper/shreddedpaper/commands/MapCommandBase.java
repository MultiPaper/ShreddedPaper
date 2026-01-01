package io.multipaper.shreddedpaper.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class MapCommandBase extends Command {

    public MapCommandBase(String command) {
        super(command);
    }

    protected abstract ChunkStatus getStatus(Player player, ChunkPos chunkPos);

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (!testPermission(sender)) return false;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return false;
        }

        sendMap(player);

        return true;
    }

    protected void sendMap(Player player) {
        sendMap(player, 8);
    }

    protected void sendMap(Player player, int radius) {
        sendHeader(player, radius);
        for (int row = -radius; row <= radius; row++) {
            sendRow(player, radius, row);
        }
        sendHeader(player, radius);
    }

    private ChunkStatus getStatus(Player player, int x, int z) {
        return getStatus(player, new ChunkPos(((CraftPlayer) player).getHandle().blockPosition().offset(x << 4, 0, z << 4)));
    }

    private void sendRow(Player player, int radius, int row) {
        Component component = Component.text(" | ").color(NamedTextColor.GOLD);

        int i = -radius;
        while (i <= radius) {
            StringBuilder builder = new StringBuilder();
            ChunkStatus status = getStatus(player, i, row);

            while (i <= radius) {
                ChunkStatus status2 = getStatus(player, i, row);

                if (!Objects.equals(status, status2)) {
                    break;
                }

                builder.append(row == 0 && i == 0 ? "\u25A0 " : "+ ");

                i++;
            }

            Component innerComponent = Component.text(builder.toString()).color(status.color());

            if (status.description() != null) {
                innerComponent = innerComponent.hoverEvent(HoverEvent.showText(Component.text(status.description()).color(status.color())));
            }

            component = component.append(innerComponent);
        }

        component = component.append(Component.text("| ").color(NamedTextColor.GOLD));
        player.sendMessage(component);
    }

    private void sendHeader(Player player, int radius) {
        player.sendMessage(Component.text("+ " + "- ".repeat(radius * 2 + 1) + "+").color(NamedTextColor.GOLD));
    }

    protected record ChunkStatus(NamedTextColor color, @Nullable String description) {};
}
