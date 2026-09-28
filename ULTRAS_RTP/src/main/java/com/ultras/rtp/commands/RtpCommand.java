package com.ultras.rtp.commands;

import com.ultras.rtp.UltrasRTP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/** /rtp, /rtp reload (permission only), optional /rtp sounds. */
public final class RtpCommand implements CommandExecutor, TabCompleter {
    private final UltrasRTP plugin;

    public RtpCommand(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return execute(sender, args);
    }

    /** Shared by the registered command and the conflict-override listener. */
    public boolean execute(CommandSender sender, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if (sub.equals("reload")) {
            return reload(sender);
        }
        if (!(sender instanceof Player p)) {
            plugin.lang().send(sender, "player-only");
            return true;
        }
        if (sub.equals("sounds") && plugin.config().playerSoundToggle()) {
            boolean now = !plugin.storage().soundsEnabled(p.getUniqueId());
            plugin.storage().setSoundsEnabled(p.getUniqueId(), now);
            plugin.lang().send(p, now ? "sounds-enabled" : "sounds-disabled");
            if (now) {
                plugin.sounds().play(p, "gui-click");
            }
            return true;
        }
        plugin.rtp().handleCommand(p);
        return true;
    }

    private boolean canReload(CommandSender s) {
        return s.hasPermission(plugin.config().permReload()) || s.hasPermission(plugin.config().permAdmin());
    }

    private boolean reload(CommandSender sender) {
        if (!canReload(sender)) {
            plugin.lang().send(sender, "no-permission");
            if (sender instanceof Player p) {
                plugin.sounds().play(p, "error");
            }
            return true;
        }
        try {
            plugin.reloadEverything();
            plugin.lang().send(sender, "reload-success");
            if (sender instanceof Player p) {
                plugin.sounds().play(p, "reload");
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Reload failed", ex);
            plugin.lang().send(sender, "reload-failed");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length != 1) {
            return out;
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        if (canReload(sender) && "reload".startsWith(prefix)) {
            out.add("reload");
        }
        if (plugin.config().playerSoundToggle() && sender instanceof Player && "sounds".startsWith(prefix)) {
            out.add("sounds");
        }
        return out;
    }
}
