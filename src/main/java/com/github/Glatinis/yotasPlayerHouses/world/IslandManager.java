package com.github.Glatinis.yotasPlayerHouses.world;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import com.github.Glatinis.yotasPlayerHouses.data.PlayerDataManager;
import com.github.Glatinis.yotasPlayerHouses.data.PlayerHouseData;
import com.github.Glatinis.yotasPlayerHouses.schematic.SchematicManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class IslandManager {
    private final YotasPlayerHouses plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final SchematicManager schematicManager;

    public IslandManager(YotasPlayerHouses plugin, ConfigManager configManager,
                          PlayerDataManager playerDataManager, SchematicManager schematicManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.schematicManager = schematicManager;
    }

    // Grid placement

    private Location originForIndex(int index) {
        World houseWorld = Bukkit.getWorld(configManager.getHouseWorldName());
        int rowLength = configManager.getIslandRowLength();
        int spacing = configManager.getIslandSpacing();

        int column = index % rowLength;
        int row = index / rowLength;

        double x = column * spacing;
        double z = row * spacing;

        return new Location(houseWorld, x + 0.5, configManager.getIslandYLevel(), z + 0.5);
    }

    public Location getIslandOrigin(Player player) {
        PlayerHouseData data = playerDataManager.get(player.getUniqueId());
        if (!data.hasIsland())
            return null;
        return originForIndex(data.getIslandIndex());
    }

    // Creation

    public void getOrCreateIsland(Player player, Consumer<Location> onReady) {
        PlayerHouseData data = playerDataManager.get(player.getUniqueId());

        if (data.hasIsland()) {
            onReady.accept(originForIndex(data.getIslandIndex()));
            return;
        }

        int index = playerDataManager.allocateIslandIndex();
        Location origin = originForIndex(index);

        schematicManager.pasteAsync(configManager.getBaseSchematic(), origin, () -> {
            data.setIslandIndex(index);
            playerDataManager.save(data);
            onReady.accept(origin);
        }, exception -> {
            plugin.getLogger().warning("Failed to paste base schematic for " + player.getName() + ": " + exception.getMessage());
            player.sendMessage("§cFailed to create your house, please contact an admin.");
        });
    }

    public void teleportHome(Player player) {
        getOrCreateIsland(player, origin -> {
            Location safeSpot = origin.clone().add(0.0, 1.0, 0.0);
            safeSpot.setYaw(player.getLocation().getYaw());
            safeSpot.setPitch(0.0f);
            player.teleport(safeSpot);
        });
    }
}
