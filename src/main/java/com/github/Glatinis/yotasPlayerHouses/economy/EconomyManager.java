package com.github.Glatinis.yotasPlayerHouses.economy;

import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final YotasPlayerHouses plugin;
    private Economy economy;

    public EconomyManager(YotasPlayerHouses plugin) {
        this.plugin = plugin;
        setup();
    }

    private boolean setup() {
        RegisteredServiceProvider<Economy> provider = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (provider == null)
            return false;

        economy = provider.getProvider();
        return true;
    }

    public boolean isReady() {
        return economy != null;
    }

    public boolean hasBalance(OfflinePlayer player, double amount) {
        return isReady() && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return isReady() && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (!isReady())
            return String.valueOf(amount);
        return economy.format(amount);
    }
}
