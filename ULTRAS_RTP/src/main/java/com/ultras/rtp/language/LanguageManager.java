package com.ultras.rtp.language;

import com.ultras.rtp.UltrasRTP;
import com.ultras.rtp.utils.Text;
import com.ultras.rtp.utils.YamlFiles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/** Loads languages/*.yml and renders MiniMessage strings. Falls back to en.yml, then to the key name. */
public final class LanguageManager {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final UltrasRTP plugin;
    private volatile YamlConfiguration active;
    private volatile YamlConfiguration fallback;

    public LanguageManager(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        YamlConfiguration en = YamlFiles.load(plugin, "languages/en.yml");
        YamlFiles.load(plugin, "languages/ar.yml"); // make sure it is extracted
        String code = plugin.config().language();
        File file = new File(plugin.getDataFolder(), "languages/" + code + ".yml");
        YamlConfiguration selected = en;
        if (file.exists()) {
            selected = YamlFiles.load(plugin, "languages/" + code + ".yml");
        } else {
            plugin.getLogger().warning("Language file languages/" + code + ".yml not found, using en.");
        }
        this.fallback = en;
        this.active = selected;
    }

    public static TagResolver time(long seconds) {
        return TagResolver.resolver(
                Placeholder.unparsed("time", Text.mono((int) seconds)),
                Placeholder.unparsed("seconds", Long.toString(seconds)));
    }

    public String raw(String key) {
        String path = "messages." + key;
        String value = active.getString(path);
        if (value == null) {
            value = fallback.getString(path);
        }
        return value != null ? value : "<red>Missing message: " + key;
    }

    private String prefix() {
        String p = active.getString("prefix");
        if (p == null) {
            p = fallback.getString("prefix", "ULTRAS |");
        }
        return p;
    }

    /** Parses any MiniMessage string (also used by the GUI); &lt;prefix&gt; is always available. */
    public Component parse(String text, TagResolver... resolvers) {
        return MM.deserialize(text, TagResolver.resolver(Placeholder.parsed("prefix", prefix()), TagResolver.resolver(resolvers)));
    }

    public Component component(String key, TagResolver... resolvers) {
        return parse(raw(key), resolvers);
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(component(key, resolvers));
    }
}
