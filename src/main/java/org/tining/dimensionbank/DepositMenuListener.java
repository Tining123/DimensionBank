package org.tining.dimensionbank;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class DepositMenuListener implements Listener {

    private final DimensionBank plugin;

    // 二次确认窗口：玩家名 -> 截止时间
    private final Map<String, Long> confirmUntil = new HashMap<String, Long>();

    // 二次确认有效期（毫秒）
    private static final long CONFIRM_WINDOW_MS = 5000L;

    public DepositMenuListener(DimensionBank plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        Inventory top = e.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof DepositMenu)) return;

        int raw = e.getRawSlot();
        int topSize = top.getSize();

        // 上层 GUI：全部禁止拿走/移动
        if (raw < topSize) {
            e.setCancelled(true);

            if (raw == DepositMenu.SLOT_CLOSE) {
                p.closeInventory();
                return;
            }

            if (raw == DepositMenu.SLOT_FUNNEL) {
                // 只认左键：全存（带二次确认）
                if (!e.isLeftClick()) return;

                // 冷却
                if (!plugin.getCooldowns().pass(p.getName())) {
                    int ms = plugin.getCooldowns().getCooldownMs();
                    p.sendMessage("§7[DimensionBank] " + plugin.getMenuLang().tr(
                            "menu.cooldown", "冷却中 (%ms%ms)", "%ms%", String.valueOf(ms)
                    ));
                    return;
                }

                // 二次确认
                long now = System.currentTimeMillis();
                Long until = confirmUntil.get(p.getName());
                if (until == null || until.longValue() < now) {
                    confirmUntil.put(p.getName(), now + CONFIRM_WINDOW_MS);
                    p.sendMessage("§e[DimensionBank] " + plugin.getMenuLang().tr(
                            "menu.deposit.confirm",
                            "在 5 秒内再次点击确认存入所有物品。"
                    ));
                    return;
                }

                // 确认通过：执行全存
                confirmUntil.remove(p.getName());
                depositAll(p);
                return;
            }

            return;
        }

        // 下层：玩家背包/快捷栏区域
        // 你要求“只支持（左键）把当前格子全部存入”
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == null || clicked.getType() == Material.AIR) {
            e.setCancelled(true);
            return;
        }

        // 不让 Bukkit 默认把物品拿起来/交换
        e.setCancelled(true);

        // 只认左键
        if (!e.isLeftClick()) return;

        // 冷却
        if (!plugin.getCooldowns().pass(p.getName())) {
            int ms = plugin.getCooldowns().getCooldownMs();
            p.sendMessage("§7[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.cooldown", "冷却中 (%ms%ms)", "%ms%", String.valueOf(ms)
            ));
            return;
        }

        depositSlot(p, e.getSlot(), clicked);
    }

    /**
     * 存入一个格子的全部数量（1..64）
     * slotIndex: 玩家背包槽位（0..35 等）
     */
    private void depositSlot(Player p, int slotIndex, ItemStack clicked) {

        // 获取当前玩家的种类数
        int currentTypes = plugin.getBank().countTypes(p.getName());
        int maxTypes = plugin.getConfig().getInt("deposit.max-types.value", 100); // 从配置中读取最大种类数

        // 如果启用最大种类数限制，并且当前种类数已达到上限，阻止存入
        if (plugin.getConfig().getBoolean("deposit.max-types.enable", true) && currentTypes >= maxTypes) {
            p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.deposit.type_cap", "存入失败：你的银行已达到最大种类上限（%max% 种）",
                    "%max%", String.valueOf(maxTypes)
            ));
            return;
        }

        // 只处理背包+快捷栏
        if (slotIndex < 0 || slotIndex > 35) {
            p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.deposit.slot_not_allowed", "此格不允许存入物品。"
            ));
            return;
        }

        Material type = clicked.getType();
        if (type == null || type == Material.AIR) return;

        int amount = clicked.getAmount();
        if (amount <= 0) return;

        // deny
        List<String> deny = plugin.getConfig().getStringList("deny-items");
        if (deny != null && deny.contains(type.name())) {
            p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.deposit.deny", "该物品禁止存入。"
            ));
            return;
        }

        // ===== ID 段检测 =====
        if (!plugin.isIdAllowed(type)) {

            int id = type.getId();

            p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.deposit.id_blocked",
                    "该物品 ID=%id% 不允许存入",
                    "%id%", String.valueOf(id)
            ));

            return;
        }

        boolean onlyPure = plugin.getConfig().getBoolean("only-pure-vanilla", true);
        if (!plugin.getVanillaRegistry().isAllowedDeposit(clicked, onlyPure)) {
            p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.deposit.invalid", "该物品不符合存入规则。"
            ));
            return;
        }

        // 快照
        final int slot = slotIndex;
        final ItemStack snap = clicked.clone();

        // 下一 tick 执行（关键）
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {

                ItemStack now = p.getInventory().getItem(slot);

                // 格子变了就放弃（不硬刚）
                if (now == null || now.getType() != snap.getType()) {
                    return;
                }

                int realAmount = now.getAmount();
                if (realAmount <= 0) return;

                // 清格
                p.getInventory().setItem(slot, null);
                p.updateInventory();

                // 入库
                long total = plugin.getBank().addAmount(
                        p.getName(),
                        snap.getType(),
                        realAmount
                );

                String show = plugin.getItemLang().tr(snap.getType());

                p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.deposit.slot_ok",
                        "已存入 %name% (%id%) ×%amount% | 余额: %total%",
                        "%name%", show,
                        "%id%", snap.getType().name(),
                        "%amount%", String.valueOf(realAmount),
                        "%total%", String.valueOf(total)
                ));
            }
        });
    }



    /**
     * 漏斗左键：存入背包所有可存物品（不限种类，忽略不可存）
     */
    private void depositAll(Player p) {

        // 获取当前玩家的种类数
        int currentTypes = plugin.getBank().countTypes(p.getName());
        int maxTypes = plugin.getConfig().getInt("deposit.max-types.value", 100); // 从配置中读取最大种类数

        // 如果启用最大种类数限制，并且当前种类数已达到上限，阻止存入
        if (plugin.getConfig().getBoolean("deposit.max-types.enable", true) && currentTypes >= maxTypes) {
            p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.deposit.type_cap", "存入失败：你的银行已达到最大种类上限（%max% 种）",
                    "%max%", String.valueOf(maxTypes)
            ));
            return;
        }

        ItemStack[] contents = p.getInventory().getContents();

        List<String> deny = plugin.getConfig().getStringList("deny-items");
        boolean onlyPure = plugin.getConfig().getBoolean("only-pure-vanilla", true);

        // 统计（Material -> count）
        Map<Material, Long> delta = new HashMap<Material, Long>();

        long moved = 0L;
        long ignored = 0L;

        // 只扫 0..35（背包 + 快捷栏）
        for (int i = 0; i <= 35 && i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() == null || it.getType() == Material.AIR) continue;

            Material type = it.getType();

            // ID 段限制
            if (!plugin.isIdAllowed(type)) {
                ignored += it.getAmount();
                continue;
            }


            // deny
            if (deny != null && deny.contains(type.name())) {
                ignored += it.getAmount();
                continue;
            }

            // pure vanilla check
            if (!plugin.getVanillaRegistry().isAllowedDeposit(it, onlyPure)) {
                ignored += it.getAmount();
                continue;
            }

            long add = it.getAmount();
            moved += add;

            Long old = delta.get(type);
            delta.put(type, (old == null ? add : old.longValue() + add));

            contents[i] = null; // 清空该槽
        }

        if (moved <= 0) {
            p.sendMessage("§e[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.deposit.all_empty",
                    "背包中没有可存入的物品。"
            ));
            return;
        }

        // 先清背包
        p.getInventory().setContents(contents);
        p.updateInventory();

        // 再入库
        //（按 Material 批量累加）
        for (Map.Entry<Material, Long> en : delta.entrySet()) {
            plugin.getBank().addAmount(p.getName(), en.getKey(), en.getValue());
        }

        p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                "menu.deposit.all_ok",
                "已存入所有物品：存入数量=%moved%，忽略数量=%ignored%",
                "%moved%", String.valueOf(moved),
                "%ignored%", String.valueOf(ignored)
        ));
    }
}
