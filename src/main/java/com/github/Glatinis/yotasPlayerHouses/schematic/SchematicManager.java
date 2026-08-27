package com.github.Glatinis.yotasPlayerHouses.schematic;

import com.github.Glatinis.yotasPlayerHouses.core.YotasPlayerHouses;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SchematicManager {
    private final YotasPlayerHouses plugin;
    private final File schematicsFolder;
    private final Map<String, Clipboard> clipboardCache = new ConcurrentHashMap<>();

    public SchematicManager(YotasPlayerHouses plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");

        if (!schematicsFolder.exists() && !schematicsFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create schematics folder.");
        }
    }

    public File getSchematicsFolder() {
        return schematicsFolder;
    }

    public boolean exists(String fileName) {
        return new File(schematicsFolder, fileName).isFile();
    }

    public void clearCache() {
        clipboardCache.clear();
    }

    private Clipboard loadClipboard(String fileName) throws Exception {
        Clipboard cached = clipboardCache.get(fileName);
        if (cached != null)
            return cached;

        File file = new File(schematicsFolder, fileName);
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null)
            throw new IllegalArgumentException("Unknown schematic format for file '" + fileName + "'");

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            clipboardCache.put(fileName, clipboard);
            return clipboard;
        }
    }

    // Pastes off the main thread, then hands the result back on the main thread.
    public void pasteAsync(String fileName, Location origin, Runnable onSuccess, Consumer<Exception> onFailure) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                paste(fileName, origin);
                Bukkit.getScheduler().runTask(plugin, onSuccess);
            } catch (Exception exception) {
                Bukkit.getScheduler().runTask(plugin, () -> onFailure.accept(exception));
            }
        });
    }

    // Clears the footprint fileName's clipboard would occupy if pasted at origin (sets it to air),
    // without pasting anything back.
    public void clearAsync(String fileName, Location origin, Runnable onSuccess, Consumer<Exception> onFailure) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                clear(fileName, origin);
                Bukkit.getScheduler().runTask(plugin, onSuccess);
            } catch (Exception exception) {
                Bukkit.getScheduler().runTask(plugin, () -> onFailure.accept(exception));
            }
        });
    }

    // Clears the footprint clearFileName would occupy if pasted at clearOrigin, then pastes pasteFileName
    // at pasteOrigin. Used when replacing a structure with a smaller one (e.g. reverting an upgrade) so
    // blocks outside the new footprint don't linger behind.
    public void replaceAsync(String clearFileName, Location clearOrigin, String pasteFileName, Location pasteOrigin,
                              Runnable onSuccess, Consumer<Exception> onFailure) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                clear(clearFileName, clearOrigin);
                paste(pasteFileName, pasteOrigin);
                Bukkit.getScheduler().runTask(plugin, onSuccess);
            } catch (Exception exception) {
                Bukkit.getScheduler().runTask(plugin, () -> onFailure.accept(exception));
            }
        });
    }

    private void paste(String fileName, Location origin) throws Exception {
        Clipboard clipboard = loadClipboard(fileName);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(origin.getWorld());
        BlockVector3 pastePoint = BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pastePoint)
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(operation);
        }
    }

    // Sets every block fileName's clipboard would cover if pasted at origin back to air.
    private void clear(String fileName, Location origin) throws Exception {
        Clipboard clipboard = loadClipboard(fileName);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(origin.getWorld());
        BlockVector3 pastePoint = BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());

        // Shift the clipboard's own region by the same offset createPaste().to(pastePoint) would apply,
        // so we clear exactly the footprint the paste would have covered.
        BlockVector3 shift = pastePoint.subtract(clipboard.getOrigin());
        BlockVector3 min = clipboard.getRegion().getMinimumPoint().add(shift);
        BlockVector3 max = clipboard.getRegion().getMaximumPoint().add(shift);
        Region region = new CuboidRegion(weWorld, min, max);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
            editSession.setBlocks(region, BlockTypes.AIR.getDefaultState());
        }
    }
}
