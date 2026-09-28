package com.ultras.rtp.world;

import com.ultras.rtp.UltrasRTP;
import com.ultras.rtp.models.HeightMode;
import com.ultras.rtp.models.WorldSettings;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reads the "worlds:" section of config.yml (any number of custom worlds). */
public final class WorldManager {
    private final UltrasRTP plugin;
    private volatile Map<String, WorldSettings> worlds = Map.of();

    public WorldManager(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        Map<String, WorldSettings> map = new LinkedHashMap<>();
        ConfigurationSection defs = plugin.getConfig().getConfigurationSection("defaults");
        if (defs == null) {
            defs = new MemoryConfiguration();
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("worlds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                try {
                    map.put(key.toLowerCase(Locale.ROOT), parse(key, s, defs));
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Invalid world config '" + key + "': " + ex.getMessage());
                }
            }
        }
        this.worlds = map;
    }

    private WorldSettings parse(String name, ConfigurationSection s, ConfigurationSection d) {
        int min = Math.max(0, s.getInt("min-radius", d.getInt("min-radius", 500)));
        int max = Math.max(min + 1, s.getInt("max-radius", d.getInt("max-radius", 5000)));
        HeightMode mode;
        try {
            mode = HeightMode.valueOf(s.getString("height-mode", d.getString("height-mode", "AUTO")).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            mode = HeightMode.AUTO;
        }
        List<String> biomes = s.contains("blocked-biomes") ? s.getStringList("blocked-biomes") : d.getStringList("blocked-biomes");
        Set<String> blocked = new HashSet<>();
        for (String b : biomes) {
            blocked.add(b.toLowerCase(Locale.ROOT).replace("minecraft:", ""));
        }
        return new WorldSettings(
                name,
                s.getString("display-name", name),
                s.getBoolean("enabled", false),
                min,
                max,
                s.getInt("center-x", 0),
                s.getInt("center-z", 0),
                Math.max(0, s.getInt("cooldown", d.getInt("cooldown", 30))),
                Math.max(0, s.getInt("countdown", d.getInt("countdown", 3))),
                s.getBoolean("cancel-on-move", d.getBoolean("cancel-on-move", true)),
                s.getDouble("movement-threshold", d.getDouble("movement-threshold", 0.3D)),
                Math.max(1, s.getInt("max-attempts", d.getInt("max-attempts", 30))),
                s.getBoolean("allow-water", d.getBoolean("allow-water", false)),
                mode,
                s.contains("scan-min-y") ? s.getInt("scan-min-y") : null,
                s.contains("scan-max-y") ? s.getInt("scan-max-y") : null,
                s.contains("fallback-to-world") ? s.getBoolean("fallback-to-world") : null,
                s.getString("permission", ""),
                blocked);
    }

    /** Settings for a world name (case-insensitive) or null if the world is not configured. */
    public WorldSettings get(String name) {
        return name == null ? null : worlds.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<WorldSettings> all() {
        return new ArrayList<>(worlds.values());
    }

    /** Enabled in config AND loaded on the server. */
    public boolean isUsable(WorldSettings s) {
        return s != null && s.enabled() && Bukkit.getWorld(s.name()) != null;
    }
}
