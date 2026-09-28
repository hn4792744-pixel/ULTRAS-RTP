package com.ultras.rtp.listeners;

import com.ultras.rtp.UltrasRTP;
import com.ultras.rtp.gui.RtpGuiHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GuiListener implements Listener {
    private final UltrasRTP plugin;

    public GuiListener(UltrasRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof RtpGuiHolder holder)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getInventory()) {
            return;
        }
        String world = holder.worldAt(e.getSlot());
        if (world == null) {
            return;
        }
        p.closeInventory();
        plugin.gui().handleClick(p, world);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof RtpGuiHolder) {
            e.setCancelled(true);
        }
    }
}
