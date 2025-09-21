package com.braur0.PartsVitality.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;

import com.braur0.PartsVitality.PartsVitality;
import com.braur0.PartsVitality.listener.ArmorDamageListener;
import com.braur0.PartsVitality.manager.ArmorStatsManager;
import com.braur0.PartsVitality.model.PartHP;

import org.bukkit.attribute.Attribute;

public class PlayerSetupListener implements Listener {
    private final PartsVitality plugin;
    private final ArmorStatsManager armorStatsManager;
    private final ArmorDamageListener armorDamageListener;
    
    public PlayerSetupListener(PartsVitality plugin, ArmorStatsManager armorStatsManager, ArmorDamageListener armorDamageListener) {
        this.plugin = plugin;
        this.armorStatsManager = armorStatsManager;
        this.armorDamageListener = armorDamageListener;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        // This runs only when a player "logs in" to the server.
        // This avoids conflicts with onJoin during respawn.
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;

        final Player player = event.getPlayer();
        // Get existing part HP data, or create new if it doesn't exist
        PartHP partHP = armorStatsManager.getOrCreatePartHP(player);

        // Re-apply debuffs and HP penalty after 1 tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Clear existing debuffs before recalculating
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            armorDamageListener.updateDebuffs(player, partHP);
            armorDamageListener.updateHealthPenalty(player, partHP);
        }, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Handled in onLogin, so nothing to do in onJoin
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // If the player is in part HP display mode when they quit,
        // revert the display to ensure armor data is saved correctly.
        if (plugin.getPlayerInventoryListener().isViewingPartHP(player)) {
            armorStatsManager.getOrCreatePartHP(player).updateArmorDisplay(player, false, -1);
        }
        // Reset the inventory display state
        plugin.getPlayerInventoryListener().resetViewingState(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Delay by 1 tick to ensure some processes apply correctly
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Reset player's max health to the default value (20)
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            // Restore health to full
            player.setHealth(20.0);
            // Re-initialize part HP data
            armorStatsManager.initializePlayer(player);
            // Clear existing debuffs
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        }, 1L);
    }
}