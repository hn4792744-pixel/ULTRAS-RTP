package com.ultras.rtp.configuration;

import com.ultras.rtp.UltrasRTP;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/** Typed access to config.yml. */
public final class ConfigManager {
    private final UltrasRTP plugin;

    public ConfigManager(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    private FileConfiguration c() {
        return plugin.getConfig();
    }

    public String language() {
        return c().getString("language", "en").toLowerCase(Locale.ROOT);
    }

    public boolean guiEnabled() {
        return c().getBoolean("gui.enabled", false);
    }

    public boolean actionbarEnabled() {
        return c().getBoolean("countdown.actionbar", true);
    }

    /** true = cooldown starts when the countdown begins; false = only after a successful RTP. */
    public boolean cooldownOnBegin() {
        return "START".equalsIgnoreCase(c().getString("cooldown.start-on", "SUCCESS"));
    }

    public boolean overrideConflicts() {
        return c().getBoolean("command.override-conflicts", true);
    }

    public boolean playerSoundToggle() {
        return c().getBoolean("player-sound-toggle.enabled", false);
    }

    public boolean fallbackEnabled() {
        return c().getBoolean("fallback.enabled", true);
    }

    public String fallbackTarget() {
        return c().getString("fallback.target-world", "world");
    }

    public String permUse() {
        return c().getString("permissions.use", "ultras.rtp.use");
    }

    public String permReload() {
        return c().getString("permissions.reload", "ultras.rtp.reload");
    }

    public String permBypassCooldown() {
        return c().getString("permissions.bypass-cooldown", "ultras.rtp.bypass.cooldown");
    }

    public String permBypassMove() {
        return c().getString("permissions.bypass-move", "ultras.rtp.bypass.move");
    }

    public String permAdmin() {
        return c().getString("permissions.admin", "ultras.rtp.admin");
    }
}
