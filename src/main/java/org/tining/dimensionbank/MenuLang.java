package org.tining.dimensionbank;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MenuLang {

    private final DimensionBank plugin;
    private YamlConfiguration cfg;

    // 简单缓存（防止频繁 IO）
    private final Map<String, String> cache = new HashMap<String, String>();

    public MenuLang(DimensionBank plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {

        File file = new File(plugin.getDataFolder(), "menu-lang.yml");

        if (!file.exists()) {
            plugin.saveResource("menu-lang.yml", false);
        }

        cfg = YamlConfiguration.loadConfiguration(file);
        cache.clear();
    }

    /**
     * 翻译：找不到就回退 defaultText
     */
    public String tr(String key, String defaultText) {

        // 先查缓存
        String v = cache.get(key);
        if (v != null) return color(v);

        String val = cfg.getString(key);

        if (val == null || val.trim().isEmpty()) {
            cache.put(key, defaultText);
            return defaultText;
        }

        cache.put(key, val);
        return color(val);
    }

    private String color(String s) {
        return s.replace("&", "§");
    }

    public String tr(String key, String defaultText, String... kv) {
        String s = tr(key, defaultText);
        if (kv == null) return s;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = kv[i];
            String v = kv[i + 1];
            if (k != null) s = s.replace(k, v == null ? "" : v);
        }
        return s;
    }

}
