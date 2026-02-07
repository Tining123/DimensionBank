package org.tining.dimensionbank;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public final class ItemLangManager {

    private final DimensionBank plugin;

    private File file;
    private YamlConfiguration cfg;

    // 缓存：Material -> display
    private final Map<Material, String> cache = new EnumMap<Material, String>(Material.class);

    public ItemLangManager(DimensionBank plugin) {
        this.plugin = plugin;
    }

    public void load() {

        // 永远先给 cfg 一个对象，保证 tr() 不会 NPE
        cfg = new YamlConfiguration();
        if (cfg.getConfigurationSection("items") == null) cfg.createSection("items");

        // 统一用 Bukkit config
        FileConfiguration pcfg = plugin.getConfig();
        boolean enable = true;
        try {
            enable = pcfg.getBoolean("item-lang.enable", true);
        } catch (Throwable ignored) {}

        cache.clear();

        if (!enable) {
            this.file = null;
            plugin.log(plugin.getMenuLang().tr("core.itemlang_disabled", "item-lang 已关闭"));
            return;
        }

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        String name = pcfg.getString("item-lang.file", "item-lang.yml");
        this.file = new File(dataFolder, name);

        // 不自动生成也行，但建议生成一个空模板，方便服主编辑
        if (!file.exists()) {
            try {
                cfg.save(file);
                plugin.log(plugin.getMenuLang().tr(
                        "core.itemlang_generated",
                        "已生成默认 item-lang.yml: %path%",
                        "%path%", file.getAbsolutePath()
                ));
            } catch (IOException e) {
                plugin.warn(plugin.getMenuLang().tr(
                        "core.itemlang_save_failed",
                        "无法保存默认 item-lang.yml: %error%",
                        "%error%", String.valueOf(e.getMessage())
                ));
            }
        }

        cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getConfigurationSection("items") == null) cfg.createSection("items");

        plugin.log(plugin.getMenuLang().tr("core.itemlang_loaded", "item-lang.yml 已加载"));
    }

    public String tr(Material m) {
        if (m == null || m == Material.AIR) return "";

        // cfg 永远不该为 null，但仍然兜底
        if (cfg == null) return prettyEnglish(m.name());

        String hit = cache.get(m);
        if (hit != null) return hit;

        String key = "items." + m.name();
        String val = cfg.getString(key, null);

        if (val != null) {
            val = val.trim();
            if (val.length() > 0) {
                cache.put(m, val);
                return val;
            }
        }

        String fallback = prettyEnglish(m.name());
        cache.put(m, fallback);
        return fallback;
    }


    public void reload() {
        load();
    }

    // 你已有的 prettyEnglish 可以挪过来复用
    public static String prettyEnglish(String enumName) {
        // IRON_INGOT -> Iron Ingot
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.length() == 0) continue;
            if (i > 0) sb.append(' ');
            sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
