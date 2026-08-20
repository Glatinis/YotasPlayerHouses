package com.github.Glatinis.yotasPlayerHouses.world;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;

public class HouseWorldManager {
    private static final String VOID_GENERATOR_SETTINGS =
            "{\"layers\":[{\"block\":\"air\",\"height\":1}],\"biome\":\"the_void\"}";

    private MultiverseCoreApi coreApi;
    private ConfigManager configManager;
    private YotasPlayerHouses plugin;

    public HouseWorldManager(YotasPlayerHouses plugin, ConfigManager configManager) {
        this.plugin = plugin;
        coreApi = MultiverseCoreApi.get();
        this.configManager = configManager;

        initializeWorld();
    }

    private void initializeWorld() {
        WorldManager worldManager = coreApi.getWorldManager();
        String worldName = configManager.getHouseWorldName();

        // Check if world exists
        if (worldManager.isWorld(worldName))
            return;

        // Create void world
        worldManager.createWorld(CreateWorldOptions
                .worldName(worldName)
                .worldType(WorldType.FLAT)
                .generatorSettings(VOID_GENERATOR_SETTINGS)
                .environment(World.Environment.NORMAL)
        ).onFailure(failure ->
                plugin.getLogger().warning("Failed to create house world '" + worldName + "': "
                        + failure.getFailureMessage())
        );
    }
}
