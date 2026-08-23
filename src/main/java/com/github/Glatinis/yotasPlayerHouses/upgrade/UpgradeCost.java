package com.github.Glatinis.yotasPlayerHouses.upgrade;

import java.util.List;

public class UpgradeCost {
    private final double money;
    private final List<ItemCost> items;

    public UpgradeCost(double money, List<ItemCost> items) {
        this.money = money;
        this.items = items;
    }

    public double getMoney() {
        return money;
    }

    public List<ItemCost> getItems() {
        return items;
    }
}
