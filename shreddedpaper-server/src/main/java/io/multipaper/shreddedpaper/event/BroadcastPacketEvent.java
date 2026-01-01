package io.multipaper.shreddedpaper.event;

import net.minecraft.network.protocol.Packet;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called whenever a packet is broadcasted to players, but will not be sent to
 * the player that initiates the packet. Will be called even if no players will
 * receive the packet.
 */
@ApiStatus.Experimental
public class BroadcastPacketEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final World world;
    private final Packet<?> packet;
    private final Entity source;

    public BroadcastPacketEvent(@NotNull World world, @NotNull Packet<?> packet, @Nullable Entity source) {
        this.world = world;
        this.packet = packet;
        this.source = source;
    }

    /**
     * The world the packet is being broadcasted in.
     */
    @NotNull
    public World getWorld() {
        return world;
    }

    /**
     * The packet being broadcasted.
     */
    @NotNull
    public Packet<?> getPacket() {
        return packet;
    }

    /**
     * Gets the entity that triggered this packet to be broadcasted. May be null
     * if the packet was not triggered by an entity.
     */
    @Nullable
    public Entity getSource() {
        return source;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
