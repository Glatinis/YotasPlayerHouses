package com.github.Glatinis.yotasPlayerHouses.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerHouseData {
    public static final int NO_ISLAND = -1;
    // Sentinel for "no origin pinned yet" (0,0 is a legitimate origin, so it can't double as "unset").
    public static final double NO_ORIGIN = Double.NaN;

    private final UUID uuid;
    private int islandIndex;
    private final Set<String> ownedUpgrades;
    // Coordinates the base schematic was actually pasted at. Pinned once so a later change to the grid
    // config (spacing/row-length/y-level) can't move an existing house out from under its player.
    private double originX;
    private double originY;
    private double originZ;

    public PlayerHouseData(UUID uuid, int islandIndex, Set<String> ownedUpgrades) {
        this(uuid, islandIndex, ownedUpgrades, NO_ORIGIN, NO_ORIGIN, NO_ORIGIN);
    }

    public PlayerHouseData(UUID uuid, int islandIndex, Set<String> ownedUpgrades,
                            double originX, double originY, double originZ) {
        this.uuid = uuid;
        this.islandIndex = islandIndex;
        this.ownedUpgrades = ownedUpgrades;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
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

    public boolean hasOrigin() {
        return !Double.isNaN(originX) && !Double.isNaN(originY) && !Double.isNaN(originZ);
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginY() {
        return originY;
    }

    public double getOriginZ() {
        return originZ;
    }

    public void setOrigin(double x, double y, double z) {
        this.originX = x;
        this.originY = y;
        this.originZ = z;
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
