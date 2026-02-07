# DimensionBank

A cross-dimension bank system for Minecraft 1.7.10 servers.  
一个面向 1.7.10 老版本服务器的跨世界银行插件。

---

## 📦 Features | 功能特性

- ✅ Cross-world item bank（跨世界物品银行）
- ✅ GUI deposit & withdraw（图形化存取界面）
- ✅ Batch deposit / withdraw（批量存取）
- ✅ Search & sort system（搜索与排序）
- ✅ Item localization (item-lang)（物品翻译支持）
- ✅ Menu language support（菜单国际化）
- ✅ Cooldown protection（操作冷却保护）
- ✅ Lightweight file storage（轻量文件存储）

---

## ⚙️ Environment | 运行环境

| Item | Requirement |
|------|-------------|
| Minecraft | 1.7.10 |
| Server | Spigot / Thermos / Cauldron |
| Java | Java 8 |
| Dependency | None |

---

## 📥 Installation | 安装方法

1. Download `DimensionBank.jar`
2. Put into `/plugins/`
3. Restart server
4. Configure `config.yml`

下载插件 → 放入 plugins → 重启 → 修改配置

---

## 📖 Commands | 指令

| Command | Description |
|---------|-------------|
| `/db` | Show help |
| `/db open` | Open withdraw GUI |
| `/db box` | Open deposit box |
| `/db bal` | View balance |
| `/db list` | List vanilla items |
| `/db reload` | Reload config |

示例：

```bash
/db open
/db box
/db bal
