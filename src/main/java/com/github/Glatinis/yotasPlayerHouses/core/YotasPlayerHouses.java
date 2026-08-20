package com.github.Glatinis.yotasPlayerHouses.core;

import com.github.Glatinis.yotasPlayerHouses.world.HouseWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class YotasPlayerHouses extends JavaPlugin {
    private ConfigManager configManager;
    private HouseWorldManager houseWorldManager;

    @Override
    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core")) {
            getLogger().severe("Multiverse-Core version 5.8.0 is required to run this plugin!");
            getLogger().severe("Unloading...");
            Bukkit.getPluginManager().disablePlugin(this);
        }

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
