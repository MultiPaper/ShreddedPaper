package io.multipaper.shreddedpaper.commands;

import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;

import java.util.HashMap;
import java.util.Map;

public class ShreddedPaperCommands {

    private static final Map<String, Command> COMMANDS = new HashMap<>();
    static {
        for (Command command : new Command[] {
                new MPMapCommand("mpmap")
        }) {
            COMMANDS.put(command.getName(), command);
        }
    }

    public static void registerCommands(final MinecraftServer server) {
        COMMANDS.forEach((s, command) -> {
            server.server.getCommandMap().register(s, "shreddedpaper", command);
        });
    }

}
