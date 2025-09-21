package com.braur0.PartsVitality.manager;

import com.braur0.PartsVitality.PartsVitality;
import com.braur0.PartsVitality.model.PartHP;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorStatsManager {

    private final PartsVitality plugin;
    private final Map<UUID, PartHP> playerPartHP = new ConcurrentHashMap<>();

    public ArmorStatsManager(PartsVitality plugin) {
        this.plugin = plugin;
    }

    public void initializePlayer(Player player) {
        playerPartHP.put(player.getUniqueId(), new PartHP());
        plugin.getLogger().info("Initialized part HP data for " + player.getName());
    }

    public void removePlayer(Player player) {
        playerPartHP.remove(player.getUniqueId());
        plugin.getLogger().info("Removed part HP data for " + player.getName());
    }

    public PartHP getPartHP(Player player) {
        return playerPartHP.get(player.getUniqueId());
    }

    public PartHP getOrCreatePartHP(Player player) {
        return playerPartHP.computeIfAbsent(player.getUniqueId(), k -> new PartHP());
    }
}
