package io.multipaper.shreddedpaper.region;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.Validate;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

public class ShreddedPaperRegionSchedulerApiImpl implements RegionScheduler {


    @Override
    public void execute(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Runnable run) {
        run(plugin, world, chunkX, chunkZ, task -> run.run());
    }

    @Override
    public @NotNull ScheduledTask run(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<ScheduledTask> task) {
        return runDelayed(plugin, world, chunkX, chunkZ, task, 1);
    }

    @Override
    public @NotNull ScheduledTask runDelayed(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<ScheduledTask> task, long delayTicks) {
        return runAtFixedRate(plugin, world, chunkX, chunkZ, task, delayTicks, -1);
    }

    @Override
    public @NotNull ScheduledTask runAtFixedRate(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        Validate.notNull(plugin, "Plugin may not be null");
        Validate.notNull(world, "World may not be null");
        Validate.notNull(task, "Task may not be null");
        if (initialDelayTicks <= 0) {
            throw new IllegalArgumentException("Initial delay ticks may not be <= 0");
        }
        if (periodTicks == 0) {
            throw new IllegalArgumentException("Period ticks may not be <= 0");
        }

        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register task while disabled");
        }

        return new RegionScheduledTask(plugin, world, chunkX, chunkZ, task, initialDelayTicks, periodTicks);
    }

    private class RegionScheduledTask implements ScheduledTask, Runnable {

        private final Plugin plugin;
        private final ServerLevel serverLevel;
        private final RegionPos regionPos;
        private final Consumer<ScheduledTask> task;
        private final long periodTicks;
        private final AtomicReference<ExecutionState> executionState = new AtomicReference<>(ExecutionState.IDLE);

        public RegionScheduledTask(Plugin plugin, World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
            this.plugin = plugin;
            this.serverLevel = ((CraftWorld) world).getHandle();
            this.task = task;
            this.periodTicks = periodTicks;
            this.regionPos = RegionPos.forChunk(new ChunkPos(chunkX, chunkZ));

            schedule(delayTicks);
        }

        private void schedule(long delayTicks) {
            serverLevel.getChunkSource().tickingRegions.scheduleTask(regionPos, this, delayTicks);
        }

        @Override
        public @NotNull Plugin getOwningPlugin() {
            return plugin;
        }

        @Override
        public boolean isRepeatingTask() {
            return periodTicks > 0;
        }

        @Override
        public @NotNull CancelledState cancel() {
            if (executionState.compareAndSet(ExecutionState.IDLE, ExecutionState.CANCELLED)) {
                return CancelledState.CANCELLED_BY_CALLER;
            }
            if (executionState.compareAndSet(ExecutionState.RUNNING, ExecutionState.CANCELLED_RUNNING)) {
                if (isRepeatingTask()) {
                    return CancelledState.NEXT_RUNS_CANCELLED;
                } else {
                    return CancelledState.RUNNING;
                }
            }
            return switch (executionState.get()) {
                case IDLE, RUNNING -> {
                    executionState.set(ExecutionState.CANCELLED);
                    yield CancelledState.CANCELLED_BY_CALLER;
                }
                case CANCELLED -> CancelledState.CANCELLED_ALREADY;
                case CANCELLED_RUNNING -> CancelledState.NEXT_RUNS_CANCELLED_ALREADY;
                case FINISHED -> CancelledState.ALREADY_EXECUTED;
            };
        }

        @Override
        public @NotNull ExecutionState getExecutionState() {
            return executionState.get();
        }

        @Override
        public void run() {
            if (!getOwningPlugin().isEnabled()) {
                executionState.set(ExecutionState.CANCELLED);
                return;
            }

            if (!executionState.compareAndSet(ExecutionState.IDLE, ExecutionState.RUNNING)) {
                return;
            }

            try {
                task.accept(this);
            } catch (Throwable throwable) {
                this.plugin.getLogger().log(Level.WARNING, "Region task for " + this.plugin.getDescription().getFullName() + " generated an exception", throwable);
            } finally {
                executionState.compareAndSet(ExecutionState.RUNNING, isRepeatingTask() ? ExecutionState.IDLE : ExecutionState.FINISHED);
                executionState.compareAndSet(ExecutionState.CANCELLED_RUNNING, ExecutionState.CANCELLED);

                if (isRepeatingTask()) {
                    schedule(periodTicks);
                }
            }
        }
    }
}

