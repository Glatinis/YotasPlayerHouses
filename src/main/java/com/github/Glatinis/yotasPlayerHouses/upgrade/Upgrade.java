package com.github.Glatinis.yotasPlayerHouses.upgrade;

public class Upgrade {
    private final String id;
    private final String displayName;
    private final String category;
    private final String schematic;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final String requires;
    private final String permission;
    private final UpgradeCost cost;

    public Upgrade(String id, String displayName, String category, String schematic,
                    int offsetX, int offsetY, int offsetZ,
                    String requires, String permission, UpgradeCost cost) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.schematic = schematic;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.requires = requires;
        this.permission = permission;
        this.cost = cost;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    public String getSchematic() {
        return schematic;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public String getRequires() {
        return requires;
    }

    public boolean hasRequirement() {
        return requires != null && !requires.isEmpty();
    }

    public String getPermission() {
        return permission;
    }

    public boolean requiresPermission() {
        return permission != null && !permission.isEmpty();
    }

    public UpgradeCost getCost() {
        return cost;
    }
}
