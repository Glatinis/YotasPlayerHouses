package com.github.Glatinis.yotasPlayerHouses.upgrade;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import com.github.Glatinis.yotasPlayerHouses.data.PlayerDataManager;
import com.github.Glatinis.yotasPlayerHouses.data.PlayerHouseData;
import com.github.Glatinis.yotasPlayerHouses.economy.EconomyManager;
import com.github.Glatinis.yotasPlayerHouses.schematic.SchematicManager;
import com.github.Glatinis.yotasPlayerHouses.world.IslandManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class UpgradeManager {
    private final YotasPlayerHouses plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final SchematicManager schematicManager;
    private final PlayerDataManager playerDataManager;
    private final IslandManager islandManager;

    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();

    public UpgradeManager(YotasPlayerHouses plugin, ConfigManager configManager, EconomyManager economyManager,
                           SchematicManager schematicManager, PlayerDataManager playerDataManager,
                           IslandManager islandManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.economyManager = economyManager;
        this.schematicManager = schematicManager;
        this.playerDataManager = playerDataManager;
        this.islandManager = islandManager;

        load();
    }

    // Loading

    public void load() {
        upgrades.clear();
        ConfigurationSection section = configManager.getUpgradesSection();
        if (section == null)
            return;

        for (String id : section.getKeys(false)) {
            try {
                upgrades.put(id, parseUpgrade(id, section.getConfigurationSection(id)));
            } catch (Exception exception) {
                plugin.getLogger().warning("Skipping invalid upgrade '" + id + "': " + exception.getMessage());
            }
        }

        validateRequirementChains();
    }

    private Upgrade parseUpgrade(String id, ConfigurationSection upgradeSection) {
        if (upgradeSection == null)
            throw new IllegalArgumentException("missing section");

        String schematic = upgradeSection.getString("schematic");
        if (schematic == null || schematic.isEmpty())
            throw new IllegalArgumentException("missing schematic file name");
        if (!schematicManager.exists(schematic))
            throw new IllegalArgumentException("schematic file '" + schematic + "' not found");

        String displayName = upgradeSection.getString("display-name", id);
        String category = upgradeSection.getString("category", "general");
        String requires = upgradeSection.getString("requires", "");
        String permission = upgradeSection.getString("permission", "");

        int offsetX = upgradeSection.getInt("offset.x", 0);
        int offsetY = upgradeSection.getInt("offset.y", 0);
        int offsetZ = upgradeSection.getInt("offset.z", 0);

        UpgradeCost cost = parseCost(upgradeSection.getConfigurationSection("cost"));

        return new Upgrade(id, displayName, category, schematic, offsetX, offsetY, offsetZ, requires, permission, cost);
    }

    private UpgradeCost parseCost(ConfigurationSection costSection) {
        if (costSection == null)
            return new UpgradeCost(0.0, Collections.emptyList());

        double money = costSection.getDouble("money", 0.0);
        List<ItemCost> items = new ArrayList<>();

        for (Map<?, ?> entry : costSection.getMapList("items")) {
            Object materialName = entry.get("material");
            Object amountValue = entry.get("amount");
            if (materialName == null || amountValue == null)
                continue;

            Material material = Material.matchMaterial(materialName.toString());
            if (material == null) {
                plugin.getLogger().warning("Unknown material '" + materialName + "' in an upgrade cost, skipping item.");
                continue;
            }

            items.add(new ItemCost(material, Integer.parseInt(amountValue.toString())));
        }

        return new UpgradeCost(money, items);
    }

    private void validateRequirementChains() {
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.hasRequirement() && !upgrades.containsKey(upgrade.getRequires())) {
                plugin.getLogger().warning("Upgrade '" + upgrade.getId() + "' requires unknown upgrade '"
                        + upgrade.getRequires() + "'. It will be unreachable until fixed.");
            }
        }
    }

    // Lookup

    public Upgrade getUpgrade(String id) {
        return upgrades.get(id);
    }

    public List<Upgrade> getAll() {
        return new ArrayList<>(upgrades.values());
    }

    public List<Upgrade> getByCategory(String category) {
        List<Upgrade> result = new ArrayList<>();
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.getCategory().equalsIgnoreCase(category))
                result.add(upgrade);
        }
        return result;
    }

    // Purchasing

    public void purchase(Player player, String upgradeId, Consumer<PurchaseResult> callback) {
        Upgrade upgrade = upgrades.get(upgradeId);
        if (upgrade == null) {
            callback.accept(PurchaseResult.UNKNOWN_UPGRADE);
            return;
        }

        PlayerHouseData data = playerDataManager.get(player.getUniqueId());
        if (!data.hasIsland()) {
            callback.accept(PurchaseResult.NO_ISLAND);
            return;
        }

        if (data.hasUpgrade(upgradeId)) {
            callback.accept(PurchaseResult.ALREADY_OWNED);
            return;
        }

        if (upgrade.hasRequirement() && !data.hasUpgrade(upgrade.getRequires())) {
            callback.accept(PurchaseResult.MISSING_REQUIREMENT);
            return;
        }

        if (upgrade.requiresPermission() && !player.hasPermission(upgrade.getPermission())) {
            callback.accept(PurchaseResult.MISSING_PERMISSION);
            return;
        }

        UpgradeCost cost = upgrade.getCost();
        if (!economyManager.hasBalance(player, cost.getMoney())) {
            callback.accept(PurchaseResult.INSUFFICIENT_FUNDS);
            return;
        }

        if (!hasRequiredItems(player, cost.getItems())) {
            callback.accept(PurchaseResult.INSUFFICIENT_ITEMS);
            return;
        }

        if (!schematicManager.exists(upgrade.getSchematic())) {
            callback.accept(PurchaseResult.SCHEMATIC_MISSING);
            return;
        }

        Location origin = islandManager.getIslandOrigin(player);
        if (origin == null) {
            callback.accept(PurchaseResult.HOUSE_WORLD_UNAVAILABLE);
            return;
        }
        Location pastePoint = origin.clone().add(upgrade.getOffsetX(), upgrade.getOffsetY(), upgrade.getOffsetZ());

        schematicManager.pasteAsync(upgrade.getSchematic(), pastePoint, () -> {
            economyManager.withdraw(player, cost.getMoney());
            removeItems(player, cost.getItems());

            data.addUpgrade(upgradeId);
            playerDataManager.save(data);

            callback.accept(PurchaseResult.SUCCESS);
        }, exception -> {
            plugin.getLogger().warning("Failed to paste schematic '" + upgrade.getSchematic() + "': " + exception.getMessage());
            callback.accept(PurchaseResult.PASTE_FAILED);
        });
    }

    private boolean hasRequiredItems(Player player, List<ItemCost> items) {
        for (ItemCost itemCost : items) {
            if (!player.getInventory().containsAtLeast(new ItemStack(itemCost.getMaterial()), itemCost.getAmount()))
                return false;
        }
        return true;
    }

    private void removeItems(Player player, List<ItemCost> items) {
        for (ItemCost itemCost : items) {
            player.getInventory().removeItem(new ItemStack(itemCost.getMaterial(), itemCost.getAmount()));
        }
    }

    // Admin actions

    public List<String> getOwnedUpgradeIds(UUID targetUuid) {
        return new ArrayList<>(playerDataManager.get(targetUuid).getOwnedUpgrades());
    }

    public void grant(UUID targetUuid, String upgradeId, Consumer<AdminActionResult> callback) {
        Upgrade upgrade = upgrades.get(upgradeId);
        if (upgrade == null) {
            callback.accept(AdminActionResult.UNKNOWN_UPGRADE);
            return;
        }

        PlayerHouseData data = playerDataManager.get(targetUuid);
        if (!data.hasIsland()) {
            callback.accept(AdminActionResult.NO_ISLAND);
            return;
        }

        if (data.hasUpgrade(upgradeId)) {
            callback.accept(AdminActionResult.ALREADY_OWNED);
            return;
        }

        if (!schematicManager.exists(upgrade.getSchematic())) {
            callback.accept(AdminActionResult.SCHEMATIC_MISSING);
            return;
        }

        Location origin = islandManager.getIslandOrigin(targetUuid);
        if (origin == null) {
            callback.accept(AdminActionResult.HOUSE_WORLD_UNAVAILABLE);
            return;
        }
        Location pastePoint = origin.clone().add(upgrade.getOffsetX(), upgrade.getOffsetY(), upgrade.getOffsetZ());

        schematicManager.pasteAsync(upgrade.getSchematic(), pastePoint, () -> {
            data.addUpgrade(upgradeId);
            playerDataManager.save(data);
            callback.accept(AdminActionResult.SUCCESS);
        }, exception -> callback.accept(AdminActionResult.PASTE_FAILED));
    }

    // Clears ownership of the upgrade and everything currently owned that's built on top of it, then
    // reveals whatever's left underneath.
    public void revoke(UUID targetUuid, String upgradeId, Consumer<AdminActionResult> callback) {
        Upgrade upgrade = upgrades.get(upgradeId);
        if (upgrade == null) {
            callback.accept(AdminActionResult.UNKNOWN_UPGRADE);
            return;
        }

        PlayerHouseData data = playerDataManager.get(targetUuid);
        if (!data.hasUpgrade(upgradeId)) {
            callback.accept(AdminActionResult.NOT_OWNED);
            return;
        }

        // Checked before mutating data below, so a revoke can't be recorded without actually happening.
        Location islandOrigin = islandManager.getIslandOrigin(targetUuid);
        if (islandOrigin == null) {
            callback.accept(AdminActionResult.HOUSE_WORLD_UNAVAILABLE);
            return;
        }

        // Anything currently owned that's built on top of this upgrade (directly or transitively) has to
        // be stripped too. Otherwise its structure keeps standing on a footprint we're about to clear out
        // from under it (e.g. clearing tier_1's floor while tier_2/VIP, which share its footprint, are
        // still sitting on top of it) — which is how you end up with a hole and a player falling through.
        List<Upgrade> toStrip = new ArrayList<>();
        collectOwnedDependents(data, upgradeId, toStrip, new HashSet<>());
        toStrip.add(upgrade);

        for (Upgrade stripped : toStrip)
            data.removeUpgrade(stripped.getId());
        playerDataManager.save(data);

        clearAll(islandOrigin, toStrip, 0, () -> pasteFallback(islandOrigin, data, upgrade, callback),
                exception -> callback.accept(AdminActionResult.PASTE_FAILED));
    }

    // Finds whichever tier the player still owns below "upgrade" (may be none) and pastes it, or falls
    // back to the base house if this upgrade shares its footprint, or leaves it cleared if neither apply
    // (e.g. a standalone structure like a farm that the base house never touched).
    private void pasteFallback(Location islandOrigin, PlayerHouseData data, Upgrade upgrade, Consumer<AdminActionResult> callback) {
        Upgrade fallback = findOwnedFallback(data, upgrade);

        String pasteSchematic;
        Location pastePoint;
        if (fallback != null) {
            pasteSchematic = fallback.getSchematic();
            pastePoint = islandOrigin.clone().add(fallback.getOffsetX(), fallback.getOffsetY(), fallback.getOffsetZ());
        } else if (sharesBaseFootprint(upgrade)) {
            pasteSchematic = configManager.getBaseSchematic();
            pastePoint = islandOrigin;
        } else {
            callback.accept(AdminActionResult.SUCCESS);
            return;
        }

        schematicManager.pasteAsync(pasteSchematic, pastePoint,
                () -> callback.accept(AdminActionResult.SUCCESS),
                exception -> callback.accept(AdminActionResult.PASTE_FAILED));
    }

    // Walks the requires chain below "revoked", skipping tiers the player doesn't actually own (e.g. this
    // upgrade may have been admin-granted directly, skipping the chain) or that no longer exist in config.
    private Upgrade findOwnedFallback(PlayerHouseData data, Upgrade revoked) {
        Set<String> visited = new HashSet<>();
        String requiresId = revoked.getRequires();
        while (requiresId != null && !requiresId.isEmpty() && visited.add(requiresId)) {
            Upgrade candidate = upgrades.get(requiresId);
            if (candidate == null)
                return null; // chain is broken here, nothing further we can safely reveal

            if (data.hasUpgrade(candidate.getId()))
                return candidate;

            requiresId = candidate.getRequires();
        }
        return null;
    }

    // Recursively gathers every currently-owned upgrade built on top of upgradeId, deepest first.
    private void collectOwnedDependents(PlayerHouseData data, String upgradeId, List<Upgrade> results, Set<String> visited) {
        if (!visited.add(upgradeId))
            return;

        for (Upgrade candidate : upgrades.values()) {
            if (upgradeId.equals(candidate.getRequires()) && data.hasUpgrade(candidate.getId())) {
                collectOwnedDependents(data, candidate.getId(), results, visited);
                results.add(candidate);
            }
        }
    }

    private void clearAll(Location islandOrigin, List<Upgrade> toClear, int index,
                           Runnable onSuccess, Consumer<Exception> onFailure) {
        if (index >= toClear.size()) {
            onSuccess.run();
            return;
        }

        Upgrade upgrade = toClear.get(index);
        Location clearPoint = islandOrigin.clone().add(upgrade.getOffsetX(), upgrade.getOffsetY(), upgrade.getOffsetZ());
        schematicManager.clearAsync(upgrade.getSchematic(), clearPoint,
                () -> clearAll(islandOrigin, toClear, index + 1, onSuccess, onFailure),
                onFailure);
    }

    // A (0,0,0) offset is what the base house is always pasted at, and the config convention is that tiers
    // replacing the same structure share that offset too — so this is true only for upgrades sitting
    // directly on top of the base house's own footprint, not for standalone structures like a farm.
    private boolean sharesBaseFootprint(Upgrade upgrade) {
        return upgrade.getOffsetX() == 0 && upgrade.getOffsetY() == 0 && upgrade.getOffsetZ() == 0;
    }
}
