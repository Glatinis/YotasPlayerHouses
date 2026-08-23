package com.github.Glatinis.yotasPlayerHouses.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final YotasPlayerHouses plugin;
    private FileConfiguration config;

    public ConfigManager(YotasPlayerHouses plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // World

    public String getHouseWorldName() {
        return config.getString("houseworld-name");
    }

    // Island grid

    public int getIslandSpacing() {
        return config.getInt("island.spacing", 500);
    }

    public int getIslandRowLength() {
        return config.getInt("island.row-length", 50);
    }

    public int getIslandYLevel() {
        return config.getInt("island.y-level", 100);
    }

    public String getBaseSchematic() {
        return config.getString("island.base-schematic", "base_house.schem");
    }

    public boolean isProtectionEnabled() {
        return config.getBoolean("island.protection", true);
    }

    // Hub

    public Location getHubLocation() {
        String worldName = config.getString("hub.world", "world");
        return new Location(
                Bukkit.getWorld(worldName),
                config.getDouble("hub.x"),
                config.getDouble("hub.y"),
                config.getDouble("hub.z"),
                (float) config.getDouble("hub.yaw"),
                (float) config.getDouble("hub.pitch")
        );
    }

    // Invites

    public int getInviteExpirySeconds() {
        return config.getInt("invite-expiry-seconds", 60);
    }

    // Upgrades

    public ConfigurationSection getUpgradesSection() {
        return config.getConfigurationSection("upgrades");
    }
}
