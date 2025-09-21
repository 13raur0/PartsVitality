package com.braur0.PartsVitality.listener;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.braur0.PartsVitality.PartsVitality;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatusListener implements Listener {

    private static final int INTERVAL_CRITICAL = 10; // 0.5 seconds
    private static final int INTERVAL_LOW = 30;      // 1.5 seconds
    private static final int INTERVAL_MID = 50;      // 2.5 seconds
    private static final int INTERVAL_HIGH = 70;     // 3.5 seconds


    private final PartsVitality plugin;
    private final Map<UUID, BukkitTask> soundTasks = new ConcurrentHashMap<>();

    public PlayerStatusListener(PartsVitality plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Check health after damage, delayed by 1 tick
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkHealthAndPlaySound(player), 1L);
        }
    }

    @EventHandler
    public void onHeal(EntityRegainHealthEvent event) {
        // Disable natural regeneration from saturation
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof Player player) {
            // Check health after healing, delayed by 1 tick
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkHealthAndPlaySound(player), 1L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Check health on join
        checkHealthAndPlaySound(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Cancel task on quit
        stopSoundTask(event.getPlayer());
    }

    private void checkHealthAndPlaySound(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) {
            stopSoundTask(player);
            return;
        }

        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        double healthPercentage = currentHealth / maxHealth;

        // Stop the task if health is above 75%
        if (healthPercentage > 0.75) {
            stopSoundTask(player);
            return;
        }

        // If a task is already running, do nothing (interval is handled within the task)
        if (soundTasks.containsKey(player.getUniqueId())) {
            return;
        }

        // Start a new task
        BukkitTask task = new BukkitRunnable() {
            private int counter = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || player.getHealth() / player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() > 0.75) {
                    stopSoundTask(player); // Remove self from the map
                    this.cancel();
                    return;
                }

                double currentPercentage = player.getHealth() / player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                // Determine the sound interval in ticks based on health percentage (1 second = 20 ticks)
                int intervalTicks = switch ((int) (currentPercentage * 100)) {
                    case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 -> INTERVAL_CRITICAL;
                    case 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45 -> INTERVAL_LOW;
                    case 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60 -> INTERVAL_MID;
                    case 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75 -> INTERVAL_HIGH;
                    default -> -1; // Do not play
                };

                // The task runs every 10 ticks, so play the sound when the counter reaches (intervalTicks / 10)
                if (intervalTicks > 0 && counter >= (intervalTicks / 10)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 1.0f);
                    counter = 0; // Reset the counter
                } else {
                    counter++;
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Check the counter every 0.5 seconds (10 ticks)

        soundTasks.put(player.getUniqueId(), task);
    }

    private void stopSoundTask(Player player) {
        if (player == null) return;
        BukkitTask existingTask = soundTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
    }
}
