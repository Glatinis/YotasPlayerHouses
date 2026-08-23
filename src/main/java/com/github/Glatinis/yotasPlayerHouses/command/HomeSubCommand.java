package com.github.Glatinis.yotasPlayerHouses.command;

import com.github.Glatinis.yotasPlayerHouses.world.IslandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeSubCommand implements SubCommand {
    private final IslandManager islandManager;

    public HomeSubCommand(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @Override
    public String getName() {
        return "home";
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
        islandManager.teleportHome(player);
    }
}
