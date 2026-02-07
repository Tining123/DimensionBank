package org.tining.dimensionbank;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class LangManager {

    private final DimensionBank plugin;

    private File langFile;
    private YamlConfiguration lang;

    public LangManager(DimensionBank plugin) {
        this.plugin = plugin;
    }

    /**
     * 确保 lang.yml 存在，并加载
     */
    public void load() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        langFile = new File(dataFolder, "lang.yml");

        if (!langFile.exists()) {
            // 生成一个“半字典示例”，你后续随时补
            lang = new YamlConfiguration();
            writeDefaults(lang);
            try {
                lang.save(langFile);

                plugin.log(plugin.getMenuLang().tr(
                        "core.lang_generated",
                        "已生成默认 lang.yml: %path%",
                        "%path%", langFile.getAbsolutePath()
                ));

            } catch (IOException e) {

                plugin.warn(plugin.getMenuLang().tr(
                        "core.lang_save_failed",
                        "无法保存默认 lang.yml: %error%",
                        "%error%", String.valueOf(e.getMessage())
                ));
            }
        }

        lang = YamlConfiguration.loadConfiguration(langFile);

        plugin.log(plugin.getMenuLang().tr(
                "core.lang_loaded",
                "lang.yml 已加载"
        ));
    }


    /**
     * 取 Material 的显示名：
     * 1) 优先查 lang.yml 的键（Material.name）
     * 2) 没有则回退为友好英文（DIAMOND_SWORD -> Diamond Sword）
     */
    public String tr(Material material) {
        if (material == null) return "UNKNOWN";

        String key = material.name();

        try {
            String v = lang.getString("items." + key);
            if (v != null && v.trim().length() > 0) {
                return v.trim();
            }
        } catch (Throwable ignored) {}

        return prettyEnglish(key);
    }

    /**
     * 友好英文回退：DIAMOND_SWORD -> Diamond Sword
     */
    public String prettyEnglish(String enumName) {
        if (enumName == null) return "Unknown";

        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.length() == 0) continue;

            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }

        return sb.length() == 0 ? "Unknown" : sb.toString();
    }

    /**
     * 写入半字典示例（你最常用的一批）
     */
    private void writeDefaults(YamlConfiguration y) {
        y.set("meta.version", 1);
        y.set("meta.note", "只需要写你想中文化的条目；没有的会自动显示友好英文。");
        y.set("items.DIAMOND", "钻石");
        y.set("items.IRON_INGOT", "铁锭");
        y.set("items.GOLD_INGOT", "金锭");
        y.set("items.COAL", "煤炭");
        y.set("items.REDSTONE", "红石");
        y.set("items.LAPIS_LAZULI", "青金石");
        y.set("items.EMERALD", "绿宝石");
        y.set("items.STONE", "石头");
        y.set("items.COBBLESTONE", "圆石");
        y.set("items.LOG", "原木");
        y.set("items.LOG_2", "原木(2)");
        y.set("items.PLANKS", "木板");
        y.set("items.TORCH", "火把");
        y.set("items.BREAD", "面包");
        y.set("items.COOKED_BEEF", "熟牛肉");
        y.set("items.ARROW", "箭");
        y.set("items.BOW", "弓");
        y.set("items.ENDER_PEARL", "末影珍珠");
        y.set("items.BLAZE_ROD", "烈焰棒");
        y.set("items.NETHER_STAR", "下界之星");
    }
}
