package com.ultras.rtp.storage;

import com.ultras.rtp.UltrasRTP;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Per-player settings (UUID keyed) in plugins/ULTRAS_RTP/data/players.yml. Writes happen off the main thread. */
public final class PlayerDataStorage {
    private final UltrasRTP plugin;
    private final Path file;
    private final YamlConfiguration data;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ULTRAS_RTP-IO");
        t.setDaemon(true);
        return t;
    });

    public PlayerDataStorage(UltrasRTP plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "data");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        this.file = new File(dir, "players.yml").toPath();
        this.data = YamlConfiguration.loadConfiguration(file.toFile());
    }

    public synchronized boolean soundsEnabled(UUID id) {
        return data.getBoolean("players." + id + ".sounds", true);
    }

    public void setSoundsEnabled(UUID id, boolean enabled) {
        String snapshot;
        synchronized (this) {
            data.set("players." + id + ".sounds", enabled);
            snapshot = data.saveToString();
        }
        try {
            io.execute(() -> write(snapshot));
        } catch (RuntimeException ex) {
            write(snapshot); // executor already closed (shutdown)
        }
    }

    private void write(String content) {
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not save players.yml", ex);
        }
    }

    public void close() {
        io.shutdown();
        try {
            io.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
