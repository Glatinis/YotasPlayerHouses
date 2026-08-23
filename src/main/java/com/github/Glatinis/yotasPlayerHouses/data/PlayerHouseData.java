package com.github.Glatinis.yotasPlayerHouses.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerHouseData {
    public static final int NO_ISLAND = -1;

    private final UUID uuid;
    private int islandIndex;
    private final Set<String> ownedUpgrades;

    public PlayerHouseData(UUID uuid, int islandIndex, Set<String> ownedUpgrades) {
        this.uuid = uuid;
        this.islandIndex = islandIndex;
        this.ownedUpgrades = ownedUpgrades;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getIslandIndex() {
        return islandIndex;
    }

    public void setIslandIndex(int islandIndex) {
        this.islandIndex = islandIndex;
    }

    public boolean hasIsland() {
        return islandIndex != NO_ISLAND;
    }

    public Set<String> getOwnedUpgrades() {
        return ownedUpgrades;
    }

    public boolean hasUpgrade(String upgradeId) {
        return ownedUpgrades.contains(upgradeId);
    }

    public void addUpgrade(String upgradeId) {
        ownedUpgrades.add(upgradeId);
    }

    public void removeUpgrade(String upgradeId) {
        ownedUpgrades.remove(upgradeId);
    }

    public static PlayerHouseData createEmpty(UUID uuid) {
        return new PlayerHouseData(uuid, NO_ISLAND, new HashSet<>());
    }
}
