package com.github.Glatinis.yotasPlayerHouses.core;

import com.github.Glatinis.yotasPlayerHouses.world.HouseWorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class YotasPlayerHouses extends JavaPlugin {
    private ConfigManager configManager;
    private HouseWorldManager houseWorldManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        houseWorldManager = new HouseWorldManager(this, configManager);

        getLogger().info("Loaded plugin!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Unloaded plugin!");
    }
}
