package org.tining.dimensionbank;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class DepositMenu implements InventoryHolder {

    public static final int SIZE = 9;

    public static final int SLOT_HELP  = 0;
    public static final int SLOT_FUNNEL = 4;
    public static final int SLOT_CLOSE = 8;

    private final DimensionBank plugin;
    private final String playerName;

    private final Inventory inv;

    public DepositMenu(DimensionBank plugin, String playerName) {
        this.plugin = plugin;
        this.playerName = playerName;
        this.inv = Bukkit.createInventory(this, SIZE, plugin.getMenuLang().tr(
                "menu.deposit.title", "DimensionBank - 存入"
        ));
        render();
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public String getPlayerName() {
        return playerName;
    }

    private void render() {
        inv.clear();

        // filler
        ItemStack glass = makeButton(Material.STAINED_GLASS_PANE,
                "§7",
                "§7");

        for (int i = 0; i < SIZE; i++) inv.setItem(i, glass);

        // help
        inv.setItem(SLOT_HELP, makeButton(Material.CHEST,
                "§b" + plugin.getMenuLang().tr("menu.deposit.help", "如何存入"),
                "§7" + plugin.getMenuLang().tr("menu.deposit.help_line1", "右键点击物品栏中的物品来存入该格"),
                "§7" + plugin.getMenuLang().tr("menu.deposit.help_line2", "左键点击漏斗存入所有可存入的物品"),
                "§7" + plugin.getMenuLang().tr("menu.deposit.help_line3", "无法存入的物品会被忽略")
        ));

        // funnel
        inv.setItem(SLOT_FUNNEL, makeButton(Material.HOPPER,
                "§a" + plugin.getMenuLang().tr("menu.deposit.funnel", "存入漏斗"),
                "§a" + plugin.getMenuLang().tr("menu.deposit.funnel_left", "左键：存入所有可存入的物品"),
                "§7" + plugin.getMenuLang().tr("menu.deposit.funnel_confirm", "需要确认（双击）"),
                "§b" + plugin.getMenuLang().tr("menu.deposit.slot_left", "左键物品栏物品：存入该格")
        ));

        // close
        inv.setItem(SLOT_CLOSE, makeButton(Material.REDSTONE_BLOCK,
                "§c" + plugin.getMenuLang().tr("menu.deposit.close", "关闭"),
                "§7" + plugin.getMenuLang().tr("menu.deposit.close_line", "点击关闭")
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

    public static void open(DimensionBank plugin, Player p) {
        DepositMenu menu = new DepositMenu(plugin, p.getName());
        p.openInventory(menu.getInventory());
    }
}
