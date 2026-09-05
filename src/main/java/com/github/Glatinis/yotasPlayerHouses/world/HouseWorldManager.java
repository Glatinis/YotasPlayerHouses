package com.github.Glatinis.yotasPlayerHouses.world;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.reasons.CreateFailureReason;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class HouseWorldManager {
    private static final String VOID_GENERATOR_SETTINGS =
            "{\"layers\":[{\"block\":\"air\",\"height\":1}],\"biome\":\"the_void\"}";

    public enum FixResult {
        ALREADY_LOADED,
        RECOVERED_BY_RELOAD,
        NEEDS_RECREATE_CONFIRMATION,
        RECREATED,
        FAILED
    }

    private final YotasPlayerHouses plugin;
    private final ConfigManager configManager;
    private MultiverseCoreApi coreApi;

    private boolean ready;

    public HouseWorldManager(YotasPlayerHouses plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        try {
            coreApi = MultiverseCoreApi.get();
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not reach the Multiverse-Core API: " + exception.getMessage());
            plugin.getLogger().severe("Houses will not work until this is fixed.");
            return;
        }

        initializeWorld();
    }

    // Whether the house world is actually loaded and usable right now.
    public boolean isReady() {
        return ready;
    }

    private void initializeWorld() {
        String worldName = configManager.getHouseWorldName();

        if (Bukkit.getWorld(worldName) != null) {
            ready = true;
            return;
        }

        if (coreApi.getWorldManager().isWorld(worldName)) {
            // Registered but not loaded - try a plain reload before giving up.
            if (tryReload(worldName)) {
                plugin.getLogger().info("House world '" + worldName + "' loaded on retry.");
                return;
            }

            plugin.getLogger().severe("House world '" + worldName + "' is registered with Multiverse but " +
                    "failed to load (check the earlier Multiverse-Core startup errors for the reason, e.g. " +
                    "WORLD_FOLDER_INVALID - a missing or corrupt world folder). Houses will not work until " +
                    "this is fixed - run '/playerhouse admin fixworld' in-game to attempt an automatic " +
                    "repair, or fix/restore the world folder yourself and restart.");
            return;
        }

        // Not known to Multiverse at all - first run, safe to create fresh.
        createFreshWorld(worldName);
    }

    // allowRecreate must only be true with explicit admin confirmation - it discards the old world.
    public FixResult attemptFix(boolean allowRecreate) {
        if (coreApi == null) {
            plugin.getLogger().severe("Can't fix the house world: the Multiverse-Core API was unreachable " +
                    "when this plugin started. Restart the server once Multiverse-Core is working.");
            return FixResult.FAILED;
        }

        String worldName = configManager.getHouseWorldName();

        if (Bukkit.getWorld(worldName) != null) {
            ready = true;
            return FixResult.ALREADY_LOADED;
        }

        WorldManager worldManager = coreApi.getWorldManager();

        if (worldManager.isWorld(worldName) && tryReload(worldName))
            return FixResult.RECOVERED_BY_RELOAD;

        if (!allowRecreate)
            return FixResult.NEEDS_RECREATE_CONFIRMATION;

        // Folder first so a failure here leaves nothing else touched.
        if (!moveBrokenFolderAside(worldName))
            return FixResult.FAILED;

        if (worldManager.isWorld(worldName)) {
            Attempt<String, ?> removed = worldManager.removeWorld(worldName);
            if (removed.isFailure()) {
                plugin.getLogger().severe("Could not recreate house world '" + worldName + "': failed to " +
                        "unregister the broken entry from Multiverse (" + removed.getFailureMessage() + ").");
                return FixResult.FAILED;
            }
        }

        return createFreshWorld(worldName) ? FixResult.RECREATED : FixResult.FAILED;
    }

    private boolean tryReload(String worldName) {
        boolean loaded = coreApi.getWorldManager().loadWorld(worldName).isSuccess();
        ready = loaded && Bukkit.getWorld(worldName) != null;
        return ready;
    }

    // Renames the existing world folder aside instead of deleting it. No-op if there's none to move.
    private boolean moveBrokenFolderAside(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists())
            return true;

        Path backup = worldFolder.toPath().resolveSibling(worldName + "_broken_" + System.currentTimeMillis());
        try {
            try {
                Files.move(worldFolder.toPath(), backup, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(worldFolder.toPath(), backup);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not recreate house world '" + worldName + "': failed to move " +
                    "aside the existing folder '" + worldFolder.getPath() + "' (" + exception.getMessage() +
                    "). Move or delete it manually, then try again.");
            return false;
        }

        plugin.getLogger().warning("Moved the broken '" + worldName + "' world folder to '"
                + backup.getFileName() + "' before recreating it.");
        return true;
    }

    private boolean createFreshWorld(String worldName) {
        Attempt<?, CreateFailureReason> created = coreApi.getWorldManager().createWorld(CreateWorldOptions
                .worldName(worldName)
                .worldType(WorldType.FLAT)
                .generatorSettings(VOID_GENERATOR_SETTINGS)
                .environment(World.Environment.NORMAL));

        if (created.isFailure()) {
            plugin.getLogger().severe("Failed to create house world '" + worldName + "': " + created.getFailureMessage());
            return false;
        }

        ready = Bukkit.getWorld(worldName) != null;
        return ready;
    }
}
