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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
}
