package com.braur0.PartsVitality.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;

import com.braur0.PartsVitality.model.PartHP;
import com.braur0.PartsVitality.PartsVitality;
import com.braur0.PartsVitality.config.Lang;
import com.braur0.PartsVitality.manager.ArmorStatsManager;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import org.bukkit.inventory.ItemStack;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInventoryListener implements Listener {

    private final PartsVitality plugin;
    private final ArmorStatsManager armorStatsManager;

    // A set to manage players who are currently viewing part HP
    private final Set<UUID> viewingPartHP = ConcurrentHashMap.newKeySet();

    public PlayerInventoryListener(PartsVitality plugin, ArmorStatsManager armorStatsManager) {
        this.plugin = plugin;
        this.armorStatsManager = armorStatsManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UUID playerUUID = player.getUniqueId();

        // This listener only acts when an armor slot is shift-right-clicked
        if (event.getClick() == ClickType.SHIFT_RIGHT && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            // Cancel the original action (like moving items) because this is a display toggle operation
            event.setCancelled(true);

            if (viewingPartHP.contains(playerUUID)) {
                viewingPartHP.remove(playerUUID);
                player.sendMessage(Lang.get("inventory-display-durability"));
                PartHP partHP = armorStatsManager.getOrCreatePartHP(player);
                partHP.updateArmorDisplay(player, false, -1);
            } else {
                viewingPartHP.add(playerUUID);
                player.sendMessage(Lang.get("inventory-display-part-hp"));
                // Pass -1 to avoid making the slot glow, as specifying a slot would cause it to glow
                PartHP partHP = armorStatsManager.getOrCreatePartHP(player);
                partHP.updateArmorDisplay(player, true, -1);
            }
            player.updateInventory();
            return; // End processing here
        }

        // If the player is in part HP display mode, prevent any armor manipulation.
        if (viewingPartHP.contains(playerUUID)) {
            // Prevent equipping armor via Shift+Click from the main inventory.
            if (event.isShiftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && isArmor(clickedItem)) {
                    event.setCancelled(true);
                }
            }
            // Prevent swapping/moving armor in the armor slots.
            else if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // If in part HP display mode, revert the display
        if (viewingPartHP.remove(player.getUniqueId())) {
            armorStatsManager.getOrCreatePartHP(player).updateArmorDisplay(player, false, -1);
        }
    }

    public boolean isViewingPartHP(Player player) {
        return viewingPartHP.contains(player.getUniqueId());
    }

    public void resetViewingState(Player player) {
        viewingPartHP.remove(player.getUniqueId());
    }

    /**
     * Checks if the given ItemStack is an armor piece.
     * @param item The ItemStack to check.
     * @return true if it is an armor piece, false otherwise.
     */
    private boolean isArmor(ItemStack item) {
        if (item == null) return false;
        String typeName = item.getType().name();
        return typeName.endsWith("_HELMET") || typeName.endsWith("_CHESTPLATE") ||
               typeName.endsWith("_LEGGINGS") || typeName.endsWith("_BOOTS") ||
               typeName.equals("ELYTRA");
    }
}