package org.tining.dimensionbank;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class BankManager {

    private final DimensionBank plugin;

    // 只做“文件系统安全字符替换”，不改变大小写
    private static final Pattern UNSAFE = Pattern.compile("[\\\\/:*?\"<>|]");

    public BankManager(DimensionBank plugin) {
        this.plugin = plugin;
    }

    /* ================= 文件定位 ================= */

    /**
     * 玩家文件：按玩家名（大小写敏感）生成
     */
    public File getPlayerFile(String playerName) {
        String safeName = toSafeFileName(playerName);
        return new File(plugin.getBankFolder(), safeName + ".yml");
    }

    private String toSafeFileName(String s) {
        if (s == null) return "unknown";
        // 仅替换 Windows/Linux 不安全字符，不做大小写归一
        String t = UNSAFE.matcher(s).replaceAll("_");
        // 防止空文件名
        if (t.trim().isEmpty()) return "unknown";
        return t;
    }

    /* ================= 读写 ================= */

    public YamlConfiguration load(String playerName) {
        File f = getPlayerFile(playerName);
        if (!f.exists()) {
            // 新文件默认结构
            YamlConfiguration y = new YamlConfiguration();
            y.set("player", playerName);
            y.set("items", new HashMap<String, Object>());
            saveAtomic(f, y);
            return y;
        }
        return YamlConfiguration.loadConfiguration(f);
    }

    public void save(String playerName, YamlConfiguration yml) {
        File f = getPlayerFile(playerName);
        saveAtomic(f, yml);
    }

    /**
     * 原子保存：写 tmp 再 rename，最大限度防半写文件
     */
    private void saveAtomic(File target, YamlConfiguration yml) {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        File tmp = new File(parent, target.getName() + ".tmp");

        try {
            yml.save(tmp);

            // Windows 下 renameTo 覆盖不稳定：先删旧再换
            if (target.exists() && !target.delete()) {
                plugin.warn(plugin.getMenuLang().tr(
                        "bank.delete_failed",
                        "无法删除旧文件: %path%",
                        "%path%", target.getAbsolutePath()
                ));
                // 失败了就不要覆盖（避免丢档）
                return;
            }


            if (!tmp.renameTo(target)) {
                plugin.warn(plugin.getMenuLang().tr(
                        "bank.rename_failed",
                        "原子保存失败（renameTo=false）: %path%",
                        "%path%", target.getAbsolutePath()
                ));
            }

        } catch (IOException e) {
            plugin.warn(plugin.getMenuLang().tr(
                    "bank.save_failed",
                    "保存银行文件失败: %error%",
                    "%error%", String.valueOf(e.getMessage())
            ));
        } finally {
            if (tmp.exists()) tmp.delete();
        }
    }

    /* ================= 核心接口 ================= */

    /**
     * 读取某物品数量
     */
    public long getAmount(String playerName, Material material) {
        if (material == null) return 0L;
        YamlConfiguration y = load(playerName);
        return y.getLong("items." + material.name(), 0L);
    }

    /**
     * 增加数量（可为负，但不会低于 0）
     * 返回：实际增加后的新数量
     */
    public long addAmount(String playerName, Material material, long delta) {
        if (material == null) return 0L;

        YamlConfiguration y = load(playerName);

        String key = "items." + material.name();
        long old = y.getLong(key, 0L);
        long now = old + delta;
        if (now < 0L) now = 0L;

        y.set(key, now);
        save(playerName, y);

        return now;
    }

    /**
     * 尝试扣除数量，返回实际扣除的数量
     */
    public long takeAmount(String playerName, Material material, long want) {
        if (material == null) return 0L;
        if (want <= 0) return 0L;

        YamlConfiguration y = load(playerName);

        String key = "items." + material.name();
        long old = y.getLong(key, 0L);
        long take = Math.min(old, want);

        long now = old - take;
        if (now < 0L) now = 0L;

        y.set(key, now);
        save(playerName, y);

        return take;
    }

    /**
     * 返回玩家所有非零库存（Material.name -> amount）
     */
    public Map<String, Long> getNonZeroItems(String playerName) {
        YamlConfiguration y = load(playerName);

        Map<String, Long> out = new HashMap<String, Long>();

        if (y.getConfigurationSection("items") == null) return out;

        for (String k : y.getConfigurationSection("items").getKeys(false)) {
            long v = y.getLong("items." + k, 0L);
            if (v > 0L) out.put(k, v);
        }

        return out;
    }
}
