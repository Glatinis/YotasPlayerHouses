package com.github.Glatinis.yotasPlayerHouses.invite;

import com.github.Glatinis.yotasPlayerHouses.core.ConfigManager;
import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import com.github.Glatinis.yotasPlayerHouses.data.PlayerDataManager;
import com.github.Glatinis.yotasPlayerHouses.world.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InviteManager {
    private final YotasPlayerHouses plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final IslandManager islandManager;

    private final Map<UUID, PendingInvite> pendingInvites = new HashMap<>();

    public InviteManager(YotasPlayerHouses plugin, ConfigManager configManager,
                          PlayerDataManager playerDataManager, IslandManager islandManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.islandManager = islandManager;
    }

    public InviteResult invite(Player owner, Player target) {
        if (owner.getUniqueId().equals(target.getUniqueId()))
            return InviteResult.SELF_INVITE;

        if (!playerDataManager.get(owner.getUniqueId()).hasIsland())
            return InviteResult.OWNER_NO_ISLAND;

        PendingInvite existing = pendingInvites.get(target.getUniqueId());
        if (existing != null && !existing.isExpired())
            return InviteResult.ALREADY_PENDING;

        int expirySeconds = configManager.getInviteExpirySeconds();
        long expiresAt = System.currentTimeMillis() + (expirySeconds * 1000L);
        pendingInvites.put(target.getUniqueId(), new PendingInvite(owner.getUniqueId(), expiresAt));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingInvite invite = pendingInvites.get(target.getUniqueId());
            if (invite != null && invite.getOwnerUuid().equals(owner.getUniqueId()) && invite.isExpired())
                pendingInvites.remove(target.getUniqueId());
        }, expirySeconds * 20L);

        return InviteResult.SENT;
    }

    public AcceptResult accept(Player target) {
        PendingInvite invite = pendingInvites.get(target.getUniqueId());
        if (invite == null)
            return AcceptResult.NO_PENDING_INVITE;

        if (invite.isExpired()) {
            pendingInvites.remove(target.getUniqueId());
            return AcceptResult.EXPIRED;
        }

        Player owner = Bukkit.getPlayer(invite.getOwnerUuid());
        if (owner == null || !owner.isOnline())
            return AcceptResult.OWNER_OFFLINE;

        Location origin = islandManager.getIslandOrigin(owner);
        if (origin == null)
            return AcceptResult.HOUSE_UNAVAILABLE;

        pendingInvites.remove(target.getUniqueId());
        target.teleport(origin.clone().add(0.0, 1.0, 0.0));
        return AcceptResult.SUCCESS;
    }
}
