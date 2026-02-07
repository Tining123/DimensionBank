package org.tining.dimensionbank;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class WithdrawMenu implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int PAGE_SIZE = 45;

    // 底栏按钮槽位
    public static final int SLOT_PREV = 45;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 53;
    public static final int SLOT_SEARCH = 48;
    public static final int SLOT_SORT = 50;


    private final DimensionBank plugin;
    private final String playerName;
    private final int page;

    // 当前页 0-44 对应的 Material（用于点击识别）
    private final Material[] pageMaterials = new Material[PAGE_SIZE];

    private Inventory inv;

    public WithdrawMenu(DimensionBank plugin, String playerName, int page) {
        this.plugin = plugin;
        this.playerName = playerName;
        this.page = Math.max(1, page);

        this.inv = Bukkit.createInventory(
                this,
                SIZE,
                plugin.getMenuLang().tr(
                        "menu.withdraw.title",
                        "DimensionBank - 取出"
                )
        );

        render();
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPage() {
        return page;
    }

    public Material getMaterialAtSlot(int slot) {
        if (slot < 0 || slot >= PAGE_SIZE) return null;
        return pageMaterials[slot];
    }

    private void render() {
        Arrays.fill(pageMaterials, null);
        inv.clear();

        // 读取该玩家所有非零余额
        Map<String, Long> map = plugin.getBank().getNonZeroItems(playerName);

        // 读取会话过滤词（统一为小写）
        String filter = "";
        try {
            filter = plugin.getSessions().get(playerName).filter;
        } catch (Throwable ignored) {}
        if (filter == null) filter = "";
        filter = filter.trim().toLowerCase();

        // 1) 把银行里有余额的物品转成 Material 列表
        List<Material> all = new ArrayList<Material>();
        for (String key : map.keySet()) {
            try {
                Material m = Material.valueOf(key);
                Long v = map.get(key);
                if (m != null && m != Material.AIR && v != null && v > 0) {
                    all.add(m);
                }
            } catch (Throwable ignored) {}
        }

        // 2) 排序（按字母稳定）
        MenuSession s = plugin.getSessions().get(playerName);

        final SortField field = (s != null && s.sortField != null) ? s.sortField : SortField.AMOUNT;
        final SortDir dir = (s != null && s.sortDir != null) ? s.sortDir : SortDir.DESC;

        Collections.sort(all, new Comparator<Material>() {
            @Override
            public int compare(Material a, Material b) {

                if (field == SortField.AMOUNT) {
                    long va = 0L;
                    long vb = 0L;

                    Long oa = map.get(a.name());
                    Long ob = map.get(b.name());
                    if (oa != null) va = oa;
                    if (ob != null) vb = ob;

                    if (va != vb) {
                        // ASC：小的前；DESC：大的前
                        if (dir == SortDir.ASC) {
                            return (va < vb) ? -1 : 1;
                        } else {
                            return (va > vb) ? -1 : 1;
                        }
                    }

                    // 数量相同：按名字稳定排序，防翻页抖动
                    return a.name().compareToIgnoreCase(b.name());
                }

                // field == NAME
                int c = a.name().compareToIgnoreCase(b.name());
                if (dir == SortDir.DESC) c = -c; // DESC 对名字就是 Z-A
                return c;
            }
        });

        // 3) 过滤（如果 filter 非空，只保留匹配的）
        List<Material> shown;
        if (filter.length() == 0) {
            shown = all;
        } else {
            shown = new ArrayList<Material>();
            for (Material m : all) {

                // 显示名（来自 item-lang：有翻译用翻译；没有就回退英文友好名）
                String display = plugin.getItemLang().tr(m);

                // 英文友好名（稳定）
                String enPretty = ItemLangManager.prettyEnglish(m.name()); // 或 plugin.getItemLang().prettyEnglish(...)
                // 枚举名（稳定）
                String enumName = m.name();

                String hay = (display + " " + enumName + " " + enPretty).toLowerCase();
                if (hay.contains(filter)) {
                    shown.add(m);
                }
            }
        }


        // 分页计算（基于 shown）
        int total = shown.size();
        int maxPage = (int) Math.ceil(total / (double) PAGE_SIZE);
        int safePage = Math.min(page, Math.max(1, maxPage));

        int from = (safePage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

// 4) 渲染当前页物品
        int slot = 0;
        for (int i = from; i < to; i++) {
            Material m = shown.get(i);
            long amount = map.get(m.name());

            ItemStack icon = new ItemStack(m, 1);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                String display = plugin.getItemLang().tr(m);
                meta.setDisplayName("§f" + display + " §7(" + m.name() + ")");

                List<String> lore = new ArrayList<String>();
                lore.add("§e" + plugin.getMenuLang().tr(
                        "menu.withdraw.balance",
                        "余额: ×%amount%",
                        "%amount%", String.valueOf(amount)
                ));

                lore.add("§a" + plugin.getMenuLang().tr(
                        "menu.withdraw.left",
                        "左键: 取 1 组"
                ));
                lore.add("§b" + plugin.getMenuLang().tr(
                        "menu.withdraw.right",
                        "右键: 取多组（有上限）"
                ));
                meta.setLore(lore);

                icon.setItemMeta(meta);
            }

            inv.setItem(slot, icon);
            pageMaterials[slot] = m;
            slot++;
        }


        // 5) 如果过滤后为空：放一个“空结果提示”
        String filterShow = (filter.length() > 0)
                ? filter
                : plugin.getMenuLang().tr("menu.search.none", "（无）");

        if (shown.isEmpty()) {
            inv.setItem(22, makeButton(Material.WEB,
                    "§7" + plugin.getMenuLang().tr(
                            "menu.search.status",
                            "搜索: %value%",
                            "%value%", "§f" + filterShow
                    ),
                    plugin.getMenuLang().tr("menu.search.empty", "没有匹配结果"),
                    plugin.getMenuLang().tr("menu.search.tip_input", "§b左键罗盘: 输入搜索"),
                    plugin.getMenuLang().tr("menu.search.tip_clear", "§c右键罗盘: 清空搜索（恢复全部）")
            ));
            // 注意：这里没有写入 pageMaterials[22]，所以 WEB 绝对不会被取出
        }


// 6) 底栏按钮
        inv.setItem(SLOT_PREV, makeButton(
                Material.ARROW,
                plugin.getMenuLang().tr("menu.page.prev", "§a← 上一页"),
                plugin.getMenuLang().tr("menu.page.prev_lore", "§7翻到上一页")
        ));

        inv.setItem(SLOT_NEXT, makeButton(
                Material.ARROW,
                plugin.getMenuLang().tr("menu.page.next", "§a下一页 →"),
                plugin.getMenuLang().tr("menu.page.next_lore", "§7翻到下一页")
        ));

// 显示当前排序
        String sortShow;
        if (field == SortField.AMOUNT) {
            sortShow = (dir == SortDir.DESC)
                    ? plugin.getMenuLang().tr("menu.sort.amount_desc", "数量↓")
                    : plugin.getMenuLang().tr("menu.sort.amount_asc", "数量↑");
        } else {
            sortShow = (dir == SortDir.ASC)
                    ? plugin.getMenuLang().tr("menu.sort.name_az", "A-Z")
                    : plugin.getMenuLang().tr("menu.sort.name_za", "Z-A");
        }

        inv.setItem(SLOT_SORT, makeButton(
                Material.HOPPER,
                plugin.getMenuLang().tr(
                        "menu.sort.title",
                        "§d排序: §f%mode%",
                        "%mode%", sortShow
                ),
                plugin.getMenuLang().tr("menu.sort.tip_left", "§7左键切换排序模式"),
                plugin.getMenuLang().tr("menu.sort.hint_amount", "§7- 数量↓：先看你最多的"),
                plugin.getMenuLang().tr("menu.sort.hint_name", "§7- A-Z：按名字字母")
        ));



        inv.setItem(SLOT_SEARCH, makeButton(
                Material.COMPASS,
                plugin.getMenuLang().tr("menu.search.title", "§b罗盘：搜索/清空"),
                plugin.getMenuLang().tr("menu.search.current", "§7当前搜索: §f%filter%", "%filter%", filterShow),
                plugin.getMenuLang().tr("menu.search.tip_input", "§b左键: 输入搜索关键词"),
                plugin.getMenuLang().tr("menu.search.tip_clear", "§c右键: 清空搜索（恢复全部）")
        ));

        inv.setItem(SLOT_INFO, makeButton(
                Material.PAPER,
                plugin.getMenuLang().tr("menu.info.search", "§7搜索: §f%filter%", "%filter%", filterShow),
                plugin.getMenuLang().tr("menu.info.page", "§e第 %page% 页", "%page%", String.valueOf(safePage)),
                plugin.getMenuLang().tr("menu.info.balance", "§7只显示你银行里有余额的物品"),
                plugin.getMenuLang().tr("menu.info.left_click", "§7左键物品: 取 1 组；右键: 取多组"),
                plugin.getMenuLang().tr("menu.info.search_left", "§b左键罗盘: 输入搜索"),
                plugin.getMenuLang().tr("menu.info.search_right", "§c右键罗盘: 清空搜索"),
                plugin.getMenuLang().tr("menu.info.close", "§7按 ESC 关闭")
        ));

    }


    private ItemStack makeButton(Material mat, String name, String... loreLines) {
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLines != null && loreLines.length > 0) {
                meta.setLore(Arrays.asList(loreLines));
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    public static void open(DimensionBank plugin, Player p, int page) {
        WithdrawMenu menu = new WithdrawMenu(plugin, p.getName(), page);
        p.openInventory(menu.getInventory());
    }

    /**
     * 原地重绘：不重新 openInventory，避免鼠标焦点乱跳到页码（slot49）
     */
    public void redraw() {
        render();
    }

    public void refresh(Player p) {
        // 旧逻辑会重开 GUI，导致鼠标/焦点跳动
        // open(plugin, p, page);

        // 改为原地重绘
        redraw();
        p.updateInventory(); // 1.7.10 用它强制客户端同步
    }
}
