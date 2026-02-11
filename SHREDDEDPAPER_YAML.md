# shreddedpaper.yml

```yaml

# Multithreading settings
multithreading:

  # The number of threads to use for ticking chunks. `-1` defaults to the number
  # of available processors subtract one, or 1, whichever is larger.
  # Set to `1` to essentially disable multi-threading.
  thread-count: -1
  
  # The size of a region in chunks. Chunks are grouped into regions when
  # ticking, and neighbouring regions are locked so any changes into the
  # neighbouring region do not step on other threads.
  # The larger the value, the more performant the server will be, but the more
  # likely the work is not evenly distributed between threads. Too small, and
  # threads may accidentally try accessing chunks of another thread.
  # A minimum of 8 chunks is recommended, as lightning rods have the largest
  # search radius at 8 chunks.
  # Must be a power of 2.
  region-size: 8
  
  # Whether to run plugins' code without 'folia-supported: true' in sync. This
  # will give unsupported plugins a better chance of working, but is not
  # guaranteed. If false, or if the plugin is supported, all the plugin's code
  # will run asynchronously in the ticking region worker threads.
  run-unsupported-plugins-in-sync: true
  
  # Can unsupported plugins modify blocks/entities/etc from the global scheduler
  # This will be false if there are multiple servers as tasks in the global
  # scheduler do not have the required region locks. These locks aren't needed
  # for single server instances.
  allow-unsupported-plugins-to-modify-chunks-via-global-scheduler: true

# Threading performance monitoring settings
threading:
  performance-monitor:
    # Whether the thread performance monitor is enabled
    enabled: true
    
    # How often to update thread performance metrics (in milliseconds)
    update-interval: 5000
    
    # CPU usage percentage threshold for warning
    cpu-warning-threshold: 80.0
    
    # Memory usage percentage threshold for warning
    memory-warning-threshold: 75.0
    
    # Chunk processing rate threshold for warning (chunks per second)
    chunk-processing-warning-threshold: 50
    
    # How long to retain performance history (in seconds)
    retain-history-seconds: 300
    
    # Whether to enable detailed logging of performance metrics
    detailed-logging: false

# ShreddedPaper's optimizations settings
optimizations:
  
  # Check entity activation range less often. Spigot does this every tick
  # unnecessarily. Set to '0' to disable.
  entity-activation-check-frequency: 20
  
  # Disable Bukkit's vanish api. Set to 'true' to increase performance. This
  # will break plugins using this api.
  disable-vanish-api: false
  
  # Don't wake up the event loop thread when not flushing packets. Set to
  # 'true' to increase performance. 
  use-lazy-execute-when-not-flushing: true

  # Flush the players' connection queues in parallel. Set to 'true' to increase
  # performance.
  flush-queue-in-parallel: true
  
  # Process the track queue in parallel. Set to 'true' to increase performance.
  process-track-queue-in-parallel: true
  
  # Maximum number of players to render each entity to. Can be bypassed with the
  # permission 'shreddedpaper.maximumtrackerbypass'
  maximum-trackers-per-entity: 500

  # How often (in ticks) to do a full tracker update per entity. The vanilla
  # default is '1'.
  tracker-full-update-frequency: 20

  # Cache full chunk packets so that we aren't recreating them every time the
  # same chunk is sent to a new player. Useful when lots of players are in the
  # same area.
  chunk-packet-caching:
    enabled: true
    
    # How long to keep the cached packets in ticks. Cached packets automatically
    # expire if the chunk is modified.
    expire-after: 1200
    
    # Use soft references for the cached packets. This will mean the garbage
    # collector will reclaim the memory before an OutOfMemoryError occurs,
    # but will leave the cache alive as long as possible.
    # Set to 'false' to use weak references, which will be collected by the
    # garbage collector much sooner than soft references.
    use-soft-references: true

```