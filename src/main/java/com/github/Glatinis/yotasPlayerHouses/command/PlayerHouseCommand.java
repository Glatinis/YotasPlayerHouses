package com.github.Glatinis.yotasPlayerHouses.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerHouseCommand implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final SubCommand defaultSubCommand;

    public PlayerHouseCommand(SubCommand... commands) {
        for (SubCommand command : commands) {
            subCommands.put(command.getName().toLowerCase(), command);
        }
        this.defaultSubCommand = subCommands.get("home");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            runDefault(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sendHelp(sender);
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage("§cYou do not have permission to do that.");
            return true;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    private void runDefault(CommandSender sender) {
        if (defaultSubCommand == null)
            return;
        if (!sender.hasPermission(defaultSubCommand.getPermission())) {
            sender.sendMessage("§cYou do not have permission to do that.");
            return;
        }
        defaultSubCommand.execute(sender, new String[0]);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6Player Houses");
        sender.sendMessage("§7/playerhouse §f- teleport to your house");
        sender.sendMessage("§7/playerhouse upgrade <id> §f- purchase an upgrade");
        sender.sendMessage("§7/playerhouse invite <player> §f- invite a player over");
        sender.sendMessage("§7/playerhouse accept §f- accept a pending invite");
        sender.sendMessage("§7/playerhouse hub §f- return to the hub");
        if (sender.hasPermission("yotasplayerhouses.admin"))
            sender.sendMessage("§7/playerhouse admin §f- manage player houses");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return subCommands.values().stream()
                    .filter(sub -> sender.hasPermission(sub.getPermission()))
                    .map(SubCommand::getName)
                    .filter(name -> name.startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null)
                return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        return new ArrayList<>();
    }
}
