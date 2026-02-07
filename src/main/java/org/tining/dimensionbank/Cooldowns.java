package org.tining.dimensionbank;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public final class Cooldowns {

    private final DimensionBank plugin;
    private final Map<String, Long> lastMs = new HashMap<String, Long>();

    public Cooldowns(DimensionBank plugin) {
        this.plugin = plugin;
    }

    /**
     * 返回 true = 允许操作；false = 仍在冷却
     */
    public boolean pass(String playerName) {

        FileConfiguration cfg = plugin.getPluginConfig();

        int cdMs = cfg.getInt("cooldown-ms", -1);

        // 兼容旧配置：cooldown(秒)
        if (cdMs < 0) {
            int cdSec = Math.max(0, cfg.getInt("cooldown", 3));
            cdMs = cdSec * 1000;
        }

        if (cdMs <= 0) return true;

        long now = System.currentTimeMillis();
        Long last = lastMs.get(playerName);

        if (last != null && now - last < cdMs) {
            return false;
        }

        lastMs.put(playerName, now);
        return true;
    }

    public int getCooldownMs() {
        int cdMs = plugin.getPluginConfig().getInt("cooldown-ms", -1);
        if (cdMs >= 0) return cdMs;

        int cdSec = Math.max(0, plugin.getPluginConfig().getInt("cooldown", 3));
        return cdSec * 1000;
    }
}
