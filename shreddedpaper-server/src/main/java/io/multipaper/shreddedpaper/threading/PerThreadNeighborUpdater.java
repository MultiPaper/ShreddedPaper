package io.multipaper.shreddedpaper.threading;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class PerThreadNeighborUpdater implements NeighborUpdater {

    private final ThreadLocal<NeighborUpdater> threadLocalNeighborUpdater;

    public PerThreadNeighborUpdater(Supplier<NeighborUpdater> factory) {
        this.threadLocalNeighborUpdater = ThreadLocal.withInitial(factory);
    }

    private NeighborUpdater getOrCreate() {
        return threadLocalNeighborUpdater.get();
    }

    @Override
    public void shapeUpdate(@NotNull Direction direction, @NotNull BlockState neighborState, @NotNull BlockPos pos, @NotNull BlockPos neighborPos, int flags, int maxUpdateDepth) {
        getOrCreate().shapeUpdate(direction, neighborState, pos, neighborPos, flags, maxUpdateDepth);
    }

    @Override
    public void neighborChanged(@NotNull BlockPos pos, @NotNull Block sourceBlock, @NotNull BlockPos sourcePos) {
        getOrCreate().neighborChanged(pos, sourceBlock, sourcePos);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull BlockPos pos, @NotNull Block sourceBlock, @NotNull BlockPos sourcePos, boolean notify) {
        getOrCreate().neighborChanged(state, pos, sourceBlock, sourcePos, notify);
    }

    @Override
    public void updateNeighborsAtExceptFromFacing(@NotNull BlockPos pos, @NotNull Block sourceBlock, @Nullable Direction except) {
        getOrCreate().updateNeighborsAtExceptFromFacing(pos, sourceBlock, except);
    }
}
