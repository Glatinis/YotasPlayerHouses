package com.github.Glatinis.yotasPlayerHouses.command;

import com.github.Glatinis.yotasPlayerHouses.invite.InviteManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InviteSubCommand implements SubCommand {
    private final InviteManager inviteManager;

    public InviteSubCommand(InviteManager inviteManager) {
        this.inviteManager = inviteManager;
    }

    @Override
    public String getName() {
        return "invite";
    }

    @Override
    public String getPermission() {
        return "yotasplayerhouses.use";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player owner)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        if (args.length < 1) {
            owner.sendMessage("§cUsage: /playerhouse invite <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            owner.sendMessage("§cThat player is not online.");
            return;
        }

        switch (inviteManager.invite(owner, target)) {
            case SENT -> {
                owner.sendMessage("§aInvite sent to " + target.getName() + ".");
                target.sendMessage("§a" + owner.getName() + " invited you to their house! Run §f/playerhouse accept §ato join.");
            }
            case SELF_INVITE -> owner.sendMessage("§cYou cannot invite yourself.");
            case OWNER_NO_ISLAND -> owner.sendMessage("§cYou need a house before you can invite people over.");
            case ALREADY_PENDING -> owner.sendMessage("§cThat player already has a pending invite.");
        }
    }
}
