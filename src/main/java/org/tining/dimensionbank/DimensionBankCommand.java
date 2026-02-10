package org.tining.dimensionbank;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;


public final class DimensionBankCommand implements CommandExecutor {

    private final DimensionBank plugin;

    public DimensionBankCommand(DimensionBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§a" + plugin.getMenuLang().tr(
                    "command.help.title",
                    "DimensionBank - 用法："
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.list",
                    "/db list <page> 列出原版物品（分页）"
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.reload",
                    "/db reload 重载配置并重建原版列表"
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.bal",
                    "/db bal [page] 查看你的银行余额"
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.open",
                    "/db open [page] 打开取出菜单"
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.save",
                    "/db save 存入当前手持物品"
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.saveall",
                    "/db saveall 存入当前手持物品，包括背包中的同类物品"
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.take",
                    "/db take 取出手持物品 1 组"
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.takeall",
                    "/db takeall 取出手持物品多组"
            ));

            sender.sendMessage("§e" + plugin.getMenuLang().tr(
                    "command.help.version",
                    "/db version 查看版本"
            ));

            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof Player) || sender.hasPermission("dimensionbank.adbin")) {

                plugin.reloadConfig();
                plugin.getVanillaRegistry().init();

                sender.sendMessage("§a[DimensionBank] §f" +
                        plugin.getMenuLang().tr(
                                "command.reload.success",
                                "已重载配置，并重建原版物品列表。数量=%count%",
                                "%count%", String.valueOf(plugin.getVanillaRegistry().size())
                        )
                );

            } else {

                sender.sendMessage("§c" +
                        plugin.getMenuLang().tr(
                                "command.reload.no_permission",
                                "你没有权限。"
                        )
                );
            }

            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {

            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (Throwable ignored) {
                    page = 1;
                }
            }

            int pageSize = 45;

            List<Material> mats = plugin.getVanillaRegistry().page(page, pageSize);

            int total = plugin.getVanillaRegistry().size();
            int maxPage = (int) Math.ceil(total / (double) pageSize);

            sender.sendMessage("§a[DimensionBank] §f" +
                    plugin.getMenuLang().tr(
                            "command.list.title",
                            "原版物品列表 第 %page%/%max% 页（共 %total%）",
                            "%page%", String.valueOf(page),
                            "%max%", String.valueOf(maxPage),
                            "%total%", String.valueOf(total)
                    )
            );

            if (mats.isEmpty()) {
                sender.sendMessage("§7" + plugin.getMenuLang().tr(
                        "command.list.empty",
                        "（这一页没有内容）"
                ));
                return true;
            }

            // 一行输出多个，避免刷屏太狠
            StringBuilder line = new StringBuilder();
            int col = 0;

            for (Material m : mats) {
                if (col > 0) line.append("§7, ");

                // 从 item-lang 取显示名
                String show = plugin.getItemLang().tr(m);

                line.append("§f").append(show)
                        .append("§7(").append(m.name()).append("§7)");

                col++;

                if (col >= 6) {
                    sender.sendMessage(line.toString());
                    line.setLength(0);
                    col = 0;
                }
            }


            if (line.length() > 0) {
                sender.sendMessage(line.toString());
            }

            return true;
        }

        if ("bal".equalsIgnoreCase(args[0]) || "balance".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("§c" + plugin.getMenuLang().tr(
                        "command.bal.only_player",
                        "只能在游戏内使用。"
                ));
                return true;
            }

            Player p = (Player) sender;

            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (Throwable ignored) {
                    page = 1;
                }
            }
            if (page <= 0) page = 1;

            // 取非零库存
            Map<String, Long> map = plugin.getBank().getNonZeroItems(p.getName());

            if (map.isEmpty()) {
                p.sendMessage("§a[DimensionBank] §f" +
                        plugin.getMenuLang().tr(
                                "command.bal.empty",
                                "你的银行是空的。"
                        )
                );
                return true;
            }

            // 排序：按 key（Material.name）稳定排序
            ArrayList<String> keys = new ArrayList<String>(map.keySet());
            Collections.sort(keys, new Comparator<String>() {
                @Override
                public int compare(String a, String b) {
                    return a.compareToIgnoreCase(b);
                }
            });

            int pageSize = 10; // 聊天栏别刷太多
            int total = keys.size();
            int maxPage = (int) Math.ceil(total / (double) pageSize);

            if (page > maxPage) page = maxPage;

            int from = (page - 1) * pageSize;
            int to = Math.min(from + pageSize, total);

            p.sendMessage("§a[DimensionBank] §f" +
                    plugin.getMenuLang().tr(
                            "command.bal.title",
                            "银行余额 第 %page%/%max% 页（共 %total% 种）",
                            "%page%", String.valueOf(page),
                            "%max%", String.valueOf(maxPage),
                            "%total%", String.valueOf(total)
                    )
            );

            for (int i = from; i < to; i++) {

                String matName = keys.get(i);
                long amount = map.get(matName);

                Material m = null;
                try {
                    m = Material.valueOf(matName);
                } catch (Throwable ignored) {}

                String display = (m != null) ? plugin.getItemLang().tr(m) : matName;

                p.sendMessage("§f- " + display +
                        " §7(" + matName + ") §e×" + amount);
            }

            return true;
        }

        if ("open".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof org.bukkit.entity.Player)) {

                sender.sendMessage("§c" + plugin.getMenuLang().tr(
                        "command.open.only_player",
                        "只能在游戏内使用。"
                ));

                return true;
            }

            org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;

            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (Throwable ignored) {
                    page = 1;
                }
            }

            WithdrawMenu.open(plugin, p, page);
            return true;
        }

        if ("box".equalsIgnoreCase(args[0]) || "savebox".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("§c" + plugin.getMenuLang().tr("command.only_player", "只能在游戏内使用。"));
                return true;
            }

            Player p = (Player) sender;
            DepositMenu.open(plugin, p);
            return true;
        }


        if ("save".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("§c" + plugin.getMenuLang().tr(
                        "command.save.only_player",
                        "只能在游戏内使用。"
                ));
                return true;
            }

            Player p = (Player) sender;

            ItemStack hand = p.getItemInHand(); // 1.7.10
            if (hand == null || hand.getType() == null || hand.getType() == Material.AIR) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.save.no_item",
                        "你手里没有物品。"
                ));
                return true;
            }

            // deny-items
            List<String> deny = plugin.getPluginConfig().getStringList("deny-items");
            if (deny != null && deny.contains(hand.getType().name())) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.save.deny",
                        "该物品禁止存入。"
                ));
                return true;
            }

            boolean onlyPure = plugin.getPluginConfig().getBoolean("only-pure-vanilla", true);
            if (!plugin.getVanillaRegistry().isAllowedDeposit(hand, onlyPure)) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.save.invalid",
                        "该物品不符合存入规则（可能含有附魔/自定义信息）。"
                ));
                return true;
            }

            int maxStack = hand.getType().getMaxStackSize();
            int amountInHand = hand.getAmount();

            int deposit = Math.min(maxStack, amountInHand); // 1组或不足1组就全存
            if (deposit <= 0) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.save.failed",
                        "无法存入。"
                ));
                return true;
            }

            // ===== 种类上限检测 =====
            if (!plugin.getBank().canAddNewType(p.getName(), hand.getType())) {

                int max = plugin.getConfig()
                        .getInt("deposit.max-types.value", 100);

                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.deposit.type_cap",
                        "存入失败：你的银行已达到最大种类上限（%max% 种）",
                        "%max%", String.valueOf(max)
                ));

                return true;
            }

            // ===== ID 段检测 =====
            if (!plugin.isIdAllowed(hand.getType())) {

                int id = hand.getType().getId();

                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.deposit.id_blocked",
                        "该物品 ID=%id% 不允许存入",
                        "%id%", String.valueOf(id)
                ));

                return true;
            }


            // 扣手上数量
            int remain = amountInHand - deposit;
            if (remain <= 0) {
                p.setItemInHand(new ItemStack(Material.AIR));
            } else {
                hand.setAmount(remain);
                p.setItemInHand(hand);
            }
            p.updateInventory();

            // 入库
            long now = plugin.getBank().addAmount(p.getName(), hand.getType(), deposit);

            String show = plugin.getItemLang().tr(hand.getType());

            p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                    "command.save.success",
                    "已存入 %name% (%id%) ×%amount% | 余额: %total%",
                    "%name%", show,
                    "%id%", hand.getType().name(),
                    "%amount%", String.valueOf(deposit),
                    "%total%", String.valueOf(now)
            ));

            return true;
        }

        if ("saveall".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("§c" + plugin.getMenuLang().tr(
                        "command.saveall.only_player",
                        "只能在游戏内使用。"
                ));
                return true;
            }

            Player p = (Player) sender;

            ItemStack hand = p.getItemInHand();
            if (hand == null || hand.getType() == null || hand.getType() == Material.AIR) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.saveall.no_item",
                        "你手里没有物品。"
                ));
                return true;
            }

            Material type = hand.getType();

            // deny-items
            List<String> deny = plugin.getPluginConfig().getStringList("deny-items");
            if (deny != null && deny.contains(type.name())) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.saveall.deny",
                        "该物品禁止存入。"
                ));
                return true;
            }

            // ===== 种类上限检测 =====
            if (!plugin.getBank().canAddNewType(p.getName(), hand.getType())) {

                int max = plugin.getConfig()
                        .getInt("deposit.max-types.value", 100);

                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.deposit.type_cap",
                        "存入失败：你的银行已达到最大种类上限（%max% 种）",
                        "%max%", String.valueOf(max)
                ));

                return true;
            }

            // ===== ID 段检测 =====
            if (!plugin.isIdAllowed(hand.getType())) {

                int id = hand.getType().getId();

                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.deposit.id_blocked",
                        "该物品 ID=%id% 不允许存入",
                        "%id%", String.valueOf(id)
                ));

                return true;
            }


            boolean onlyPure = plugin.getPluginConfig().getBoolean("only-pure-vanilla", true);

            // 扫整个背包（包含快捷栏），把同类都存进去
            ItemStack[] contents = p.getInventory().getContents();
            long deposit = 0L;

            for (int i = 0; i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it == null || it.getType() == Material.AIR) continue;
                if (it.getType() != type) continue;

                // 同类但不符合规则（有meta/附魔）则跳过，不动它
                if (!plugin.getVanillaRegistry().isAllowedDeposit(it, onlyPure)) {
                    continue;
                }

                deposit += it.getAmount();
                contents[i] = null; // 清掉该槽位
            }

            if (deposit <= 0) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.saveall.empty",
                        "背包里没有可存入的同类物品（可能含附魔/自定义信息）。"
                ));
                return true;
            }

            p.getInventory().setContents(contents);
            p.updateInventory();

            long now = plugin.getBank().addAmount(p.getName(), type, deposit);

            String show = plugin.getItemLang().tr(type);

            p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                    "command.saveall.success",
                    "已存入 %name% (%id%) ×%amount% | 余额: %total%",
                    "%name%", show,
                    "%id%", type.name(),
                    "%amount%", String.valueOf(deposit),
                    "%total%", String.valueOf(now)
            ));

            return true;
        }

        if ("take".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("§c" + plugin.getMenuLang().tr(
                        "command.take.only_player",
                        "只能在游戏内使用。"
                ));
                return true;
            }

            Player p = (Player) sender;

            if (!plugin.getCooldowns().pass(p.getName())) {
                int ms = plugin.getCooldowns().getCooldownMs();
                p.sendMessage("§7[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.take.cooldown",
                        "冷却中（%ms%ms）",
                        "%ms%", String.valueOf(ms)
                ));
                return true;
            }

            ItemStack hand = p.getItemInHand();
            if (hand == null || hand.getType() == null || hand.getType() == Material.AIR) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.take.need_item",
                        "你需要手里拿着一种物品，才能指定要取出的类型。"
                ));
                return true;
            }

            Material type = hand.getType();
            int stackSize = Math.max(1, type.getMaxStackSize());

            long taken = plugin.getBank().takeAmount(p.getName(), type, stackSize);
            if (taken <= 0) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.take.no_balance",
                        "银行里没有该物品余额。"
                ));
                return true;
            }

            boolean dropIfFull = plugin.getPluginConfig().getBoolean("withdraw.drop-if-full", true);
            giveOrDrop(p, type, (int) taken, dropIfFull);

            String show = plugin.getItemLang().tr(type);
            p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                    "command.take.success",
                    "已取出 %name% (%id%) ×%amount%",
                    "%name%", show,
                    "%id%", type.name(),
                    "%amount%", String.valueOf(taken)
            ));

            return true;
        }

        if ("takeall".equalsIgnoreCase(args[0])) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("§c" + plugin.getMenuLang().tr(
                        "command.takeall.only_player",
                        "只能在游戏内使用。"
                ));
                return true;
            }

            Player p = (Player) sender;

            if (!plugin.getCooldowns().pass(p.getName())) {
                int ms = plugin.getCooldowns().getCooldownMs();
                p.sendMessage("§7[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.takeall.cooldown",
                        "冷却中（%ms%ms）",
                        "%ms%", String.valueOf(ms)
                ));
                return true;
            }

            ItemStack hand = p.getItemInHand();
            if (hand == null || hand.getType() == null || hand.getType() == Material.AIR) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.takeall.need_item",
                        "你需要手里拿着一种物品，才能指定要取出的类型。"
                ));
                return true;
            }

            Material type = hand.getType();
            int stackSize = Math.max(1, type.getMaxStackSize());

            boolean rightEnable = plugin.getPluginConfig().getBoolean("withdraw.right-click-enable", true);
            int maxStacks = Math.max(1, plugin.getPluginConfig().getInt("withdraw.right-click-max-stacks", 5));
            boolean dropIfFull = plugin.getPluginConfig().getBoolean("withdraw.drop-if-full", true);

            if (!rightEnable) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.takeall.disabled",
                        "takeall 已被配置关闭。"
                ));
                return true;
            }

            long want = (long) stackSize * (long) maxStacks;

            long taken = plugin.getBank().takeAmount(p.getName(), type, want);
            if (taken <= 0) {
                p.sendMessage("§c[DimensionBank] " + plugin.getMenuLang().tr(
                        "command.takeall.no_balance",
                        "银行里没有该物品余额。"
                ));
                return true;
            }

            giveOrDrop(p, type, (int) taken, dropIfFull);

            String show = plugin.getItemLang().tr(type);
            p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                    "command.takeall.success",
                    "已取出 %name% (%id%) ×%amount%",
                    "%name%", show,
                    "%id%", type.name(),
                    "%amount%", String.valueOf(taken)
            ));

            return true;
        }

        if ("version".equalsIgnoreCase(args[0]) || "ver".equalsIgnoreCase(args[0])) {

            String v = plugin.getDescription().getVersion();

            sender.sendMessage("§a[DimensionBank] §fVersion: §e" + v);
            sender.sendMessage("§a[DimensionBank] §7Author: Tining");
            sender.sendMessage("§a[DimensionBank] §7Core: Bank + GUI + Search + Sort");

            return true;
        }

        sender.sendMessage("§c" + plugin.getMenuLang().tr(
                "command.unknown",
                "未知子命令。用 /db 查看帮助。"
        ));

        return true;
    }

    private void giveOrDrop(Player p, Material m, int amount, boolean dropIfFull) {

        int maxStack = Math.max(1, m.getMaxStackSize());
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
                        plugin.getBank().addAmount(p.getName(), m, it.getAmount());
                    }
                }
            }

            left -= give;
        }

        p.updateInventory();

        if (dropped) {
            p.sendMessage("§e[DimensionBank] " +
                    plugin.getMenuLang().tr(
                            "menu.withdraw.full_drop",
                            "背包已满，部分物品已掉落在脚下"
                    ));

        }
    }


}
