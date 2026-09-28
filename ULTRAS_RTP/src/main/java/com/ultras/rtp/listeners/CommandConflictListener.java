package com.ultras.rtp.listeners;

import com.ultras.rtp.UltrasRTP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerLoadEvent;

import java.util.Arrays;
import java.util.Locale;

/**
 * Handles /rtp when another plugin registered the "rtp" label first. Uses only public Bukkit events:
 * the other plugin is never disabled, edited or unregistered, and unrelated commands are untouched.
 */
public final class CommandConflictListener implements Listener {
    private final UltrasRTP plugin;

    public CommandConflictListener(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    private boolean ownsLabel() {
        Command registered = Bukkit.getCommandMap().getCommand("rtp");
        return registered == null || registered == plugin.getCommand("rtp");
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent e) {
        if (e.getType() != ServerLoadEvent.LoadType.STARTUP || ownsLabel()) {
            return;
        }
        plugin.getLogger().warning("Another plugin owns the /rtp label. "
                + (plugin.config().overrideConflicts()
                ? "command.override-conflicts is ON: player /rtp is handled by ULTRAS_RTP (the other plugin is untouched)."
                : "command.override-conflicts is OFF: use /ultras_rtp:rtp or remove the other /rtp."));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreprocess(PlayerCommandPreprocessEvent e) {
        if (!plugin.config().overrideConflicts()) {
            return;
        }
        String message = e.getMessage();
        if (message.length() < 2) {
            return;
        }
        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0 || !parts[0].toLowerCase(Locale.ROOT).equals("rtp") || ownsLabel()) {
            return;
        }
        e.setCancelled(true);
        plugin.command().execute(e.getPlayer(), Arrays.copyOfRange(parts, 1, parts.length));
    }
}
