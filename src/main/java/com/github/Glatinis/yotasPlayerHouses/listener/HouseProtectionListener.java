package com.github.Glatinis.yotasPlayerHouses.listener;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class HouseProtectionListener implements Listener {
    private static final String BYPASS_PERMISSION = "yotasplayerhouses.admin.bypass";

    private final ConfigManager configManager;

    public HouseProtectionListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    private boolean isHouseWorld(World world) {
        return configManager.isProtectionEnabled() && world.getName().equals(configManager.getHouseWorldName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isHouseWorld(event.getBlock().getWorld()))
            return;
        if (hasBypass(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isHouseWorld(event.getBlock().getWorld()))
            return;
        if (hasBypass(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isHouseWorld(event.getLocation().getWorld()))
            event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (isHouseWorld(event.getBlock().getWorld()))
            event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (isHouseWorld(event.getBlock().getWorld()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (isHouseWorld(event.getBlock().getWorld()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (isHouseWorld(event.getBlock().getWorld()))
            event.setCancelled(true);
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission(BYPASS_PERMISSION);
    }
}
