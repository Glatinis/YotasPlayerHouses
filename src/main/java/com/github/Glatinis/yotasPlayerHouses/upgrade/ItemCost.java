package com.github.Glatinis.yotasPlayerHouses.upgrade;

import org.bukkit.Material;

public class ItemCost {
    private final Material material;
    private final int amount;

    public ItemCost(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }
}
