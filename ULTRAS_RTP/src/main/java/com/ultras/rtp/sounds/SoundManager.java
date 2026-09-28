package com.ultras.rtp.sounds;

import com.ultras.rtp.UltrasRTP;
import com.ultras.rtp.utils.YamlFiles;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Plays sounds defined in sounds.yml (global switch + optional per-player toggle). */
public final class SoundManager {
    private final UltrasRTP plugin;
    private volatile YamlConfiguration yaml;

    public SoundManager(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.yaml = YamlFiles.load(plugin, "sounds.yml");
    }

    public void play(Player player, String key) {
        YamlConfiguration cfg = yaml;
        if (cfg == null || player == null || !player.isOnline() || !cfg.getBoolean("enabled", true)) {
            return;
        }
        if (plugin.config().playerSoundToggle() && !plugin.storage().soundsEnabled(player.getUniqueId())) {
            return;
        }
        ConfigurationSection s = cfg.getConfigurationSection(key);
        if (s == null) {
            return;
        }
        String name = s.getString("sound", "none").trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.equals("none")) {
            return;
        }
        float volume = (float) Math.max(0D, s.getDouble("volume", 0.7D));
        float pitch = (float) Math.max(0.5D, Math.min(2D, s.getDouble("pitch", 1.0D)));
        player.playSound(player.getLocation(), name, SoundCategory.MASTER, volume, pitch);
    }
}
