package org.tining.dimensionbank;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

public final class MenuListener implements Listener {

    private final DimensionBank plugin;

    // 简单冷却：按玩家名（大小写敏感）
    private final Map<String, Long> lastActionMs = new HashMap<String, Long>();

    public MenuListener(DimensionBank plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player)) return;

        if (!(e.getInventory().getHolder() instanceof WithdrawMenu)) return;

        e.setCancelled(true); // 防止把展示物品拖走

        if (e.getClickedInventory() == null) return;
        if (e.getCurrentItem() == null) return;
        if (e.getCurrentItem().getType() == Material.AIR) return;

        Player p = (Player) e.getWhoClicked();
        WithdrawMenu menu = (WithdrawMenu) e.getInventory().getHolder();

        int raw = e.getRawSlot();

        // 点击玩家自己背包区域也取消（避免乱拖）
        if (raw >= WithdrawMenu.SIZE) return;

        // 冷却
        if (!plugin.getCooldowns().pass(p.getName())) {
            return;
        }

        // 底栏按钮
        if (raw == WithdrawMenu.SLOT_PREV) {
            WithdrawMenu.open(plugin, p, menu.getPage() - 1);
            return;
        }
        if (raw == WithdrawMenu.SLOT_NEXT) {
            WithdrawMenu.open(plugin, p, menu.getPage() + 1);
            return;
        }
        if (raw == WithdrawMenu.SLOT_INFO) {
            return;
        }
        if (raw == WithdrawMenu.SLOT_SEARCH) {
            // 右键：清空搜索并刷新
            if (e.isRightClick()) {
                MenuSession s = plugin.getSessions().get(p.getName());
                s.filter = "";
                s.page = 1;
                p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.search.cleared",
                        "已清空搜索。"
                ));

                menu.refresh(p); // 你现在 refresh 是原地 redraw，不会跳焦点
                return;
            }

            // 左键：进入搜索输入模式
            plugin.getSearchListener().begin(p);
            return;
        }

        if (raw == WithdrawMenu.SLOT_SORT) {

            MenuSession s = plugin.getSessions().get(p.getName());

            // 左键：切换字段（数量 <-> 名字）
            if (e.isLeftClick()) {
                if (s.sortField == null || s.sortField == SortField.AMOUNT) {
                    s.sortField = SortField.NAME;
                } else {
                    s.sortField = SortField.AMOUNT;
                }
                s.page = 1;
            }

            // 右键：切换方向（ASC <-> DESC）
            else if (e.isRightClick()) {
                if (s.sortDir == null || s.sortDir == SortDir.DESC) {
                    s.sortDir = SortDir.ASC;
                } else {
                    s.sortDir = SortDir.DESC;
                }
                s.page = 1;
            } else {
                return;
            }

            String show;

            if (s.sortField == SortField.AMOUNT) {

                show = (s.sortDir == SortDir.DESC)

                        ? plugin.getMenuLang().tr(
                        "menu.sort.amount_desc",
                        "数量↓"
                )

                        : plugin.getMenuLang().tr(
                        "menu.sort.amount_asc",
                        "数量↑"
                );

            } else {

                show = (s.sortDir == SortDir.ASC)

                        ? plugin.getMenuLang().tr(
                        "menu.sort.name_az",
                        "A-Z"
                )

                        : plugin.getMenuLang().tr(
                        "menu.sort.name_za",
                        "Z-A"
                );
            }

            p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.sort.changed",
                    "排序已切换为: %mode%",
                    "%mode%", show
            ));

            menu.refresh(p);
            return;

        }


        // 物品区域
        if (raw < 0 || raw >= WithdrawMenu.PAGE_SIZE) return;

        Material m = menu.getMaterialAtSlot(raw);
        if (m == null || m == Material.AIR) return;

        FileConfiguration cfg = plugin.getPluginConfig();

        int leftStacks = Math.max(1, cfg.getInt("withdraw.left-click-stacks", 1));
        boolean rightEnable = cfg.getBoolean("withdraw.right-click-enable", true);
        int rightMaxStacks = Math.max(1, cfg.getInt("withdraw.right-click-max-stacks", 5));
        boolean dropIfFull = cfg.getBoolean("withdraw.drop-if-full", true);

        int maxStackSize = safeMaxStackSize(m); // 1/16/64...

        int stacks;
        if (e.isRightClick()) {
            if (!rightEnable) return;
            stacks = rightMaxStacks;
        } else if (e.isLeftClick()) {
            stacks = leftStacks;
        } else {
            // 其他点击方式（中键等）暂时忽略
            return;
        }

        long want = (long) maxStackSize * (long) stacks;

        // 如果不允许掉地上，就按背包容量裁剪，避免“扣了银行却发不出来”
        if (!dropIfFull) {
            int fit = calcFit(p.getInventory(), m, maxStackSize);
            if (fit <= 0) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.withdraw.inv_full_cannot_withdraw",
                        "背包已满，无法取出。"
                ));
                return;
            }
            if (want > fit) want = fit;
        }

        // 从银行扣除（不会超扣）
        long taken = plugin.getBank().takeAmount(p.getName(), m, want);
        if (taken <= 0) {
            p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                    "menu.withdraw.insufficient_balance",
                    "该物品余额不足。"
            ));
            // 余额不足时也刷新一下（可能显示旧了）
            menu.refresh(p);
            return;
        }


        giveOrDrop(p, m, (int) taken, dropIfFull);

        // 刷新菜单（最稳）
        menu.refresh(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof WithdrawMenu) {
            e.setCancelled(true);
        }
    }

    private int safeMaxStackSize(Material m) {
        try {
            int s = m.getMaxStackSize();
            return s <= 0 ? 64 : s;
        } catch (Throwable ignored) {
            return 64;
        }
    }

    private void giveOrDrop(Player p, Material m, int amount, boolean dropIfFull) {

        int maxStack = safeMaxStackSize(m);

        int left = amount;
        boolean dropped = false;

        while (left > 0) {
            int give = Math.min(left, maxStack);

            ItemStack stack = new ItemStack(m, give);

            Map<Integer, ItemStack> leftover = p.getInventory().addItem(stack);

            if (!leftover.isEmpty()) {
                for (ItemStack it : leftover.values()) {
                    if (it == null || it.getType() == Material.AIR) continue;

                    if (dropIfFull) {
                        p.getWorld().dropItemNaturally(p.getLocation(), it);
                        dropped = true;
                    } else {
                        // 不掉地上：兜底加回银行
                        plugin.getBank().addAmount(p.getName(), m, it.getAmount());
                    }
                }
            }

            left -= give;
        }

        if (dropped) {
            p.sendMessage("§e[DimensionBank] " +
                    plugin.getMenuLang().tr(
                            "menu.withdraw.full_drop",
                            "背包已满，部分物品已掉落在脚下"
                    ));

        }
    }


    /**
     * 计算背包还能装下多少该 Material（以“物品数量”计）
     */
    private int calcFit(PlayerInventory inv, Material m, int maxStack) {

        int fit = 0;

        ItemStack[] contents = inv.getContents();
        if (contents == null) return 0;

        for (ItemStack it : contents) {
            if (it == null || it.getType() == Material.AIR) {
                fit += maxStack;
                continue;
            }
            if (it.getType() == m) {
                int space = maxStack - it.getAmount();
                if (space > 0) fit += space;
            }
        }

        return fit;
    }

    private boolean passCooldown(String playerName) {

        int cdMs = plugin.getPluginConfig().getInt("cooldown-ms", -1);

        // 兼容旧配置：cooldown(秒)
        if (cdMs < 0) {
            int cdSec = Math.max(0, plugin.getPluginConfig().getInt("cooldown", 3));
            cdMs = cdSec * 1000;
        }

        if (cdMs <= 0) return true;

        long now = System.currentTimeMillis();
        Long last = lastActionMs.get(playerName);
        if (last != null) {
            if (now - last < cdMs) return false;
        }
        lastActionMs.put(playerName, now);
        return true;
    }

}
