package io.multipaper.shreddedpaper.threading;

import com.mojang.logging.LogUtils;
import io.multipaper.shreddedpaper.region.RegionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ShreddedPaperDimensionChanger {

//    private static final Logger LOGGER = LogUtils.getClassLogger();
//
//    private final Entity entity;
//    private ServerLevel destinationWorld;
//    private final PlayerTeleportEvent.TeleportCause cause;
//
//    public ShreddedPaperDimensionChanger(Entity entity, ServerLevel destinationWorld, PlayerTeleportEvent.TeleportCause cause) {
//        this.entity = entity;
//        this.destinationWorld = destinationWorld;
//        this.cause = cause;
//
//        this.stage1();
//    }
//
//    private void ensureSyncWith(ServerLevel level, ChunkPos pos, Runnable runnable) {
//        if (!TickThread.isTickThreadFor(this.entity) || !TickThread.isTickThreadFor(level, pos)) {
//            ShreddedPaperRegionScheduler.scheduleAcrossLevels((ServerLevel) this.entity.level(), RegionPos.forChunk(this.entity.chunkPosition()), level, RegionPos.forChunk(pos), runnable);
//        } else {
//            runnable.run();
//        }
//    }
//
//    private void stage1() {
//        if (this.destinationWorld == null) {
//            return;
//        }
//
//        if (this.entity instanceof ServerPlayer serverPlayer && serverPlayer.isSleeping()) {
//            return;
//        }
//
//        if (isPlayerExitingEnd()) {
//            return;
//        }
//
//        CompletableFuture<PortalInfo> future = new CompletableFuture<>();
//        this.findDimensionEntryPoint(future);
//
//        future.thenAccept(portalInfo -> {
//            if (portalInfo == null) {
//                this.stage2(portalInfo);
//            } else {
//                this.ensureSyncWith(this.destinationWorld, new ChunkPos(BlockPos.containing(portalInfo.pos)), () -> {
//                    try {
//                        this.stage2(portalInfo);
//                    } catch (Exception e) {
//                        LOGGER.error("Failed to teleport entity through portal", e);
//                    }
//                });
//            }
//        })
//        .exceptionally(e -> {
//            LOGGER.error("Failed to find dimension entry point", e);
//            return null;
//        });
//    }
//
//    private boolean isPlayerExitingEnd() {
//        if (this.entity instanceof ServerPlayer serverPlayer && this.entity.level().getTypeKey() == LevelStem.END && this.destinationWorld.getTypeKey() == LevelStem.OVERWORLD) {
//            serverPlayer.isChangingDimension = true; // CraftBukkit - Moved down from above
//            serverPlayer.unRide();
//            serverPlayer.serverLevel().removePlayerImmediately(serverPlayer, Entity.RemovalReason.CHANGED_DIMENSION);
//            if (!serverPlayer.wonGame) {
//                if (serverPlayer.level().paperConfig().misc.disableEndCredits)
//                    serverPlayer.seenCredits = true; // Paper - Option to disable end credits
//                serverPlayer.wonGame = true;
//                serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, serverPlayer.seenCredits ? 0.0F : 1.0F));
//                serverPlayer.seenCredits = true;
//            }
//            return true;
//        }
//
//        return false;
//    }
//
//    private void findDimensionEntryPoint(CompletableFuture<PortalInfo> future) {
//        try {
//            boolean fromEndToOverworld = this.entity.level().getTypeKey() == LevelStem.END && this.destinationWorld.getTypeKey() == LevelStem.OVERWORLD;
//            boolean destinationIsEnd = this.destinationWorld.getTypeKey() == LevelStem.END;
//            boolean destinationIsNether = this.destinationWorld.getTypeKey() == LevelStem.NETHER;
//
//            if (!fromEndToOverworld && !destinationIsEnd) {
//
//                if (this.entity.level().getTypeKey() != LevelStem.NETHER && !destinationIsNether) { // CraftBukkit
//                    future.complete(null);
//                }
//
//                WorldBorder worldborder = this.destinationWorld.getWorldBorder();
//                double d0 = DimensionType.getTeleportationScale(this.entity.level().dimensionType(), this.destinationWorld.dimensionType());
//                BlockPos blockposition = worldborder.clampToBounds(this.entity.getX() * d0, this.entity.getY(), this.entity.getZ() * d0);
//
//                int portalSearchRadius = this.destinationWorld.paperConfig().environment.portalSearchRadius;
//                if (this.entity.level().paperConfig().environment.portalSearchVanillaDimensionScaling && destinationIsNether) { // == THE_NETHER
//                    portalSearchRadius = (int) (portalSearchRadius / this.destinationWorld.dimensionType().coordinateScale());
//                }
//
//                callPortalEvent(blockposition, portalSearchRadius).thenAccept(event -> {
//                    if (event == null) {
//                        future.complete(null);
//                        return;
//                    }
//
//                    this.destinationWorld = ((CraftWorld) event.getTo().getWorld()).getHandle();
//
//                    ensureSyncWith(this.destinationWorld, new ChunkPos(CraftLocation.toBlockPosition(event.getTo())), () -> {
//                        try {
//                            future.complete(this.getExitPortal(event));
//                        } catch (Exception e) {
//                            future.completeExceptionally(e);
//                        }
//                    });
//                });
//            } else {
//                BlockPos blockposition1 = destinationIsEnd ? ServerLevel.END_SPAWN_POINT : this.destinationWorld.getSharedSpawnPos();
//
//                this.destinationWorld.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(blockposition1), 3, blockposition1);
//
//                getSpawnYValue(blockposition1, destinationIsEnd).thenAccept(i -> {
//                    callPortalEvent(blockposition1, 16).thenAccept(event -> {
//                        if (event == null) {
//                            future.complete(null);
//                            return;
//                        }
//
//                        this.destinationWorld = ((CraftWorld) event.getTo().getWorld()).getHandle();
//
//                        future.complete(new PortalInfo(new Vec3(event.getTo().getX(), i, event.getTo().getZ()), this.entity.getDeltaMovement(), this.entity.getYRot(), this.entity.getXRot(), this.destinationWorld, event));
//                    });
//                });
//            }
//        } catch (Exception e) {
//            future.completeExceptionally(e);
//        }
//    }
//
//    private CompletableFuture<Integer> getSpawnYValue(BlockPos blockposition1, boolean destinationIsEnd) {
//        if (destinationIsEnd) {
//            return CompletableFuture.completedFuture(blockposition1.getY());
//        } else {
//            return this.destinationWorld.getWorld().getChunkAtAsyncUrgently(CraftLocation.toBukkit(blockposition1, this.destinationWorld))
//                    .thenApply(chunk -> this.destinationWorld.getChunkAt(blockposition1).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockposition1.getX(), blockposition1.getZ()) + 1);
//        }
//    }
//
//    private PortalInfo getExitPortal(CraftPortalEvent event) {
//        WorldBorder worldborder = this.destinationWorld.getWorldBorder();
//        BlockPos blockposition = worldborder.clampToBounds(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
//        boolean destinationIsNether = this.destinationWorld.getTypeKey() == LevelStem.NETHER;
//
//        return this.entity.getExitPortal(this.destinationWorld, blockposition, destinationIsNether, worldborder, event.getSearchRadius(), event.getCanCreatePortal(), event.getCreationRadius()).map((blockutil_rectangle) -> {
//            // CraftBukkit end
//            BlockState iblockdata = this.entity.level().getBlockState(this.entity.portalEntrancePos);
//            Direction.Axis enumdirection_enumaxis;
//            Vec3 vec3d;
//
//            if (iblockdata.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
//                enumdirection_enumaxis = iblockdata.getValue(BlockStateProperties.HORIZONTAL_AXIS);
//                BlockUtil.FoundRectangle blockutil_rectangle1 = BlockUtil.getLargestRectangleAround(this.entity.portalEntrancePos, enumdirection_enumaxis, 21, Direction.Axis.Y, 21, (blockposition1) -> {
//                    return this.entity.level().getBlockState(blockposition1) == iblockdata;
//                });
//
//                vec3d = this.entity.getRelativePortalPosition(enumdirection_enumaxis, blockutil_rectangle1);
//            } else {
//                enumdirection_enumaxis = Direction.Axis.X;
//                vec3d = new Vec3(0.5D, 0.0D, 0.0D);
//            }
//
//            return PortalShape.createPortalInfo(this.destinationWorld, blockutil_rectangle, enumdirection_enumaxis, vec3d, this.entity, this.entity.getDeltaMovement(), this.entity.getYRot(), this.entity.getXRot(), event); // CraftBukkit
//        }).orElse(null); // CraftBukkit - decompile error
//    }
//
//    private CompletableFuture<CraftPortalEvent> callPortalEvent(BlockPos blockposition, int portalSearchRadius) {
//        return CompletableFuture.supplyAsync(() -> {
//            return this.entity.callPortalEvent(this.entity, this.destinationWorld, new Vec3(blockposition.getX(), blockposition.getY(), blockposition.getZ()), this.cause, portalSearchRadius, this.destinationWorld.paperConfig().environment.portalCreateRadius);
//        }, r -> this.ensureSyncWith(this.destinationWorld, new ChunkPos(blockposition), r));
//    }
//
//    private void stage2(PortalInfo portalInfo) {
//        if (portalInfo == null) {
//            return;
//        }
//
//        if (this.entity instanceof ServerPlayer serverPlayer) {
//            if (this.entity.level().getTypeKey() == LevelStem.OVERWORLD && this.destinationWorld.getTypeKey() == LevelStem.NETHER) {
//                serverPlayer.enteredNetherPosition = this.entity.position();
//            } else if (this.destinationWorld.getTypeKey() == LevelStem.END && portalInfo.portalEventInfo != null && portalInfo.portalEventInfo.getCanCreatePortal()) { // CraftBukkit
//                serverPlayer.createEndPlatform(this.destinationWorld, BlockPos.containing(portalInfo.pos));
//            }
//        }
//
//        Location exit = CraftLocation.toBukkit(portalInfo.pos, this.destinationWorld.getWorld(), portalInfo.yRot, portalInfo.xRot);
//        Vec3 speed;
//        if (this.entity instanceof ServerPlayer serverPlayer) {
//            Location enter = serverPlayer.getBukkitEntity().getLocation();
//            PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(serverPlayer.getBukkitEntity(), enter, exit, cause);
//            Bukkit.getServer().getPluginManager().callEvent(tpEvent);
//            if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
//                return;
//            }
//            exit = tpEvent.getTo();
//            speed = null;
//        } else {
//            CraftEntity bukkitEntity = this.entity.getBukkitEntity();
//            org.bukkit.event.entity.EntityPortalExitEvent event = new org.bukkit.event.entity.EntityPortalExitEvent(bukkitEntity,
//                    bukkitEntity.getLocation(), exit,
//                    bukkitEntity.getVelocity(), org.bukkit.craftbukkit.util.CraftVector.toBukkit(portalInfo.speed));
//            event.callEvent();
//            if (this.entity.isRemoved() || event.isCancelled() || event.getTo() == null) {
//                return; // ShreddedPaper
//            }
//            exit = event.getTo();
//            speed = org.bukkit.craftbukkit.util.CraftVector.toNMS(event.getAfter());
//        }
//
//        this.destinationWorld = ((CraftWorld) exit.getWorld()).getHandle();
//
//        Location finalExit = exit;
//        this.ensureSyncWith(this.destinationWorld, new ChunkPos(CraftLocation.toBlockPosition(finalExit)), () -> stage3(finalExit, speed));
//    }
//
//    private void stage3(Location exit, Vec3 speed) {
//        if (this.entity instanceof ServerPlayer serverPlayer) {
//            serverPlayer.isChangingDimension = true; // CraftBukkit - Set teleport invulnerability only if player changing worlds
//
//            serverPlayer.connection.send(new ClientboundRespawnPacket(serverPlayer.createCommonSpawnInfo(this.destinationWorld), (byte) 3));
//            serverPlayer.connection.send(new ClientboundChangeDifficultyPacket(this.destinationWorld.getDifficulty(), this.entity.level().getLevelData().isDifficultyLocked())); // Paper - per level difficulty
//            PlayerList playerlist = serverPlayer.server.getPlayerList();
//
//            playerlist.sendPlayerPermissionLevel(serverPlayer);
//            serverPlayer.serverLevel().removePlayerImmediately(serverPlayer, Entity.RemovalReason.CHANGED_DIMENSION);
//            serverPlayer.unsetRemoved();
//            serverPlayer.portalPos = io.papermc.paper.util.MCUtil.toBlockPosition(exit); // Purpur
//
//            // CraftBukkit end
//            serverPlayer.setServerLevel(this.destinationWorld);
//            serverPlayer.connection.teleport(exit); // CraftBukkit - use internal teleport without event
//            serverPlayer.connection.resetPosition();
//            this.destinationWorld.addDuringPortalTeleport(serverPlayer);
//            //worldserver1.getProfiler().pop(); // Purpur
//            serverPlayer.triggerDimensionChangeTriggers(this.destinationWorld);
//            serverPlayer.connection.send(new ClientboundPlayerAbilitiesPacket(serverPlayer.getAbilities()));
//            playerlist.sendLevelInfo(serverPlayer, this.destinationWorld);
//            playerlist.sendAllPlayerInfo(serverPlayer);
//
//            for (MobEffectInstance mobeffect : serverPlayer.getActiveEffects()) {
//                serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(serverPlayer.getId(), mobeffect, false));
//            }
//
//            serverPlayer.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
//            serverPlayer.lastSentExp = -1;
//            serverPlayer.lastSentHealth = -1.0F;
//            serverPlayer.lastSentFood = -1;
//
//            // CraftBukkit start
//            PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(serverPlayer.getBukkitEntity(), this.destinationWorld.getWorld());
//            serverPlayer.level().getCraftServer().getPluginManager().callEvent(changeEvent);
//            // CraftBukkit end
//        } else {
//            if (this.destinationWorld == this.entity.level()) {
//                // SPIGOT-6782: Just move the entity if a plugin changed the world to the one the entity is already in
//                this.entity.moveTo(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch());
//                this.entity.setDeltaMovement(speed);
//                return;
//            }
//
//            this.entity.unRide();
//
//            if (this.entity instanceof Mob mob) {
//                mob.dropLeash(true, true); // Paper drop lead
//            }
//
//            Entity entity = this.entity.getType().create(this.destinationWorld);
//
//            if (entity != null) {
//                entity.restoreFrom(this.entity);
//                entity.moveTo(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch()); // Paper - EntityPortalExitEvent
//                entity.setDeltaMovement(speed); // Paper - EntityPortalExitEvent
//                // CraftBukkit start - Don't spawn the new entity if the current entity isn't spawned
//                if (this.entity.inWorld) {
//                    this.destinationWorld.addDuringTeleport(entity);
//                    if (this.destinationWorld.getTypeKey() == LevelStem.END) { // CraftBukkit
//                        ServerLevel.makeObsidianPlatform(this.destinationWorld, this.entity); // CraftBukkit
//                    }
//                }
//                // CraftBukkit end
//                // // CraftBukkit start - Forward the CraftEntity to the new entity // Paper - Forward CraftEntity in teleport command; moved to Entity#restoreFrom
//                // this.getBukkitEntity().setHandle(entity);
//                // entity.bukkitEntity = this.getBukkitEntity();
//                // // CraftBukkit end
//            }
//
//            this.entity.removeAfterChangingDimensions();
//            ((ServerLevel) this.entity.level()).resetEmptyTime();
//            this.destinationWorld.resetEmptyTime();
//        }
//    }

}
