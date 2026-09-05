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

    // Checked live (not cached) - null means the house world isn't loaded right now.
    private World resolveHouseWorld() {
        return Bukkit.getWorld(configManager.getHouseWorldName());
    }

    public boolean isHouseWorldReady() {
        return resolveHouseWorld() != null;
    }

    // Where index N sits under the currently configured spacing/row-length/y-level. Only for handing out
    // a fresh spot or migrating a legacy pin - an existing house's real location is getIslandOrigin(uuid).
    private Location computeGridOrigin(int islandIndex, World houseWorld) {
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

    // Null when the player has no island, or when they do but the house world isn't loaded right now.
    public Location getIslandOrigin(UUID uuid) {
        PlayerHouseData data = playerDataManager.get(uuid);
        if (!data.hasIsland())
            return null;

        World houseWorld = resolveHouseWorld();

        if (!data.hasOrigin()) {
            if (houseWorld == null)
                return null;
            migrateOrigin(data, houseWorld);
        }

        if (houseWorld == null) {
            plugin.getLogger().warning("Can't resolve " + uuid + "'s house location: world '"
                    + configManager.getHouseWorldName() + "' is not loaded.");
            return null;
        }

        return new Location(houseWorld, data.getOriginX(), data.getOriginY(), data.getOriginZ());
    }

    // Backfills the origin for player data saved before pinning existed, using the current config as a
    // best guess. If that guess is wrong, /playerhouse admin relocate fixes it.
    private void migrateOrigin(PlayerHouseData data, World houseWorld) {
        Location origin = computeGridOrigin(data.getIslandIndex(), houseWorld);
        data.setOrigin(origin.getX(), origin.getY(), origin.getZ());
        playerDataManager.save(data);
    }

    // Creation

    public void getOrCreateIsland(Player player, Consumer<Location> onReady) {
        PlayerHouseData data = playerDataManager.get(player.getUniqueId());

        if (data.hasIsland()) {
            Location origin = getIslandOrigin(data.getUuid());
            if (origin == null)
                player.sendMessage("§cYour house world isn't available right now, please contact an admin.");
            else
                onReady.accept(origin);
            return;
        }

        World houseWorld = resolveHouseWorld();
        if (houseWorld == null) {
            plugin.getLogger().severe("Can't create a house for " + player.getName() + ": world '"
                    + configManager.getHouseWorldName() + "' is not loaded.");
            player.sendMessage("§cHouses aren't available right now, please contact an admin.");
            return;
        }

        int index = playerDataManager.allocateIslandIndex();
        Location origin = computeGridOrigin(index, houseWorld);

        schematicManager.pasteAsync(configManager.getBaseSchematic(), origin, () -> {
            data.setIslandIndex(index);
            data.setOrigin(origin.getX(), origin.getY(), origin.getZ());
            playerDataManager.save(data);
            onReady.accept(origin);
        }, exception -> {
            plugin.getLogger().warning("Failed to paste base schematic for " + player.getName() + ": " + exception.getMessage());
            player.sendMessage("§cFailed to create your house, please contact an admin.");
        });
    }

    // Re-pins an existing player's origin without touching any blocks.
    public boolean relocateOrigin(UUID targetUuid, Location newOrigin) {
        PlayerHouseData data = playerDataManager.get(targetUuid);
        if (!data.hasIsland())
            return false;

        data.setOrigin(newOrigin.getX(), newOrigin.getY(), newOrigin.getZ());
        playerDataManager.save(data);
        return true;
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

        Location origin = getIslandOrigin(targetUuid);
        if (origin == null) {
            onFailure.accept(new IllegalStateException("house world '" + configManager.getHouseWorldName()
                    + "' is not loaded"));
            return;
        }
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
