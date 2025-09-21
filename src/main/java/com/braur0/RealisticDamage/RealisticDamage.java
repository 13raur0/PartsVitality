package com.braur0.RealisticDamage;

import com.braur0.RealisticDamage.config.Lang;
import com.braur0.RealisticDamage.config.PluginConfig;
import com.braur0.RealisticDamage.listener.ArmorDamageListener;
import com.braur0.RealisticDamage.listener.PlayerSetupListener;
import com.braur0.RealisticDamage.listener.PlayerInventoryListener;
import com.braur0.RealisticDamage.listener.PlayerStatusListener;
import com.braur0.RealisticDamage.listener.PlayerHealingListener;
import com.braur0.RealisticDamage.manager.ArmorStatsManager;
import com.braur0.RealisticDamage.model.PartHP;
import org.bukkit.plugin.java.JavaPlugin;

public class RealisticDamage extends JavaPlugin {

    private ArmorStatsManager armorStatsManager;
    private ArmorDamageListener armorDamageListener;
    private PlayerInventoryListener playerInventoryListener;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        // Generate and load the configuration file
        saveDefaultConfig();

        // Pass the configuration to the PartHP model
        PartHP.loadConfig(getConfig());

        // Load the language file
        Lang.load(this);

        // Initialize the centralized configuration class
        this.pluginConfig = new PluginConfig(getConfig());

        // Initialize managers and listeners
        this.armorStatsManager = new ArmorStatsManager(this);
        this.armorDamageListener = new ArmorDamageListener(this, armorStatsManager, pluginConfig);
        this.playerInventoryListener = new PlayerInventoryListener(this, armorStatsManager);
        PlayerSetupListener playerSetupListener = new PlayerSetupListener(this, armorStatsManager, armorDamageListener);
        PlayerStatusListener playerStatusListener = new PlayerStatusListener(this);
        PlayerHealingListener playerHealingListener = new PlayerHealingListener(this, armorStatsManager, armorDamageListener, playerInventoryListener, pluginConfig);

        // Register listeners with the server
        getServer().getPluginManager().registerEvents(armorDamageListener, this);
        getServer().getPluginManager().registerEvents(playerSetupListener, this);
        getServer().getPluginManager().registerEvents(playerInventoryListener, this);
        getServer().getPluginManager().registerEvents(playerStatusListener, this);
        getServer().getPluginManager().registerEvents(playerHealingListener, this);

        getLogger().info(Lang.get("plugin-enabled"));
    }

    @Override
    public void onDisable() {
        getLogger().info(Lang.get("plugin-disabled"));
    }

    public PlayerInventoryListener getPlayerInventoryListener() {
        return playerInventoryListener;
    }
}
