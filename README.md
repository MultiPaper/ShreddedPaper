# ShreddedPaper

[![Discord](https://img.shields.io/discord/937309618743427113.svg?color=738ad6&label=Join%20the%20Discord%20server&logo=discord&logoColor=ffffff)](https://discord.gg/dN3WCZkSRV)

**ShreddedPaper is in public beta.** Most features work for most players most of
the time, however things can occasionally break.

1.20.6 [Purpur](https://github.com/PurpurMC/Purpur) fork that brings vertical scaling to Minecraft.

ShreddedPaper:

- Allows multiple threads to work together to run a single world
  - When ticking a chunk on one thread, all other chunks in a certain radius
    are locked so that only this thread has access to them, preventing any
    race conditions between threads.

See [HOW_IT_WORKS.md](HOW_IT_WORKS.md) for more information on how ShreddedPaper
works.

### Developing a plugin for a multi-threaded server

In summary, a plugin must be careful of:

- Different threads updating certain data at the same time.
- One thread reading data while it is being updated by another thread.
- Code is to be executed on the chunk's thread, not simply the main thread.

[See here for a more detailed tutorial](DEVELOPING_A_MULTITHREAD_PLUGIN.md)

### Using the ShreddedPaper API as a dependency

[![Clojars Project](https://img.shields.io/clojars/v/com.github.puregero/shreddedpaper-api.svg)](https://clojars.org/com.github.puregero/shreddedpaper-api)

Add the following into your build.gradle:

```
repositories {
  maven {
    url "https://repo.clojars.org/"
  }
}

dependencies {
  compile "com.github.puregero:shreddedpaper-api:1.20.6-R0.1-SNAPSHOT"
}
```

Or in your pom.xml:

```
<repositories>
    <repository>
        <id>clojars</id>
        <url>https://repo.clojars.org/</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.github.puregero</groupId>
        <artifactId>shreddedpaper-api</artifactId>
        <version>1.20.6-R0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Building
Requirements:
- You need `git` installed, with a configured user name and email. 
   On windows you need to run from git bash.
- You need `jdk` 21+ installed to compile (and `jre` 21+ to run)

Build instructions:
1. Patch paper with: `./gradlew applyPatches`
2. Build the shreddedpaper jars with: `./gradlew shadowjar createReobfPaperclipJar`
3. Get the shreddedpaper jar from `build/libs`

## Publishing to maven local
Publish to your local maven repository with: `./gradlew publishToMavenLocal`

Note for mac users: The latest macOS version includes an incompatible version of
diff and you'll need to install a compatible one. Use `brew install diffutils`
to install it, and then reopen the terminal window.

If `diff --version` returns the following, it is incompatible and will not work:
```
Apple diff (based on FreeBSD diff)
```

### Licensing

All code is licensed under [GPLv3](LICENSE.txt).

### Acknowledgements

ShreddedPaper uses PaperMC's paperweight framework found
[here](https://github.com/PaperMC/paperweight).