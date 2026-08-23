package com.github.Glatinis.yotasPlayerHouses.invite;

import java.util.UUID;

public class PendingInvite {
    private final UUID ownerUuid;
    private final long expiresAtMillis;

    public PendingInvite(UUID ownerUuid, long expiresAtMillis) {
        this.ownerUuid = ownerUuid;
        this.expiresAtMillis = expiresAtMillis;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAtMillis;
    }
}
