package com.braur0.RealisticDamage.listener;

import com.braur0.RealisticDamage.config.PluginConfig;
import org.bukkit.util.BoundingBox;
import com.braur0.RealisticDamage.RealisticDamage;
import com.braur0.RealisticDamage.manager.ArmorStatsManager;
import com.braur0.RealisticDamage.model.PartHP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArmorDamageListener implements Listener {

    private final RealisticDamage plugin;
    private final ArmorStatsManager armorStatsManager;

    private final PluginConfig config;

    public ArmorDamageListener(RealisticDamage plugin, ArmorStatsManager armorStatsManager, PluginConfig config) {
        this.plugin = plugin;
        this.armorStatsManager = armorStatsManager;
        this.config = config;
    }

    /**
     * Handles player damage events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PartHP partHP = armorStatsManager.getOrCreatePartHP(player);
        double heartDamage = event.getFinalDamage();
        double partDamage = heartDamage * config.damageMultiplier;

        List<String> targetParts = new ArrayList<>();
        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean shouldDamageArmor = isArmorDamage(cause);

        if (isArmorDamage(cause)) {
            // Can only get attacker's position for EntityDamageByEntityEvent
            if (event instanceof EntityDamageByEntityEvent edbe) {
                Entity damager = edbe.getDamager();
                Vector hitPoint = getHitPoint(player, damager);

                if (hitPoint != null) {
                    BoundingBox playerBox = player.getBoundingBox();
                    double boxMinY = playerBox.getMinY();
                    double boxHeight = playerBox.getHeight();
                    double hitY = hitPoint.getY();

                    // Determine based on the relative height from the bottom of the hitbox
                    if (hitY >= boxMinY + boxHeight * 0.85) { // Top 15%
                    if (hitY >= boxMinY + boxHeight * 0.85) { // Top 15% (Head)
                        targetParts.add(PartHP.HEAD);
                    } else if (hitY >= boxMinY + boxHeight * 0.4) { // Middle area
                    } else if (hitY >= boxMinY + boxHeight * 0.4) { // Middle area (Chest)
                        targetParts.add(PartHP.CHEST);
                    } else if (hitY >= boxMinY + boxHeight * 0.1) { // Knee area
                    } else if (hitY >= boxMinY + boxHeight * 0.1) { // Knee area (Legs)
                        targetParts.add(PartHP.LEGS);
                    } else { // Feet area
                        targetParts.add(PartHP.FEET);
                    }
                } else {
                    // Fallback if Ray Tracing fails to find a hit location
                    targetParts.addAll(PartHP.ALL_PARTS);
                }
            } else {
                // If attacker's position cannot be determined (e.g., explosion), distribute damage to all parts
                targetParts.addAll(PartHP.ALL_PARTS);
            }

        } else {
            // Distribute environmental damage
            switch (cause) {
            case FALL:
                // Fall damage goes to legs and feet
                targetParts.add(PartHP.LEGS);
                targetParts.add(PartHP.FEET);
                break;
            case VOID:
                // The void is instant death
                player.setHealth(0);
                return;
            default:
                // Others (poison, magic, suffocation, etc.) go to all parts with remaining HP
                targetParts.addAll(PartHP.ALL_PARTS);
                break;
            }
        }

        // If no target parts were determined (e.g., Ray Tracing failed, no target for environmental damage),
        // apply damage to all parts as a fallback.
        if (targetParts.isEmpty()) {
            targetParts.addAll(PartHP.ALL_PARTS);
        }

        applyDamageToParts(player, partHP, partDamage, targetParts, shouldDamageArmor);
    }

    /**
     * Uses Ray Tracing to calculate where an attack hit the player's hitbox.
     * @param target The player who was hit.
     * @param damager The attacker or projectile.
     * @return The Vector of the hit location, or null if it cannot be determined.
     */
    private Vector getHitPoint(Player target, Entity damager) {
        Vector start;
        Vector direction;

        if (damager instanceof Projectile projectile) {
            start = projectile.getLocation().toVector();
            direction = projectile.getVelocity().normalize();
        } else if (damager instanceof org.bukkit.entity.LivingEntity livingDamager) {
            start = livingDamager.getEyeLocation().toVector();
            direction = livingDamager.getEyeLocation().getDirection();
        } else {
            return null; // Cannot calculate if the attacker's viewpoint is unknown
        }

        BoundingBox targetBox = target.getBoundingBox();
        double maxDistance = start.distance(target.getLocation().toVector()) + 2.0; // A generous max distance

        // Advance the ray step by step and check for intersection with the hitbox
        for (double d = 0; d < maxDistance; d += config.rayTraceStep) {
            Vector currentPoint = start.clone().add(direction.clone().multiply(d));
            if (targetBox.contains(currentPoint)) {
                return currentPoint; // Return the first intersection point
            }
        }

        return null; // No hit
    }

    /**
     * Applies the actual damage to part HP and updates debuffs.
     * @param player The target player.
     * @param partHP The PartHP object.
     * @param totalDamage The total damage amount.
     * @param primaryTargets The list of parts that initially receive damage.
     * @param shouldDamageArmor Whether the armor's durability should be reduced.
     */
    private void applyDamageToParts(Player player, PartHP partHP, double totalDamage, List<String> primaryTargets, boolean shouldDamageArmor) {
        List<String> availablePrimary = primaryTargets.stream()
                .filter(part -> partHP.getPartHP(part) > 0)
                .toList();

        if (!availablePrimary.isEmpty()) {
            // Distribute damage evenly among the target parts
            double damagePerPart = totalDamage / availablePrimary.size();
            for (String part : availablePrimary) {
                double currentHP = partHP.getPartHP(part);
                partHP.setPartHP(part, currentHP - damagePerPart);
                if (config.useCustomDurabilityDamage && shouldDamageArmor) {
                    damageArmor(player, part, damagePerPart); // Reduce armor durability
                }
            }
        } else {
            // If the target part is already at 0 HP, distribute damage to other remaining parts
            List<String> remainingParts = partHP.getRemainingParts();
            if (!remainingParts.isEmpty()) {
                double damagePerPart = totalDamage / remainingParts.size();
                for (String part : remainingParts) {
                    double currentHP = partHP.getPartHP(part);
                    partHP.setPartHP(part, currentHP - damagePerPart);
                    if (config.useCustomDurabilityDamage && shouldDamageArmor) {
                        damageArmor(player, part, damagePerPart); // Reduce armor durability
                    }
                }
            }
        }

        // Update debuffs after applying damage
        updateDebuffs(player, partHP);
        // Update max HP penalty
        updateHealthPenalty(player, partHP);
    }

    /**
     * Reduces the durability of the corresponding armor piece based on part damage.
     * @param player The target player.
     * @param partName The name of the part.
     * @param partDamage The damage received by the part.
     */
    private void damageArmor(Player player, String partName, double partDamage) {
        EquipmentSlot slot = getEquipmentSlotFromPart(partName);
        if (slot == null) return;

        ItemStack armorPiece = player.getInventory().getItem(slot);
        if (armorPiece == null || armorPiece.getType().isAir() || !(armorPiece.getItemMeta() instanceof Damageable damageable)) {
            return;
        }

        // Calculate durability damage based on the configured value
        int durabilityDamage = (int) (partDamage / config.damagePerDurabilityPoint);
        if (durabilityDamage <= 0) return;

        // Consider the Unbreaking enchantment
        int unbreakingLevel = armorPiece.getEnchantmentLevel(Enchantment.DURABILITY);
        // Unbreaking enchantment calculation modifier (level + 1)
        final double UNBREAKING_CHANCE_MODIFIER = unbreakingLevel + 1.0;
        for (int i = 0; i < durabilityDamage; i++) {
            // Has a 1 / (unbreakingLevel + 1) chance to reduce durability
            if (Math.random() * UNBREAKING_CHANCE_MODIFIER < 1.0) {
                int newDamage = damageable.getDamage() + 1;
                if (newDamage >= armorPiece.getType().getMaxDurability()) {
                    // Armor breaks
                    player.getInventory().setItem(slot, null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    return; // Exit the loop
                }
                damageable.setDamage(newDamage);
            }
        }

        armorPiece.setItemMeta(damageable);
    }

    /**
     * Gets the corresponding EquipmentSlot from a part name.
     * @param partName The name of the part.
     * @return The EquipmentSlot.
     */
    private EquipmentSlot getEquipmentSlotFromPart(String partName) {
        return switch (partName.toLowerCase()) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }
    /**
     * Updates debuffs based on part HP.
     */
    public void updateDebuffs(Player player, PartHP partHP) {
        ConfigurationSection debuffsConfig = plugin.getConfig().getConfigurationSection("debuffs");
        if (debuffsConfig == null) return;

        // Process debuffs for head and chest
        List<String> individualParts = List.of(PartHP.HEAD, PartHP.CHEST);
        for (String part : individualParts) {
            double maxHP = PartHP.getMaxHPPerPart(part);
            if (maxHP <= 0) continue;
            double currentHP = partHP.getPartHP(part);
            double ratio = currentHP / maxHP;

            // Determine the final debuff to apply
            List<Map<?, ?>> partDebuffs = debuffsConfig.getMapList(part);
            PotionEffect finalEffect = null;
            for (Map<?, ?> debuffInfo : partDebuffs) {
                if (ratio <= (Double) debuffInfo.get("threshold")) {
                    PotionEffectType type = PotionEffectType.getByName((String) debuffInfo.get("effect"));
                    if (type == null) continue;
                    int level = (Integer) debuffInfo.get("level");
                    finalEffect = new PotionEffect(type, -1, level, true, false);
                    // Break the loop once the most severe debuff is found (assuming thresholds in config are in descending order)
                    break; 
                }
            }

            // Clear existing debuffs
            for (Map<?, ?> debuffInfo : partDebuffs) {
                PotionEffectType type = PotionEffectType.getByName((String) debuffInfo.get("effect"));
                if (type != null && (finalEffect == null || !finalEffect.getType().equals(type))) {
                    player.removePotionEffect(type);
                }
            }

            // Apply the new debuff
            if (finalEffect != null) {
                player.addPotionEffect(finalEffect);
            }
        }

        // Process combined debuff for legs and feet
        double legsCurrentHP = partHP.getPartHP(PartHP.LEGS);
        double feetCurrentHP = partHP.getPartHP(PartHP.FEET);
        double totalMaxHP = PartHP.getMaxHPPerPart(PartHP.LEGS) + PartHP.getMaxHPPerPart(PartHP.FEET);
        if (totalMaxHP <= 0) return;

        double totalCurrentHP = legsCurrentHP + feetCurrentHP;
        double combinedRatio = totalCurrentHP / totalMaxHP;

        // Determine the final combined debuff to apply
        List<Map<?, ?>> combinedDebuffs = debuffsConfig.getMapList("legs_and_feet");
        PotionEffect finalCombinedEffect = null;
        for (Map<?, ?> debuffInfo : combinedDebuffs) {
            if (combinedRatio <= (Double) debuffInfo.get("threshold")) {
                PotionEffectType type = PotionEffectType.getByName((String) debuffInfo.get("effect"));
                if (type == null) continue;
                int level = (Integer) debuffInfo.get("level");
                finalCombinedEffect = new PotionEffect(type, -1, level, true, false);
                break;
            }
        }

        // Clear existing combined debuffs
        for (Map<?, ?> debuffInfo : combinedDebuffs) {
            PotionEffectType type = PotionEffectType.getByName((String) debuffInfo.get("effect"));
            if (type != null && (finalCombinedEffect == null || !finalCombinedEffect.getType().equals(type))) {
                player.removePotionEffect(type);
            }
        }

        // Apply the new combined debuff
        if (finalCombinedEffect != null) {
            player.addPotionEffect(finalCombinedEffect);
        }
    }

    /**
     * Updates the player's max health based on the number of parts with 0 HP.
     * @param player The target player.
     * @param partHP The PartHP object.
     */
    public void updateHealthPenalty(Player player, PartHP partHP) {
        long brokenPartsCount = PartHP.ALL_PARTS.stream()
                .filter(part -> partHP.getPartHP(part) <= 0)
                .count();

        double penaltyPerPart = config.healthPenaltyPerBrokenPart;
        double totalPenalty = brokenPartsCount * penaltyPerPart;

        // Always calculate based on the vanilla default max health (20.0)
        // to prevent duplicate penalty application.
        double baseMaxHealth = 20.0;
        // Calculate the new max health, ensuring it's at least 2.0 (1 heart).
        double newMaxHealth = Math.max(2.0, baseMaxHealth - totalPenalty);

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);

        // Adjust current health if it exceeds the new max health
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }
    }

    private boolean isArmorDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
               cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
               cause == EntityDamageEvent.DamageCause.PROJECTILE ||
               cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
               cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
    }
}
