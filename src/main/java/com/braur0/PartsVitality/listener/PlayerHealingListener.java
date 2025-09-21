package com.braur0.PartsVitality.listener;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.braur0.PartsVitality.PartsVitality;
import com.braur0.PartsVitality.config.Lang;
import com.braur0.PartsVitality.config.PluginConfig;
import com.braur0.PartsVitality.manager.ArmorStatsManager;
import com.braur0.PartsVitality.model.PartHP;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHealingListener implements Listener {

    private final PartsVitality plugin;
    private final ArmorStatsManager armorStatsManager;
    private final ArmorDamageListener armorDamageListener;
    private final PlayerInventoryListener playerInventoryListener;

    // Item settings
    private final Map<Material, Double> healingItems = new HashMap<>();
    private final Map<Material, Boolean> surgeryItems = new HashMap<>();

    // Task management
    private final Map<UUID, BukkitTask> healingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> healingSlots = new ConcurrentHashMap<>(); // Store the slot being healed
    private final Map<UUID, BukkitTask> healingSoundTasks = new ConcurrentHashMap<>(); // Store the healing sound task

    private final PluginConfig config;

    public PlayerHealingListener(PartsVitality plugin, ArmorStatsManager armorStatsManager, ArmorDamageListener armorDamageListener, PlayerInventoryListener playerInventoryListener, PluginConfig config) {
        this.plugin = plugin;
        this.armorStatsManager = armorStatsManager;
        this.armorDamageListener = armorDamageListener;
        this.playerInventoryListener = playerInventoryListener;
        this.config = config;

        loadHealingItems();
        loadSurgeryItems();
    }

    private void loadHealingItems() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("healing-items");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            Material material = Material.getMaterial(key.toUpperCase());
            if (material != null) {
                healingItems.put(material, section.getDouble(key));
            }
        }
    }

    private void loadSurgeryItems() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("surgery-items");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            Material material = Material.getMaterial(key.toUpperCase());
            if (material != null) {
                surgeryItems.put(material, section.getBoolean(key));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Disable inventory operations during healing
        if (healingTasks.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // If holding a healing item on the cursor and left-clicking an armor slot
        if (event.getSlotType() != InventoryType.SlotType.ARMOR || event.getClick() != ClickType.LEFT) return;

        // Ignore clicks outside the player inventory's armor slots (36-39)
        String partName = PartHP.getPartNameFromArmorSlot(event.getSlot());
        if (partName == null) return;

        ItemStack cursorItem = event.getCursor();
        if (cursorItem == null || cursorItem.getType().isAir()) return;

        Material itemType = cursorItem.getType();
        boolean isHealingItem = healingItems.containsKey(itemType);
        boolean isSurgeryItem = surgeryItems.containsKey(itemType);

        if (!isHealingItem && !isSurgeryItem) return;

        event.setCancelled(true);
        
        final int slot = event.getSlot();
        PartHP partHP = armorStatsManager.getOrCreatePartHP(player);
        double currentPartHP = partHP.getPartHP(partName);

        if (isHealingItem) {
            handleHealing(player, partHP, partName, currentPartHP, cursorItem, slot);
        } else { // isSurgeryItem
            handleSurgery(player, partHP, partName, currentPartHP, cursorItem, slot);
        }
    }

    private void handleHealing(Player player, PartHP partHP, String partName, double currentPartHP, ItemStack cursorItem, int slot) {

        // Cannot heal if the part HP is full
        double maxPartHP = PartHP.getMaxHPPerPart(partName);
        if (currentPartHP >= maxPartHP) {
            player.sendMessage(Lang.get("healing-fail-healthy"));
            return;
        }

        // Cannot heal if part HP is 0 (destroyed)
        if (currentPartHP <= 0) {
            player.sendMessage(Lang.get("healing-fail-broken"));
            return;
        }

        // Consume one healing item
        cursorItem.setAmount(cursorItem.getAmount() - 1);

        final Material healingMaterial = cursorItem.getType(); // Store the type of healing item

        player.sendMessage(Lang.get("healing-start", Map.of("duration", String.valueOf(config.healingDurationTicks / 20))));

        // Start the healing sound task (wrapping cloth sound)
        BukkitTask soundTask = new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), config.healingSound, config.healingSoundVolume, config.healingSoundPitch);
            }
        }.runTaskTimer(plugin, 0L, config.healingSoundInterval);

        healingSoundTasks.put(player.getUniqueId(), soundTask);

        // Make the part being healed glow
        healingSlots.put(player.getUniqueId(), slot);
        partHP.updateArmorDisplay(player, true, slot);
        player.updateInventory();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Execute healing
                double healAmount = healingItems.get(healingMaterial);
                double currentHP = partHP.getPartHP(partName);
                double maxHP = PartHP.getMaxHPPerPart(partName);
                double newHP = Math.min(maxHP, currentHP + healAmount);
                partHP.setPartHP(partName, newHP);

                // Recalculate player's health to sync with the total part HP
                double totalMaxPartHP = PartHP.ALL_PARTS.stream().mapToDouble(PartHP::getMaxHPPerPart).sum();
                double totalCurrentPartHP = PartHP.ALL_PARTS.stream().mapToDouble(partHP::getPartHP).sum();
                double healthRatio = totalCurrentPartHP / totalMaxPartHP;

                // Always calculate health based on the vanilla default max health (20.0)
                double baseMaxHealth = 20.0;
                double newPlayerHealth = baseMaxHealth * healthRatio;

                double maxPlayerHP = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                player.setHealth(Math.min(maxPlayerHP, newPlayerHealth));

                // Healing doesn't fix a broken part, but the debuff state might change, so update it
                armorDamageListener.updateDebuffs(player, partHP);
                armorDamageListener.updateHealthPenalty(player, partHP);

                // If in part HP display mode, update the display immediately
                // Since healing is complete, do not specify a slot to glow (-1)
                if (playerInventoryListener.isViewingPartHP(player)) {
                    partHP.updateArmorDisplay(player, true, -1);
                } else {
                    // If not in display mode, revert to normal display to remove the glow
                    partHP.updateArmorDisplay(player, false, -1);
                }

                player.sendMessage(Lang.get("healing-complete"));
                player.playSound(player.getLocation(), config.healingCompleteSound, config.healingCompleteSoundVolume, config.healingCompleteSoundPitch);
                healingSlots.remove(player.getUniqueId());
                healingTasks.remove(player.getUniqueId());
                stopHealingSound(player); // Stop the healing sound
            }
        }.runTaskLater(plugin, config.healingDurationTicks);

        healingTasks.put(player.getUniqueId(), task);
    }

    private void handleSurgery(Player player, PartHP partHP, String partName, double currentPartHP, ItemStack cursorItem, int slot) {
        // Cannot perform surgery if the part is not destroyed
        if (currentPartHP > 0) {
            player.sendMessage(Lang.get("surgery-fail-not-broken"));
            return;
        }

        // Consume one surgery item
        cursorItem.setAmount(cursorItem.getAmount() - 1);

        player.sendMessage(Lang.get("surgery-start", Map.of("duration", String.valueOf(config.surgeryDurationTicks / 20))));

        // Start the surgery sound task
        BukkitTask soundTask = new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), config.surgerySound, config.surgerySoundVolume, config.surgerySoundPitch);
            }
        }.runTaskTimer(plugin, 0L, config.surgerySoundInterval);
        healingSoundTasks.put(player.getUniqueId(), soundTask);

        // Make the part being operated on glow
        healingSlots.put(player.getUniqueId(), slot);
        partHP.updateArmorDisplay(player, true, slot);
        player.updateInventory();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Execute surgery
                partHP.setPartHP(partName, config.surgeryRestoredHp);

                // Recalculate max HP penalty (as one broken part is now fixed)
                armorDamageListener.updateHealthPenalty(player, partHP);

                // Also restore a small amount of health
                double maxPlayerHP = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                double currentHealth = player.getHealth();
                player.setHealth(Math.min(maxPlayerHP, currentHealth + 1.0)); // Restore 0.5 hearts

                // If in part HP display mode, update the display immediately
                if (playerInventoryListener.isViewingPartHP(player)) {
                    partHP.updateArmorDisplay(player, true, -1);
                } else {
                    partHP.updateArmorDisplay(player, false, -1);
                }

                player.sendMessage(Lang.get("surgery-complete"));
                player.playSound(player.getLocation(), config.surgeryCompleteSound, config.surgeryCompleteSoundVolume, config.surgeryCompleteSoundPitch);

                healingSlots.remove(player.getUniqueId());
                healingTasks.remove(player.getUniqueId());
                stopHealingSound(player);
            }
        }.runTaskLater(plugin, config.surgeryDurationTicks);

        healingTasks.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // If inventory is closed during healing, interrupt it
        BukkitTask task = healingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.sendMessage(Lang.get("healing-interrupted"));
            // Returning the item is not part of the concept, so it's not done
            stopHealingSound(player); // Stop the healing sound

            // Remove the glowing effect
            if (healingSlots.remove(player.getUniqueId()) != null) {
                PartHP partHP = armorStatsManager.getOrCreatePartHP(player);
                partHP.updateArmorDisplay(player, false, -1);
            }
        }
    }

    private void stopHealingSound(Player player) {
        BukkitTask soundTask = healingSoundTasks.remove(player.getUniqueId());
        if (soundTask != null) {
            soundTask.cancel();
        }
    }
}