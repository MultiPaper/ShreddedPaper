package io.multipaper.shreddedpaper.permissions;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.permissions.DefaultPermissions;

public class ShreddedPaperPermissions {
    private static final String ROOT = "shreddedpaper";

    public static void registerCorePermissions() {
        Permission parent = DefaultPermissions.registerPermission(ROOT, "Gives the user the ability to use all ShreddedPaper utilities and commands");

        DefaultPermissions.registerPermission(ROOT + ".maximumtrackerbypass", "Gives the user the ability to see all entities regardless of the maximum tracker config", PermissionDefault.OP, parent);

        ShreddedPaperCommandPermissions.registerPermissions(parent);

        parent.recalculatePermissibles();
    }
}
