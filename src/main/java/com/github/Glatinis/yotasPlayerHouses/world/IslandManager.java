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

import java.util.UUID;
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

    public Location getIslandOrigin(int islandIndex) {
        World houseWorld = Bukkit.getWorld(configManager.getHouseWorldName());
        int rowLength = configManager.getIslandRowLength();
        int spacing = configManager.getIslandSpacing();

        int column = islandIndex % rowLength;
        int row = islandIndex / rowLength;

        double x = column * spacing;
        double z = row * spacing;

        return new Location(houseWorld, x + 0.5, configManager.getIslandYLevel(), z + 0.5);
    }

    public Location getIslandOrigin(Player player) {
        return getIslandOrigin(player.getUniqueId());
    }

    public Location getIslandOrigin(UUID uuid) {
        PlayerHouseData data = playerDataManager.get(uuid);
        if (!data.hasIsland())
            return null;
        return getIslandOrigin(data.getIslandIndex());
    }

    // Creation

    public void getOrCreateIsland(Player player, Consumer<Location> onReady) {
        PlayerHouseData data = playerDataManager.get(player.getUniqueId());

        if (data.hasIsland()) {
            onReady.accept(getIslandOrigin(data.getIslandIndex()));
            return;
        }

        int index = playerDataManager.allocateIslandIndex();
        Location origin = getIslandOrigin(index);

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

    // Admin reset

    public void resetIsland(UUID targetUuid, Runnable onSuccess, Consumer<Exception> onFailure) {
        PlayerHouseData data = playerDataManager.get(targetUuid);
        if (!data.hasIsland()) {
            onFailure.accept(new IllegalStateException("player has no house"));
            return;
        }

        Location origin = getIslandOrigin(data.getIslandIndex());
        schematicManager.pasteAsync(configManager.getBaseSchematic(), origin, () -> {
            data.getOwnedUpgrades().clear();
            playerDataManager.save(data);
            onSuccess.run();
        }, onFailure);
    }
}
