package com.ultras.rtp.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/** Marks an inventory as the RTP GUI and remembers which slot maps to which world. */
public final class RtpGuiHolder implements InventoryHolder {
    private final Map<Integer, String> slots = new HashMap<>();
    private Inventory inventory;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void put(int slot, String world) {
        slots.put(slot, world);
    }

    public String worldAt(int slot) {
        return slots.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
