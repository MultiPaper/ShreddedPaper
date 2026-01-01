package io.multipaper.shreddedpaper.region;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Location;
import org.slf4j.Logger;
import io.multipaper.shreddedpaper.config.ShreddedPaperConfiguration;

// Copy of ChunkPos basically, but separate to avoid accidental usage as a ChunkPos
public class RegionPos {

    private static final Logger LOGGER = LogUtils.getClassLogger();

    public static final int REGION_SIZE; // eg 8 (for an 8x8 region)
    public static final int REGION_SHIFT; // eg 3 (1 << 3 == 8)
    public static final int REGION_SIZE_MASK; // eg 7 (9 % 8 == 9 & 7 == 1)
    public static final int MAX_DISTANCE_SQR;

    static {
        // desiredRegionSize = 7 -> shift = 3, size = 8, mask = 7
        // desiredRegionSize = 8 -> shift = 3, size = 8, mask = 7
        // desiredRegionSize = 9 -> shift = 4, size = 16, mask = 15
        int desiredRegionSize = ShreddedPaperConfiguration.get().multithreading.regionSize - 1;
        int shift = 0;
        while (desiredRegionSize > 0) {
            shift++;
            desiredRegionSize >>= 1;
        }

        REGION_SIZE = 1 << shift;
        REGION_SHIFT = shift;
        REGION_SIZE_MASK = (1 << shift) - 1;

        if (REGION_SIZE <= 1) {
            throw new IllegalStateException("!!! Region size is " + REGION_SIZE + " chunk, this is too small. It must be at least 2 chunks !!!");
        }

        if (REGION_SIZE < 8) {
            LOGGER.warn("!!! !!!");
            LOGGER.warn("!!! Region size is less than 8 chunks, this will cause issues unless you know what you're doing!!!");
            LOGGER.warn("!!! !!!");
        }

        LOGGER.info("Using region size: {}, shift={}, mask={}", REGION_SIZE, REGION_SHIFT, REGION_SIZE_MASK);

        MAX_DISTANCE_SQR = RegionPos.REGION_SIZE * 16 * RegionPos.REGION_SIZE * 16;
    }

    public final int x;
    public final int z;
    public final long longKey; // Paper

    public RegionPos(int rx, int rz) {
        this.x = rx;
        this.z = rz;
        this.longKey = RegionPos.asLong(this.x, this.z);
    }

    public RegionPos(long regionKey) {
        this.x = (int) regionKey;
        this.z = (int) (regionKey >> 32);
        this.longKey = regionKey;
    }

    public static RegionPos forChunk(ChunkPos chunkPos) {
        return chunkPos.getRegionPos(); // Cache the RegionPos on the ChunkPos to minimize object creation
    }

    public static RegionPos forChunk(int chunkX, int chunkZ) {
        return new RegionPos(asLongForChunk(chunkX, chunkZ));
    }

    public static RegionPos forBlockPos(BlockPos blockPos) {
        return forBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static RegionPos forBlockPos(int x, int y, int z) {
        return new RegionPos(asLongForBlockPos(x, y, z));
    }

    public static RegionPos forLocation(Location location) {
        return forBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static long asLongForChunk(ChunkPos chunkPos) {
        return asLongForChunk(chunkPos.x, chunkPos.z);
    }

    public static long asLongForChunk(int chunkX, int chunkZ) {
        return asLong(chunkX >> REGION_SHIFT, chunkZ >> REGION_SHIFT);
    }

    public static long asLongForBlockPos(BlockPos blockPos) {
        return asLongForBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static long asLongForBlockPos(int x, int y, int z) {
        return asLongForChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
    }

    public static long asLong(RegionPos regionPos) {
        return asLong(regionPos.x, regionPos.z);
    }

    public static long asLong(int regionX, int regionZ) {
        return (long)regionX & 4294967295L | ((long)regionZ & 4294967295L) << 32;
    }

    public int getLowerChunkX() {
        return this.x << REGION_SHIFT;
    }

    public int getLowerChunkZ() {
        return this.z << REGION_SHIFT;
    }

    public int getUpperChunkX() {
        return getLowerChunkX() + REGION_SIZE - 1;
    }

    public int getUpperChunkZ() {
        return getLowerChunkZ() + REGION_SIZE - 1;
    }

    @Override
    public int hashCode() {
        return ChunkPos.hash(this.x, this.z);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof RegionPos regionPos && this.x == regionPos.x && this.z == regionPos.z;
    }

    @Override
    public String toString() {
        return "RegionPos[" + this.x + ", " + this.z + "]";
    }
}
