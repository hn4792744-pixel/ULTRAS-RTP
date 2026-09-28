package com.ultras.rtp.utils;

import com.ultras.rtp.language.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;
import java.util.Locale;

/** Builds GUI items from a YAML section (material, name, lore, custom-model-data, glow, enchants). */
public final class ItemFactory {
    private ItemFactory() {
    }

    public static ItemStack build(ConfigurationSection s, LanguageManager lang, TagResolver... resolvers) {
        Material material = Material.matchMaterial(s.getString("material", "STONE"));
        if (material == null || material.isAir() || !material.isItem()) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, s.getInt("amount", 1))));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        String name = s.getString("name");
        if (name != null) {
            meta.displayName(noItalic(lang.parse(name, resolvers)));
        }
        List<String> lore = s.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(line -> noItalic(lang.parse(line, resolvers))).toList());
        }
        int cmd = s.getInt("custom-model-data", 0);
        if (cmd > 0) {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setFloats(List.of((float) cmd));
            meta.setCustomModelDataComponent(component);
        }
        ConfigurationSection enchants = s.getConfigurationSection("enchants");
        if (enchants != null) {
            for (String key : enchants.getKeys(false)) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, Math.max(1, enchants.getInt(key, 1)), true);
                }
            }
        }
        if (s.getBoolean("glow", false)) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
