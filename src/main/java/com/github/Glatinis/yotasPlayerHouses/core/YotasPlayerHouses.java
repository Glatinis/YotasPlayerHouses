package com.github.Glatinis.yotasPlayerHouses.core;

import org.bukkit.plugin.java.JavaPlugin;

public final class YotasPlayerHouses extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Loaded plugin!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Unloaded plugin!");
    }
}
