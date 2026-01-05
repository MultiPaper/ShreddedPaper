package io.multipaper.shreddedpaper.config;

import io.papermc.paper.configuration.ConfigurationPart;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

@SuppressWarnings({ "InnerClassMayBeStatic" })
public class ShreddedPaperConfiguration extends ConfigurationPart {

    public static final String HEADER = """
            This is the main configuration file for ShreddedPaper.
            There's quite alot to configure. Read the docs for more information.

            Docs: https://github.com/MultiPaper/ShreddedPaper/blob/main/SHREDDEDPAPER_YAML.md\s
            """;

    private static ShreddedPaperConfiguration instance;

    public static ShreddedPaperConfiguration get() {
        return instance;
    }

    static void set(ShreddedPaperConfiguration instance) {
        ShreddedPaperConfiguration.instance = instance;
    }

    public Multithreading multithreading;

    public class Multithreading extends ConfigurationPart {

        public int threadCount = -1;
        public int regionSize = 8;
        public boolean runUnsupportedPluginsInSync = true;
        public boolean allowUnsupportedPluginsToModifyChunksViaGlobalScheduler = true;

    }

    public Optimizations optimizations;

    public class Optimizations extends ConfigurationPart {

        public int entityActivationCheckFrequency = 20;
        public boolean disableVanishApi = false;
        public boolean disableLocatorBar = true;
        public boolean useLazyExecuteWhenNotFlushing = true;
        public boolean processTrackQueueInParallel = true;
        public boolean flushQueueInParallel = true;
        public int maximumTrackersPerEntity = 500;
        public long trackerFullUpdateFrequency = 20;
        public boolean writePlayerSavesAsync = true;
        public ChunkPacketCaching chunkPacketCaching;

        public class ChunkPacketCaching extends ConfigurationPart {

            public boolean enabled = true;
            public boolean useSoftReferences = true;
            public long expireAfter = 1200;

        }

    }


}
