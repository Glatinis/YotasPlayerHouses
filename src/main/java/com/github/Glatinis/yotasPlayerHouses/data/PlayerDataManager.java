package com.github.Glatinis.yotasPlayerHouses.data;

import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final YotasPlayerHouses plugin;
    private final File playerDataFolder;
    private final File stateFile;
    private final Map<UUID, PlayerHouseData> cache = new HashMap<>();

    private int nextIslandIndex;

    public PlayerDataManager(YotasPlayerHouses plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.stateFile = new File(plugin.getDataFolder(), "state.yml");

        if (!playerDataFolder.exists() && !playerDataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create playerdata folder.");
        }

        loadState();
    }

    // State

    private void loadState() {
        if (!stateFile.exists()) {
            nextIslandIndex = 0;
            return;
        }
        YamlConfiguration state = YamlConfiguration.loadConfiguration(stateFile);
        nextIslandIndex = state.getInt("next-island-index", 0);
    }

    private void saveState() {
        YamlConfiguration state = new YamlConfiguration();
        state.set("next-island-index", nextIslandIndex);
        try {
            state.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save state.yml: " + exception.getMessage());
        }
    }

    public synchronized int allocateIslandIndex() {
        int index = nextIslandIndex;
        nextIslandIndex++;
        saveState();
        return index;
    }

    // Player data

    public PlayerHouseData get(UUID uuid) {
        PlayerHouseData cached = cache.get(uuid);
        if (cached != null)
            return cached;

        PlayerHouseData loaded = load(uuid);
        cache.put(uuid, loaded);
        return loaded;
    }

    private PlayerHouseData load(UUID uuid) {
        File file = new File(playerDataFolder, uuid.toString() + ".yml");
        if (!file.exists())
            return PlayerHouseData.createEmpty(uuid);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int islandIndex = yaml.getInt("island-index", PlayerHouseData.NO_ISLAND);
        HashSet<String> ownedUpgrades = new HashSet<>(yaml.getStringList("owned-upgrades"));
        return new PlayerHouseData(uuid, islandIndex, ownedUpgrades);
    }

    public void save(PlayerHouseData data) {
        File file = new File(playerDataFolder, data.getUuid().toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("island-index", data.getIslandIndex());
        yaml.set("owned-upgrades", new ArrayList<>(data.getOwnedUpgrades()));
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save player data for " + data.getUuid() + ": " + exception.getMessage());
        }
    }
}
