package io.multipaper.shreddedpaper.threading;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

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
    public void shapeUpdate(Direction direction, BlockState state, BlockPos pos, BlockPos neighborPos, @Block.UpdateFlags int flags, int recursionLeft) {
        getOrCreate().shapeUpdate(direction, state, pos, neighborPos, flags, recursionLeft);
    }

    @Override
    public void neighborChanged(BlockPos pos, Block neighborBlock, @Nullable Orientation orientation) {
        getOrCreate().neighborChanged(pos, neighborBlock, orientation);
    }

    @Override
    public void neighborChanged(BlockState state, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        getOrCreate().neighborChanged(state, pos, neighborBlock, orientation, movedByPiston);
    }

    @Override
    public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block block, @Nullable Direction facing, @Nullable Orientation orientation) {
        getOrCreate().updateNeighborsAtExceptFromFacing(pos, block, facing, orientation);
    }
}
