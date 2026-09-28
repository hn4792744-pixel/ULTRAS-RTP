package com.ultras.rtp.gui;

import com.ultras.rtp.UltrasRTP;
import com.ultras.rtp.models.WorldSettings;
import com.ultras.rtp.utils.ItemFactory;
import com.ultras.rtp.utils.YamlFiles;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds and handles the RTP GUI (design lives in guis/rtp.yml, visibility in config.yml gui.worlds). */
public final class GuiManager {
    private final UltrasRTP plugin;
    private volatile YamlConfiguration yaml;

    public GuiManager(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.yaml = YamlFiles.load(plugin, "guis/rtp.yml");
    }

    public void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof RtpGuiHolder) {
                p.closeInventory();
            }
        }
    }

    /** RTP enabled + world loaded + not hidden from the GUI. */
    public boolean isListed(WorldSettings s) {
        return s != null && plugin.worlds().isUsable(s)
                && plugin.getConfig().getBoolean("gui.worlds." + s.name() + ".enabled", true);
    }

    public List<WorldSettings> listedWorlds() {
        List<WorldSettings> out = new ArrayList<>();
        for (WorldSettings s : plugin.worlds().all()) {
            if (isListed(s)) {
                out.add(s);
            }
        }
        return out;
    }

    public void open(Player p) {
        List<WorldSettings> list = listedWorlds();
        if (list.isEmpty()) {
            plugin.lang().send(p, "gui-empty");
            plugin.sounds().play(p, "error");
            return;
        }
        YamlConfiguration cfg = yaml;
        int size = Math.max(9, Math.min(54, (cfg.getInt("size", 27) / 9) * 9));
        RtpGuiHolder holder = new RtpGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, size, plugin.lang().parse(cfg.getString("title", "ULTRAS RTP")));
        holder.setInventory(inv);

        ConfigurationSection filler = cfg.getConfigurationSection("filler");
        if (filler != null && filler.getBoolean("enabled", true)) {
            ItemStack item = ItemFactory.build(filler, plugin.lang());
            for (int i = 0; i < size; i++) {
                inv.setItem(i, item);
            }
        }
        ConfigurationSection border = cfg.getConfigurationSection("border");
        if (border != null && border.getBoolean("enabled", true)) {
            ItemStack item = ItemFactory.build(border, plugin.lang());
            for (int i = 0; i < size; i++) {
                if (isBorder(i, size)) {
                    inv.setItem(i, item);
                }
            }
        }

        Set<Integer> used = new HashSet<>();
        for (WorldSettings s : list) {
            int slot = plugin.getConfig().getInt("gui.worlds." + s.name() + ".slot",
                    cfg.getInt("items." + s.name() + ".slot", -1));
            if (slot < 0 || slot >= size || used.contains(slot)) {
                slot = autoSlot(size, used);
            }
            if (slot < 0) {
                continue;
            }
            ConfigurationSection sec = cfg.getConfigurationSection("items." + s.name());
            if (sec == null) {
                sec = cfg.getConfigurationSection("items.default");
            }
            if (sec == null) {
                sec = new MemoryConfiguration();
            }
            TagResolver resolvers = TagResolver.resolver(
                    Placeholder.unparsed("world", s.displayName()),
                    Placeholder.unparsed("name", s.name()),
                    Placeholder.unparsed("min", Integer.toString(s.minRadius())),
                    Placeholder.unparsed("max", Integer.toString(s.maxRadius())),
                    Placeholder.unparsed("countdown", Integer.toString(s.countdown())),
                    Placeholder.unparsed("cooldown", Integer.toString(s.cooldown())));
            inv.setItem(slot, ItemFactory.build(sec, plugin.lang(), resolvers));
            holder.put(slot, s.name());
            used.add(slot);
        }
        p.openInventory(inv);
        plugin.sounds().play(p, "gui-open");
    }

    /** Re-validates the world at click time (it may have been disabled while the GUI was open). */
    public void handleClick(Player p, String worldName) {
        plugin.sounds().play(p, "gui-click");
        WorldSettings s = plugin.worlds().get(worldName);
        if (!isListed(s)) {
            plugin.lang().send(p, "gui-world-disabled");
            plugin.sounds().play(p, "world-not-allowed");
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) {
                    open(p);
                }
            });
            return;
        }
        plugin.rtp().begin(p, s);
    }

    private static boolean isBorder(int slot, int size) {
        int rows = size / 9;
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    private static int autoSlot(int size, Set<Integer> used) {
        for (int i = 0; i < size; i++) {
            if (!used.contains(i) && !isBorder(i, size)) {
                return i;
            }
        }
        for (int i = 0; i < size; i++) {
            if (!used.contains(i)) {
                return i;
            }
        }
        return -1;
    }
}
