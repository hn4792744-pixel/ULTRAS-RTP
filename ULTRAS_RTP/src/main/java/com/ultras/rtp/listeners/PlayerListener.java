package com.ultras.rtp.listeners;

import com.ultras.rtp.UltrasRTP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final UltrasRTP plugin;

    public PlayerListener(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.rtp().quit(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.rtp().cancelCountdown(e.getEntity().getUniqueId(), "cancelled-death");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        plugin.rtp().cancelCountdown(e.getPlayer().getUniqueId(), "cancelled-world-change");
    }
}
