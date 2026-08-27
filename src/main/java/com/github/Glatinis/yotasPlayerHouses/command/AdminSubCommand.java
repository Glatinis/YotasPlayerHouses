package com.github.Glatinis.yotasPlayerHouses.command;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import com.github.Glatinis.yotasPlayerHouses.upgrade.AdminActionResult;
import com.github.Glatinis.yotasPlayerHouses.upgrade.Upgrade;
import com.github.Glatinis.yotasPlayerHouses.upgrade.UpgradeManager;
import com.github.Glatinis.yotasPlayerHouses.world.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class AdminSubCommand implements SubCommand {
    private final ConfigManager configManager;
    private final UpgradeManager upgradeManager;
    private final IslandManager islandManager;

    public AdminSubCommand(ConfigManager configManager, UpgradeManager upgradeManager, IslandManager islandManager) {
        this.configManager = configManager;
        this.upgradeManager = upgradeManager;
        this.islandManager = islandManager;
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getPermission() {
        return "yotasplayerhouses.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String action = args[0].toLowerCase();

        if (action.equals("reload")) {
            configManager.reload();
            upgradeManager.load();
            sender.sendMessage("§aConfiguration reloaded.");
            return;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        switch (action) {
            case "list" -> handleList(sender, target);
            case "add" -> handleAdd(sender, args, target);
            case "remove" -> handleRemove(sender, args, target);
            case "reset" -> handleReset(sender, target);
            default -> sendUsage(sender);
        }
    }

    private void handleList(CommandSender sender, OfflinePlayer target) {
        List<String> owned = upgradeManager.getOwnedUpgradeIds(target.getUniqueId());
        if (owned.isEmpty()) {
            sender.sendMessage("§7" + target.getName() + " does not own any upgrades.");
            return;
        }
        sender.sendMessage("§6" + target.getName() + "'s upgrades: §f" + String.join(", ", owned));
    }

    private void handleAdd(CommandSender sender, String[] args, OfflinePlayer target) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /playerhouse admin add <player> <upgrade>");
            return;
        }
        upgradeManager.grant(target.getUniqueId(), args[2], result -> sender.sendMessage(adminMessage(result)));
    }

    private void handleRemove(CommandSender sender, String[] args, OfflinePlayer target) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /playerhouse admin remove <player> <upgrade>");
            return;
        }
        upgradeManager.revoke(target.getUniqueId(), args[2], result -> sender.sendMessage(adminMessage(result)));
    }

    private void handleReset(CommandSender sender, OfflinePlayer target) {
        islandManager.resetIsland(target.getUniqueId(), upgradeManager,
                () -> sender.sendMessage("§a" + target.getName() + "'s house progress has been reset."),
                exception -> sender.sendMessage("§cFailed to reset house: " + exception.getMessage()));
    }

    private String adminMessage(AdminActionResult result) {
        return switch (result) {
            case SUCCESS -> "§aDone.";
            case UNKNOWN_UPGRADE -> "§cThat upgrade does not exist.";
            case NO_ISLAND -> "§cThat player does not have a house yet.";
            case ALREADY_OWNED -> "§cThat player already owns this upgrade.";
            case NOT_OWNED -> "§cThat player does not own this upgrade.";
            case SCHEMATIC_MISSING -> "§cThis upgrade is misconfigured, please check its schematic file.";
            case PASTE_FAILED -> "§cSomething went wrong pasting the schematic.";
        };
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6/playerhouse admin list <player>");
        sender.sendMessage("§6/playerhouse admin add <player> <upgrade>");
        sender.sendMessage("§6/playerhouse admin remove <player> <upgrade>");
        sender.sendMessage("§6/playerhouse admin reset <player>");
        sender.sendMessage("§6/playerhouse admin reload");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return List.of("list", "add", "remove", "reset", "reload").stream()
                    .filter(action -> action.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        if (args.length == 2 && !args[0].equalsIgnoreCase("reload"))
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

        if (args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")))
            return upgradeManager.getAll().stream()
                    .map(Upgrade::getId)
                    .filter(id -> id.startsWith(args[2]))
                    .collect(Collectors.toList());

        return List.of();
    }
}
