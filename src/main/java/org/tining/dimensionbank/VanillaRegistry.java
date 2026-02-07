package org.tining.dimensionbank;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class VanillaRegistry {

    private final List<Material> vanillaMaterials = new ArrayList<Material>();

    /**
     * 初始化：枚举 Material 并过滤出可用项（不依赖 id）
     */
    public void init() {
        vanillaMaterials.clear();

        for (Material m : Material.values()) {
            if (m == null) continue;
            if (m == Material.AIR) continue;

            // 关键：不靠 ID，用“可实例化”来判断
            try {
                ItemStack test = new ItemStack(m, 1);
                if (test.getType() != m) continue; // 保险校验
            } catch (Throwable t) {
                continue;
            }

            vanillaMaterials.add(m);
        }

        // 排序：按名字排序，稳定且不依赖 id（你不喜欢 id，我们就不用）
        Collections.sort(vanillaMaterials, new Comparator<Material>() {
            @Override
            public int compare(Material a, Material b) {
                return a.name().compareToIgnoreCase(b.name());
            }
        });
    }

    public int size() {
        return vanillaMaterials.size();
    }

    public List<Material> getAll() {
        return Collections.unmodifiableList(vanillaMaterials);
    }

    /**
     * 分页（page 从 1 开始）
     */
    public List<Material> page(int page, int pageSize) {
        if (pageSize <= 0) pageSize = 45;
        if (page <= 0) page = 1;

        int from = (page - 1) * pageSize;
        if (from >= vanillaMaterials.size()) {
            return Collections.emptyList();
        }

        int to = Math.min(from + pageSize, vanillaMaterials.size());
        return vanillaMaterials.subList(from, to);
    }

    /**
     * 判断某个 ItemStack 是否“允许存入”的原版物品（严一点）
     */
    public boolean isAllowedDeposit(ItemStack item, boolean onlyPureVanilla) {
        if (item == null) return false;
        Material type = item.getType();
        if (type == null || type == Material.AIR) return false;

        // 必须在我们的“原版池”里
        if (!vanillaMaterials.contains(type)) return false;

        if (!onlyPureVanilla) return true;

        // 纯原版：无自定义 meta/附魔/lore 等
        try {
            if (item.hasItemMeta()) return false;
            if (!item.getEnchantments().isEmpty()) return false;
        } catch (Throwable ignored) {
            // 1.7.10 上 hasItemMeta/enchantments 都可用；这里防极端兼容问题
        }

        return true;
    }
}
