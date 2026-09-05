# YotasPlayerHouses

Player owned, upgradable houses on individual islands in a dedicated Multiverse-Core world.
Upgrades are schematics pasted with WorldEdit (or FastAsyncWorldEdit) and paid for with Vault money
and/or items, all defined in `config.yml` so new upgrades can be added without touching code.

## Requirements

- Multiverse-Core
- WorldEdit, or FastAsyncWorldEdit if `fastasync: true` is set in `config.yml` (default: `false`,
  regular WorldEdit)
- Vault (with an economy plugin hooked in)

The plugin disables itself on startup if any of these are missing.

## How it works

- The first time a player runs `/playerhouse`, an island plot is created for them on a grid inside
  the house world and the `island.base-schematic` is pasted there for free.
- Players spend money and/or items to buy upgrades with `/playerhouse upgrade <id>`. Each upgrade
  pastes its own schematic at an offset from the island origin.
- The whole house world is protected: block break/place, explosions and fire are all blocked so the
  only way a house changes is through the upgrade system.
- Dying inside the house world respawns the player on their own island instead of wherever the server
  would otherwise send them.

## Adding a new upgrade

1. Drop the `.schem` file in `plugins/YotasPlayerHouses/schematics/`.
2. Add an entry under `upgrades:` in `config.yml`, for example:

```yaml
upgrades:
  island_farm_wheat:
    display-name: "Wheat Farm"
    category: island
    schematic: farm_wheat.schem
    offset: { x: 15, y: 0, z: -10 }
    requires: ""
    permission: ""
    cost:
      money: 300.0
      items:
        - material: WHEAT_SEEDS
          amount: 32
```

3. Run `/playerhouse admin reload` (or restart the server).

Fields:

- `requires` chains upgrades into a progression, leave blank for a standalone/root upgrade.
- `permission` gates the upgrade behind a permission node, useful for VIP-only tiers.
- `offset` is relative to the island origin, so tiers that replace the same structure should share
  the same offset while separate structures (farms, decorations) use their own.

Bad entries (missing schematic file, etc.) are skipped with a warning in the console instead of
crashing the plugin.

## Commands

| Command | Description |
| --- | --- |
| `/playerhouse` | Teleport to your house, creating it if needed |
| `/playerhouse upgrade <id>` | Purchase an upgrade |
| `/playerhouse invite <player>` | Send a one-time invite to your house |
| `/playerhouse accept` | Accept a pending invite |
| `/playerhouse hub` | Return to the hub location |
| `/playerhouse admin list <player>` | List a player's owned upgrades |
| `/playerhouse admin add <player> <upgrade>` | Grant an upgrade for free |
| `/playerhouse admin remove <player> <upgrade>` | Revoke an upgrade |
| `/playerhouse admin reset <player>` | Wipe a player's progress back to the base house |
| `/playerhouse admin relocate <player>` | Re-pin a player's house to your current location (no blocks touched) |
| `/playerhouse admin reload` | Reload config.yml and upgrades |
| `/playerhouse admin fixworld [confirm]` | Repair the house world, or recreate it with `confirm` if broken |

Island locations are pinned in place once built, so changing `island.spacing`/`row-length`/`y-level`
only affects new islands. Use `admin relocate` to fix a house that was mispinned before this existed.

`fixworld` first tries a plain reload (no data touched). If the world folder itself is broken, it
reports that and does nothing further until run again as `admin fixworld confirm`, which moves the
broken folder aside (kept on disk, not deleted) and generates a fresh world. Run `admin reset <player>`
for each existing player afterwards to give them a base house again.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `yotasplayerhouses.use` | true | Base command access |
| `yotasplayerhouses.admin` | op | Admin subcommands |
| `yotasplayerhouses.admin.bypass` | op | Bypass house world block protection |
