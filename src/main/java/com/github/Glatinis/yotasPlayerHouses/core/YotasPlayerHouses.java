package com.github.Glatinis.yotasPlayerHouses.core;

import org.bukkit.plugin.java.JavaPlugin;

public final class YotasPlayerHouses extends JavaPlugin {
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        getLogger().info("Loaded plugin!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Unloaded plugin!");
    }
}
