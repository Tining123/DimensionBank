package org.tining.dimensionbank;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class DimensionBank extends JavaPlugin {

    private static DimensionBank instance;

    private File bankFolder;
    private FileConfiguration config;
    private BankManager bank;
    private Cooldowns cooldowns;
    private SessionManager sessionManager;
    private SearchInputListener searchListener;
    private MenuLang menuLang;
    private ItemLangManager itemLangManager;

    private final VanillaRegistry vanillaRegistry = new VanillaRegistry();
    private LangManager lang;
    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();
        reloadConfig();
        config = getConfig();

        // 加载翻译
        lang = new LangManager(this);
        menuLang = new MenuLang(this);
        // 初始化并加载 item-lang
        itemLangManager = new ItemLangManager(this);
        itemLangManager.load();

        if (!config.getBoolean("bank-enable", true)) {
            warn(menuLang.tr(
                    "core.bank_disabled",
                    "银行系统已关闭（bank-enable=false），插件仍会加载，但核心功能不会启用。"
            ));
        }

        initBankFolder();

        // 初始化原版列表（A 步骤的核心）
        vanillaRegistry.init();
        log(menuLang.tr(
                "core.vanilla_loaded",
                "原版物品枚举完成，数量=%count%",
                "%count%", String.valueOf(vanillaRegistry.size())
        ));

        lang.load();
        // 加载库存
        bank = new BankManager(this);
        // 注册公共冷却
        cooldowns = new Cooldowns(this);


        // 注册菜单
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);

        // 注册session用于搜索
        sessionManager = new SessionManager();
        // 注册搜索监听
        searchListener = new SearchInputListener(this);
        Bukkit.getPluginManager().registerEvents(searchListener, this);
        // 注册收购箱
        getServer().getPluginManager().registerEvents(new DepositMenuListener(this), this);

        // 注册命令
        if (getCommand("db") != null) {
            getCommand("db").setExecutor(new DimensionBankCommand(this));
        } else {
            warn(menuLang.tr(
                    "core.command_missing",
                    "plugin.yml 里没有注册 /db 指令！"
            ));

        }

        // You can find the plugin id of your plugins on
        // the page https://bstats.org/what-is-my-plugin-id
        int pluginId = 29370;
        Metrics metrics = new Metrics(this, pluginId);
        getLogger().info("bStats enabled.");
        log(menuLang.tr(
                "core.enabled",
                "DimensionBank 已启动！"
        ));



    }

    @Override
    public void onDisable() {
        log(menuLang.tr(
                "core.disabled",
                "DimensionBank 已关闭！"
        ));
    }

    private void initBankFolder() {
        String path = config.getString("bank-path", "./DimensionBank/data/");
        bankFolder = new File(path);

        if (!bankFolder.exists()) {
            if (bankFolder.mkdirs()) {
                log(menuLang.tr(
                        "core.bank_dir_created",
                        "已创建银行目录: %path%",
                        "%path%", bankFolder.getAbsolutePath()
                ));

            } else {
                warn(menuLang.tr(
                        "core.bank_dir_failed",
                        "无法创建银行目录: %path%",
                        "%path%", bankFolder.getAbsolutePath()
                ));

            }
        }
    }

    public static DimensionBank getInstance() {
        return instance;
    }

    public File getBankFolder() {
        return bankFolder;
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public VanillaRegistry getVanillaRegistry() {
        return vanillaRegistry;
    }

    public void log(String msg) {
        Bukkit.getConsoleSender().sendMessage("§a[DimensionBank] §f" + msg);
    }

    public void warn(String msg) {
        Bukkit.getConsoleSender().sendMessage("§c[DimensionBank] §f" + msg);
    }

    public LangManager getLang() {
        return lang;
    }

    public BankManager getBank() {
        return bank;
    }

    public Cooldowns getCooldowns() {
        return cooldowns;
    }
    public SessionManager getSessions() {
        return sessionManager;
    }
    public SearchInputListener getSearchListener() { return searchListener; }
    public MenuLang getMenuLang() {
        return menuLang;
    }
    public ItemLangManager getItemLang() {
        return itemLangManager;
    }

}
