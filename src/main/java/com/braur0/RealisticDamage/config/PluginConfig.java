package com.braur0.RealisticDamage.config;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    // Damage
    public final double damageMultiplier;
    public final double rayTraceStep;

    // Durability
    public final boolean useCustomDurabilityDamage;
    public final double damagePerDurabilityPoint;

    // Healing
    public final int healingDurationTicks;
    public final Sound healingSound;
    public final float healingSoundVolume;
    public final float healingSoundPitch;
    public final long healingSoundInterval;
    public final Sound healingCompleteSound;
    public final float healingCompleteSoundVolume;
    public final float healingCompleteSoundPitch;

    // Surgery
    public final int surgeryDurationTicks;
    public final Sound surgerySound;
    public final float surgerySoundVolume;
    public final float surgerySoundPitch;
    public final long surgerySoundInterval;
    public final Sound surgeryCompleteSound;
    public final float surgeryCompleteSoundVolume;
    public final float surgeryCompleteSoundPitch;
    public final double surgeryRestoredHp;

    // Health Penalty
    public final double healthPenaltyPerBrokenPart;

    public PluginConfig(FileConfiguration config) {
        // Damage
        this.damageMultiplier = config.getDouble("damage.damage-multiplier", 5.0);
        this.rayTraceStep = config.getDouble("damage.ray-trace-step", 0.1);

        // Durability
        this.useCustomDurabilityDamage = config.getBoolean("durability.use-custom-durability-damage", true);
        this.damagePerDurabilityPoint = config.getDouble("durability.damage-per-durability-point", 0.4);

        // Healing
        this.healingDurationTicks = config.getInt("healing.duration-seconds", 3) * 20;
        this.healingSound = Sound.valueOf(config.getString("healing.sound.name", "BLOCK_WOOL_PLACE").toUpperCase());
        this.healingSoundVolume = (float) config.getDouble("healing.sound.volume", 1.0);
        this.healingSoundPitch = (float) config.getDouble("healing.sound.pitch", 1.5);
        this.healingSoundInterval = config.getLong("healing.sound.interval-ticks", 10);
        this.healingCompleteSound = Sound.valueOf(config.getString("healing.complete-sound.name", "ENTITY_PLAYER_LEVELUP").toUpperCase());
        this.healingCompleteSoundVolume = (float) config.getDouble("healing.complete-sound.volume", 0.7);
        this.healingCompleteSoundPitch = (float) config.getDouble("healing.complete-sound.pitch", 1.5);

        // Surgery
        this.surgeryDurationTicks = config.getInt("surgery.duration-seconds", 10) * 20;
        this.surgerySound = Sound.valueOf(config.getString("surgery.sound.name", "BLOCK_ANVIL_USE").toUpperCase());
        this.surgerySoundVolume = (float) config.getDouble("surgery.sound.volume", 0.5);
        this.surgerySoundPitch = (float) config.getDouble("surgery.sound.pitch", 1.8);
        this.surgerySoundInterval = config.getLong("surgery.sound.interval-ticks", 20);
        this.surgeryCompleteSound = Sound.valueOf(config.getString("surgery.complete-sound.name", "BLOCK_BEACON_ACTIVATE").toUpperCase());
        this.surgeryCompleteSoundVolume = (float) config.getDouble("surgery.complete-sound.volume", 0.8);
        this.surgeryCompleteSoundPitch = (float) config.getDouble("surgery.complete-sound.pitch", 1.5);
        this.surgeryRestoredHp = config.getDouble("surgery.restored-hp", 1.0);

        // Health Penalty
        this.healthPenaltyPerBrokenPart = config.getDouble("health-penalty-per-broken-part", 5.0);
    }
}
