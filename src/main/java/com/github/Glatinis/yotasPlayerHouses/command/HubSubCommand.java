package com.github.Glatinis.yotasPlayerHouses.command;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HubSubCommand implements SubCommand {
    private final ConfigManager configManager;

    public HubSubCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "hub";
    }

    @Override
    public String getPermission() {
        return "yotasplayerhouses.use";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        if (!configManager.isHubWorldLoaded()) {
            player.sendMessage("§cThe hub isn't available right now, please contact an admin.");
            return;
        }
        player.teleport(configManager.getHubLocation());
    }
}
