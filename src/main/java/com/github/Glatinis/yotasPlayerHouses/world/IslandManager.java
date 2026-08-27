package com.github.Glatinis.yotasPlayerHouses.world;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import com.github.Glatinis.yotasPlayerHouses.data.PlayerDataManager;
import com.github.Glatinis.yotasPlayerHouses.data.PlayerHouseData;
import com.github.Glatinis.yotasPlayerHouses.schematic.SchematicManager;
import com.github.Glatinis.yotasPlayerHouses.upgrade.Upgrade;
import com.github.Glatinis.yotasPlayerHouses.upgrade.UpgradeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
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

    // Clears every owned upgrade's footprint (they may sit at their own offset, e.g. a farm elsewhere
    // on the island) before re-pasting the base schematic, so nothing lingers after a reset.
    public void resetIsland(UUID targetUuid, UpgradeManager upgradeManager, Runnable onSuccess, Consumer<Exception> onFailure) {
        PlayerHouseData data = playerDataManager.get(targetUuid);
        if (!data.hasIsland()) {
            onFailure.accept(new IllegalStateException("player has no house"));
            return;
        }

        Location origin = getIslandOrigin(data.getIslandIndex());
        List<String> ownedIds = new ArrayList<>(data.getOwnedUpgrades());

        clearOwnedUpgrades(origin, ownedIds, 0, upgradeManager, () ->
                schematicManager.pasteAsync(configManager.getBaseSchematic(), origin, () -> {
                    data.getOwnedUpgrades().clear();
                    playerDataManager.save(data);
                    onSuccess.run();
                }, onFailure), onFailure);
    }

    private void clearOwnedUpgrades(Location origin, List<String> ownedIds, int index, UpgradeManager upgradeManager,
                                     Runnable onSuccess, Consumer<Exception> onFailure) {
        if (index >= ownedIds.size()) {
            onSuccess.run();
            return;
        }

        Upgrade upgrade = upgradeManager.getUpgrade(ownedIds.get(index));
        if (upgrade == null || !schematicManager.exists(upgrade.getSchematic())) {
            // Upgrade no longer exists in config or its schematic is gone; nothing to clear, move on.
            clearOwnedUpgrades(origin, ownedIds, index + 1, upgradeManager, onSuccess, onFailure);
            return;
        }

        Location clearPoint = origin.clone().add(upgrade.getOffsetX(), upgrade.getOffsetY(), upgrade.getOffsetZ());
        schematicManager.clearAsync(upgrade.getSchematic(), clearPoint,
                () -> clearOwnedUpgrades(origin, ownedIds, index + 1, upgradeManager, onSuccess, onFailure),
                onFailure);
    }
}
