package com.braur0.PartsVitality.model;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PartHP {

    public static final String HEAD = "head";
    public static final String CHEST = "chest";
    public static final String LEGS = "legs";
    public static final String FEET = "feet";
    public static final List<String> ALL_PARTS = Arrays.asList(HEAD, CHEST, LEGS, FEET);

    private static FileConfiguration config;

    // A map to temporarily store original durability values when displaying part HP
    private final transient Map<Integer, Integer> originalDamageMap = new HashMap<>();

    private final Map<String, Double> partHPMap = new ConcurrentHashMap<>();

    public PartHP() {
        // Set initial HP
        partHPMap.put(HEAD, getMaxHPPerPart(HEAD));
        partHPMap.put(CHEST, getMaxHPPerPart(CHEST));
        partHPMap.put(LEGS, getMaxHPPerPart(LEGS));
        partHPMap.put(FEET, getMaxHPPerPart(FEET));
    }

    public double getPartHP(String part) {
        return partHPMap.getOrDefault(part, 0.0);
    }

    public void setPartHP(String part, double hp) {
        partHPMap.put(part, Math.max(0, Math.min(getMaxHPPerPart(part), hp)));
    }

    public Map<String, Double> getPartHPMap() {
        return partHPMap;
    }

    public static void loadConfig(FileConfiguration configuration) {
        config = configuration;
    }

    public static double getMaxHPPerPart(String part) {
        return config.getDouble("parts." + part.toLowerCase() + ".max-hp", 20.0);
    }

    public static String getPartNameFromArmorSlot(int slot) {
        return switch (slot) {
            case 39 -> HEAD;  // Helmet slot
            case 38 -> CHEST; // Chestplate slot
            case 37 -> LEGS;  // Leggings slot
            case 36 -> FEET;  // Boots slot
            default -> null;
        };
    }

    private String getPartNameFromEquipmentSlot(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> HEAD;
            case CHEST -> CHEST;
            case LEGS -> LEGS;
            case FEET -> FEET;
            default -> null;
        };
    }

    public List<String> getRemainingParts() {
        return partHPMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Updates the display of the player's armor (Durability or Part HP).
     * @param player The target player.
     * @param showPartHP Whether to show part HP.
     * @param clickedSlot The slot number that was clicked.
     */
    public void updateArmorDisplay(Player player, boolean showPartHP, int clickedSlot) {
        // Iterate over armor slots directly to ensure changes are applied.
        // Using getArmorContents() returns a copy, so modifications won't be saved.
        for (EquipmentSlot equipmentSlot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack armor = player.getInventory().getItem(equipmentSlot);
            int inventoryArrayIndex = getInventoryArrayIndex(equipmentSlot);

            if (armor == null || armor.getType().isAir()) continue;

            ItemMeta meta = armor.getItemMeta();
            if (!(meta instanceof Damageable damageable)) continue;

            if (showPartHP) {
                // Store the original durability
                originalDamageMap.putIfAbsent(inventoryArrayIndex, damageable.getDamage());

                // Convert part HP to a durability bar
                String partName = getPartNameFromEquipmentSlot(equipmentSlot);
                if (partName == null) continue;

                double current = getPartHP(partName);
                double max = getMaxHPPerPart(partName);
                double hpRatio = (max > 0) ? current / max : 0.0;

                int maxDurability = armor.getType().getMaxDurability();
                int displayDamage = (int) (maxDurability * (1.0 - hpRatio));

                damageable.setDamage(displayDamage);

                // Manually convert to slot number as EquipmentSlot.getSlot() is not available in 1.20.1
                int currentArmorSlot = switch (equipmentSlot) {
                    case HEAD -> 39;
                    case CHEST -> 38;
                    case LEGS -> 37;
                    case FEET -> 36;
                    default -> -1;
                };

                if (currentArmorSlot == clickedSlot) {
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    meta.removeEnchant(Enchantment.DURABILITY);
                }

            } else {
                // Revert to the saved original durability
                Integer originalDamage = originalDamageMap.get(inventoryArrayIndex);
                if (originalDamage != null) damageable.setDamage(originalDamage);
                // Remove all enchantments
                meta.removeEnchant(Enchantment.DURABILITY);
            }
            armor.setItemMeta(meta);
            // Set the modified item back to the slot
            player.getInventory().setItem(equipmentSlot, armor);
        }
        if (!showPartHP) originalDamageMap.clear(); // Clear saved data when reverting the display
    }

    private int getInventoryArrayIndex(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 3;
            case CHEST -> 2;
            case LEGS -> 1;
            case FEET -> 0;
            default -> -1;
        };
    }
}
