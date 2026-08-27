package com.github.Glatinis.yotasPlayerHouses.core;

import com.github.Glatinis.yotasPlayerHouses.command.AcceptSubCommand;
import com.github.Glatinis.yotasPlayerHouses.command.AdminSubCommand;
import com.github.Glatinis.yotasPlayerHouses.command.HomeSubCommand;
import com.github.Glatinis.yotasPlayerHouses.command.HubSubCommand;
import com.github.Glatinis.yotasPlayerHouses.command.InviteSubCommand;
import com.github.Glatinis.yotasPlayerHouses.command.PlayerHouseCommand;
import com.github.Glatinis.yotasPlayerHouses.command.UpgradeSubCommand;
import com.github.Glatinis.yotasPlayerHouses.data.PlayerDataManager;
import com.github.Glatinis.yotasPlayerHouses.economy.EconomyManager;
import com.github.Glatinis.yotasPlayerHouses.invite.InviteManager;
import com.github.Glatinis.yotasPlayerHouses.listener.HouseProtectionListener;
import com.github.Glatinis.yotasPlayerHouses.listener.HouseRespawnListener;
import com.github.Glatinis.yotasPlayerHouses.schematic.SchematicManager;
import com.github.Glatinis.yotasPlayerHouses.upgrade.UpgradeManager;
import com.github.Glatinis.yotasPlayerHouses.world.HouseWorldManager;
import com.github.Glatinis.yotasPlayerHouses.world.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class YotasPlayerHouses extends JavaPlugin {
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private EconomyManager economyManager;
    private SchematicManager schematicManager;
    private HouseWorldManager houseWorldManager;
    private IslandManager islandManager;
    private UpgradeManager upgradeManager;
    private InviteManager inviteManager;

    @Override
    public void onEnable() {
        if (!checkDependency("Multiverse-Core") || !checkDependency("FastAsyncWorldEdit") || !checkDependency("Vault"))
            return;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        playerDataManager = new PlayerDataManager(this);
        economyManager = new EconomyManager(this);
        schematicManager = new SchematicManager(this);
        houseWorldManager = new HouseWorldManager(this, configManager);
        islandManager = new IslandManager(this, configManager, playerDataManager, schematicManager);
        upgradeManager = new UpgradeManager(this, configManager, economyManager, schematicManager, playerDataManager, islandManager);
        inviteManager = new InviteManager(this, configManager, playerDataManager, islandManager);

        registerCommands();
        registerListeners();

        getLogger().info("Loaded plugin!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Unloaded plugin!");
    }

    private boolean checkDependency(String pluginName) {
        if (Bukkit.getPluginManager().isPluginEnabled(pluginName))
            return true;

        getLogger().severe(pluginName + " is required to run this plugin!");
        getLogger().severe("Unloading...");
        Bukkit.getPluginManager().disablePlugin(this);
        return false;
    }

    private void registerCommands() {
        PlayerHouseCommand command = new PlayerHouseCommand(
                new HomeSubCommand(islandManager),
                new HubSubCommand(configManager),
                new UpgradeSubCommand(upgradeManager),
                new InviteSubCommand(inviteManager),
                new AcceptSubCommand(inviteManager),
                new AdminSubCommand(configManager, upgradeManager, islandManager)
        );

        PluginCommand playerHouseCommand = getCommand("playerhouse");
        if (playerHouseCommand == null) {
            getLogger().warning("Command 'playerhouse' is missing from plugin.yml.");
            return;
        }

        playerHouseCommand.setExecutor(command);
        playerHouseCommand.setTabCompleter(command);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new HouseProtectionListener(configManager), this);
        Bukkit.getPluginManager().registerEvents(new HouseRespawnListener(configManager, islandManager), this);
    }
}
