package io.multipaper.shreddedpaper.threading;

import ca.spottedleaf.moonrise.common.util.TickThread;
import com.mojang.logging.LogUtils;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import io.multipaper.shreddedpaper.config.ShreddedPaperConfiguration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronousPluginExecution {

    private static final Logger LOGGER = LogUtils.getClassLogger();

    private static final Map<String, List<String>> cachedDependencyLists = new ConcurrentHashMap<>(); // Does not support dynamic plugin reloading, but oh well
    private static final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private static final ThreadLocal<WeakReference<Plugin>> currentPlugin = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> heldPluginLocks = ThreadLocal.withInitial(ArrayList::new); // Use a list as the same plugin may be locked multiple times in a single thread due to recursion

    public static Plugin getCurrentPlugin() {
        WeakReference<Plugin> pluginRef = currentPlugin.get();
        return pluginRef == null ? null : pluginRef.get();
    }

    public static void executeNoException(Plugin plugin, RunnableWithException runnable) {
        try {
            execute(plugin, runnable);
        } catch (RuntimeException e) {
            throw e; // passthrough, no need to wrap it again
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void execute(Plugin plugin, RunnableWithException runnable) throws Exception {
        ShreddedPaperConfiguration config = ShreddedPaperConfiguration.get();
        if (plugin == null || config == null || !config.multithreading.runUnsupportedPluginsInSync || plugin.getDescription().isFoliaSupported() || TickThread.isShutdownThread()) {
            // Multi-thread safe plugin, run it straight away
            runnable.run();
            return;
        }

        // Lock the plugins in a predictable order to prevent deadlocks
        List<String> pluginsToLock = cachedDependencyLists.get(plugin.getName());

        if (pluginsToLock == null) {
            // computeIfAbsent requires an expensive synchronized call even if the value is already present, so check with a get first
            pluginsToLock = cachedDependencyLists.computeIfAbsent(plugin.getName(), (name) -> {
                TreeSet<String> dependencyList = new TreeSet<>(Comparator.naturalOrder());
                fillPluginsToLock(plugin, dependencyList);
                LOGGER.info("Plugin {} does not support Folia! Initializing synchronous execution. This may cause a performance degradation.", plugin.getName());
                LOGGER.info("Dependency list calculated for {}: {}", plugin.getName(), dependencyList);
                return new ArrayList<>(dependencyList);
            });
        }

        lock(pluginsToLock);

        WeakReference<Plugin> parentPlugin = currentPlugin.get();
        try {
            currentPlugin.set(new WeakReference<>(plugin));
            runnable.run();
        } finally {
            currentPlugin.set(parentPlugin);
            for (String pluginToLock : pluginsToLock) {
                getLock(pluginToLock).unlock();
                heldPluginLocks.get().remove(pluginToLock);
            }
        }
    }

    private static ReentrantLock getLock(String plugin) {
        ReentrantLock lock = locks.get(plugin);

        if (lock == null) {
            // computeIfAbsent requires an expensive synchronized call even if the value is already present, so check with a get first
            lock = locks.computeIfAbsent(plugin, (name) -> new ReentrantLock());
        }

        return lock;
    }

    private static void lock(List<String> pluginsToLock) {
        List<String> locksToAcquire = new ArrayList<>(pluginsToLock); // Must be a list as we can hold the same lock multiple times due to recursion

        if (!heldPluginLocks.get().isEmpty()) {
            // We already have some locks, take care to avoid a deadlock with another thread
            if (tryLockNow(locksToAcquire)) {
                heldPluginLocks.get().addAll(locksToAcquire);
                return;
            } else {
                // We failed to instantly acquire the lock, back off from all locks and try again to avoid a deadlock
                for (String plugin : heldPluginLocks.get()) {
                    getLock(plugin).unlock();
                    locksToAcquire.add(plugin);
                }
                heldPluginLocks.get().clear();
                Collections.sort(locksToAcquire); // Ensure we lock in a predictable order to avoid deadlocks
            }
        }

        for (String plugin : locksToAcquire) {
            getLock(plugin).lock();
            heldPluginLocks.get().add(plugin);
        }
    }

    private static boolean tryLockNow(List<String> locksToReacquire) {
        boolean success = true;

        List<ReentrantLock> locksAquired = new ArrayList<>();

        for (String plugin : locksToReacquire) {
            ReentrantLock lock = getLock(plugin);
            if (lock.tryLock()) {
                locksAquired.add(lock);
            } else {
                success = false;
                break;
            }
        }

        if (!success) {
            for (ReentrantLock lock : locksAquired) {
                lock.unlock();
            }
        }

        return success;
    }

    private static boolean fillPluginsToLock(Plugin plugin, TreeSet<String> pluginsToLock) {
        if (pluginsToLock.contains(plugin.getName())) {
            // Cyclic graphhhh
            return true;
        }

        if (plugin.getDescription().isFoliaSupported()) {
            // Multi-thread safe plugin, we don't need to lock it
            return false;
        }

        boolean hasDependency = false;

        for (String depend : plugin.getDescription().getDepend()) {
            Plugin dependPlugin = plugin.getServer().getPluginManager().getPlugin(depend);
            if (dependPlugin != null) {
                hasDependency |= fillPluginsToLock(dependPlugin, pluginsToLock);
            } else {
                LOGGER.warn("Could not find dependency " + depend + " for plugin " + plugin.getName() + " even though it is a required dependency - this code shouldn't've been able to be run!");
            }
        }

        for (String softDepend : plugin.getDescription().getSoftDepend()) {
            Plugin softDependPlugin = plugin.getServer().getPluginManager().getPlugin(softDepend);
            if (softDependPlugin != null) {
                hasDependency |= fillPluginsToLock(softDependPlugin, pluginsToLock);
            }
        }

        if (!hasDependency) {
            // Only add our own plugin if we have no dependencies to lock
            // If we have a dependency, there's no point in locking this plugin by itself as we'll always be locking the dependency anyway
            pluginsToLock.add(plugin.getName());
        }

        return true;
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

}
