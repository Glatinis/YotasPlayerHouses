package com.github.Glatinis.yotasPlayerHouses.command;

import com.github.Glatinis.yotasPlayerHouses.upgrade.Upgrade;
import com.github.Glatinis.yotasPlayerHouses.upgrade.UpgradeManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class UpgradeSubCommand implements SubCommand {
    private final UpgradeManager upgradeManager;

    public UpgradeSubCommand(UpgradeManager upgradeManager) {
        this.upgradeManager = upgradeManager;
    }

    @Override
    public String getName() {
        return "upgrade";
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

        if (args.length < 1) {
            player.sendMessage("§cUsage: /playerhouse upgrade <id>");
            return;
        }

        upgradeManager.purchase(player, args[0], result -> player.sendMessage(switch (result) {
            case SUCCESS -> "§aUpgrade purchased!";
            case UNKNOWN_UPGRADE -> "§cThat upgrade does not exist.";
            case NO_ISLAND -> "§cYou need a house before you can upgrade it.";
            case ALREADY_OWNED -> "§cYou already own this upgrade.";
            case MISSING_REQUIREMENT -> "§cYou need to purchase the previous upgrade first.";
            case MISSING_PERMISSION -> "§cYou do not have permission to purchase this upgrade.";
            case INSUFFICIENT_FUNDS -> "§cYou do not have enough money for this upgrade.";
            case INSUFFICIENT_ITEMS -> "§cYou do not have the required items for this upgrade.";
            case SCHEMATIC_MISSING -> "§cThis upgrade is misconfigured, please contact an admin.";
            case HOUSE_WORLD_UNAVAILABLE -> "§cThe house world isn't available right now, please contact an admin.";
            case PASTE_FAILED -> "§cSomething went wrong applying this upgrade, please contact an admin.";
        }));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1)
            return List.of();

        return upgradeManager.getAll().stream()
                .map(Upgrade::getId)
                .filter(id -> id.startsWith(args[0]))
                .collect(Collectors.toList());
    }
}
