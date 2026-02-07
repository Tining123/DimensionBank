package org.tining.dimensionbank;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

public final class SearchInputListener implements Listener {

    private final DimensionBank plugin;

    private static final long WINDOW_MS = 10_000L; // 10秒
    private final Map<String, Long> pendingUntil = new HashMap<String, Long>();

    public SearchInputListener(DimensionBank plugin) {
        this.plugin = plugin;
    }

    public void begin(Player p) {
        pendingUntil.put(p.getName(), System.currentTimeMillis() + WINDOW_MS);

        p.sendMessage("§b[DimensionBank] " + plugin.getMenuLang().tr(
                "menu.search.prompt",
                "请输入搜索关键词（%sec%秒内有效）。输入 %cmd% 清空。",
                "%sec%", String.valueOf(WINDOW_MS / 1000),
                "%cmd%", "§fclear§b"
        ));
    }


    private boolean isPending(String name) {
        Long until = pendingUntil.get(name);
        return until != null && System.currentTimeMillis() <= until;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        String name = p.getName();

        if (!isPending(name)) return;

        // 这是搜索输入，不让它发到公共聊天
        e.setCancelled(true);
        pendingUntil.remove(name);

        final String msg = e.getMessage() == null ? "" : e.getMessage().trim();

        // 异步事件里不能直接操作 GUI，切回主线程
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {

                if (msg.length() == 0) {
                    p.sendMessage("§7[DimensionBank] " + plugin.getMenuLang().tr(
                            "menu.search.cancelled",
                            "已取消搜索输入。"
                    ));
                    return;
                }

                // 设置过滤
                MenuSession s = plugin.getSessions().get(name);
                s.filter = msg.toLowerCase();
                s.page = 1;

                p.sendMessage("§a[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.search.set",
                        "已设置搜索：%msg%",
                        "%msg%", msg
                ));

                p.sendMessage("§b[DimensionBank] " + plugin.getMenuLang().tr(
                        "menu.search.prompt",
                        "请输入搜索关键词（%sec%秒内有效）。",
                        "%sec%", String.valueOf(WINDOW_MS / 1000)
                ));

                p.sendMessage("§7" + plugin.getMenuLang().tr(
                        "menu.search.clear_tip",
                        "右键点击罗盘可清空搜索。"
                ));

                // 重新打开菜单（这里重开没关系：是搜索行为，不是取出点击）
                WithdrawMenu.open(plugin, p, s.page);

            }
        });
    }
}
