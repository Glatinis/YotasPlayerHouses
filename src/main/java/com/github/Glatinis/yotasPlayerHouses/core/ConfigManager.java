package com.github.Glatinis.yotasPlayerHouses.core;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private YotasPlayerHouses plugin;
    private FileConfiguration config;

    public ConfigManager(YotasPlayerHouses plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public String getHouseWorldName() {
        return config.getString("houseworld-name");
    }
}
