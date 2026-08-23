package com.github.Glatinis.yotasPlayerHouses.command;

import com.github.Glatinis.yotasPlayerHouses.invite.InviteManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptSubCommand implements SubCommand {
    private final InviteManager inviteManager;

    public AcceptSubCommand(InviteManager inviteManager) {
        this.inviteManager = inviteManager;
    }

    @Override
    public String getName() {
        return "accept";
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

        switch (inviteManager.accept(player)) {
            case SUCCESS -> player.sendMessage("§aTeleported to the house!");
            case NO_PENDING_INVITE -> player.sendMessage("§cYou do not have a pending invite.");
            case EXPIRED -> player.sendMessage("§cThat invite has expired.");
            case OWNER_OFFLINE -> player.sendMessage("§cThe player who invited you is no longer online.");
        }
    }
}
