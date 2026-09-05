package com.github.Glatinis.yotasPlayerHouses.listener;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import com.github.Glatinis.yotasPlayerHouses.world.IslandManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

// Sends players who die inside the house world back to their own island instead of wherever the
// server would otherwise respawn them (typically the overworld spawn point, which lines up with
// nothing in the house world's grid and can easily drop them into the void).
public class HouseRespawnListener implements Listener {
    private final ConfigManager configManager;
    private final IslandManager islandManager;

    public HouseRespawnListener(ConfigManager configManager, IslandManager islandManager) {
        this.configManager = configManager;
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(configManager.getHouseWorldName()))
            return;

        Location origin = islandManager.getIslandOrigin(player);
        if (origin != null) {
            event.setRespawnLocation(safeSpotAbove(origin, player));
            return;
        }

        // Fall back to the hub, unless its world isn't loaded either.
        if (configManager.isHubWorldLoaded())
            event.setRespawnLocation(configManager.getHubLocation());
    }

    private Location safeSpotAbove(Location origin, Player player) {
        Location safeSpot = origin.clone().add(0.0, 1.0, 0.0);
        safeSpot.setYaw(player.getLocation().getYaw());
        safeSpot.setPitch(0.0f);
        return safeSpot;
    }
}
