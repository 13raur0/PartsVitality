package com.braur0.PartsVitality.config;

import com.braur0.PartsVitality.PartsVitality;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Lang {

    private static final Map<String, String> messages = new HashMap<>();

    public static void load(PartsVitality plugin) {
        String lang = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "messages_" + lang + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("messages_" + lang + ".yml", false);
        }

        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Fallback to internal file if external file is incomplete
        InputStream defaultStream = plugin.getResource("messages_" + lang + ".yml");
        if (defaultStream != null) {
            langConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
        }

        messages.clear();
        for (String key : langConfig.getKeys(true)) {
            if (!langConfig.isConfigurationSection(key)) {
                messages.put(key, langConfig.getString(key));
            }
        }
    }

    public static String get(String key) {
        return messages.getOrDefault(key, "§c[Missing message: " + key + "]");
    }

    public static String get(String key, Map<String, String> placeholders) {
        String message = get(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}