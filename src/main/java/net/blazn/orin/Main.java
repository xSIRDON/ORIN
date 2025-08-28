package net.blazn.orin;

import net.blazn.orin.engine.Commands.HelpCommand;
import net.blazn.orin.engine.Commands.ListCommand;
import net.blazn.orin.engine.Commands.MessageCommand;
import net.blazn.orin.engine.Commands.Operator.*;
import net.blazn.orin.engine.Commands.Premium.NickCommand;
import net.blazn.orin.engine.Commands.Punishment.*;
import net.blazn.orin.engine.Commands.Staff.*;
import net.blazn.orin.engine.Listeners.ChatListener;
import net.blazn.orin.engine.Listeners.JoinListener;
import net.blazn.orin.engine.Listeners.QuitListener;
import net.blazn.orin.engine.Listeners.ServerListPingListener;
import net.blazn.orin.engine.Listeners.Staff.PunishmentListener;
import net.blazn.orin.engine.Listeners.Staff.RankListener;
import net.blazn.orin.engine.Listeners.Staff.WatchdogListener;
import net.blazn.orin.engine.Managers.*;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

public final class Main extends JavaPlugin {

    private SQLManager sqlManager;
    private NameManager nameManager;
    private RankManager rankManager;
    private MOTDManager motdManager;
    private PermissionsManager permissionsManager;
    private PunishmentManager punishmentManager;
    private TablistManager tablistManager;
    private WatchdogManager watchdogManager;

    private static final String PLUGIN_NAME = "orin-1.0-SNAPSHOT";
    private static File pluginFile;
    private static long lastModified = 0;

    File ranksFile = new File(getDataFolder(), "ranks.yml");
    FileConfiguration ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);

    @Override
    public void onEnable() {
        registerConfigurations();
        registerManagers();
        registerListeners();
        registerCommands();
        startUpdateChecker();
        getLogger().info("✅ ORIN has been enabled!");
        tablistManager.setTablistForAll("&b&lᴏʀɪɴ ɴᴇᴛᴡᴏʀᴋ\n&7ᴏɴʟɪɴᴇ ᴘʟᴀʏᴇʀѕ&8: &f" + Bukkit.getOnlinePlayers().size() + "\n ", " \n&eᴏʀɪɴ.ᴍᴏᴅᴅᴇᴅ.ꜰᴜɴ");
    }

    @Override
    public void onDisable() {
        if (sqlManager != null) {
            sqlManager.closeConnection(); // ✅ Properly close database connection
        }
        getLogger().info("❌ ORIN has been disabled.");
    }

    private void registerConfigurations() {
        saveDefaultConfig();
        saveResource("ranks.yml", false);
        ChatUtil.loadConfig(this);
    }

    private void registerManagers() {
        sqlManager = new SQLManager(this);
        nameManager = new NameManager(this, sqlManager);
        rankManager = new RankManager(this, sqlManager, nameManager);
        motdManager = new MOTDManager(this);
        permissionsManager = new PermissionsManager(ranksConfig);
        punishmentManager = new PunishmentManager(this, sqlManager);
        tablistManager = new TablistManager(this);
        watchdogManager = new WatchdogManager(this, nameManager, tablistManager);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new RankListener(this, rankManager, nameManager), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this, nameManager, rankManager, tablistManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, rankManager, nameManager, permissionsManager), this);
        getServer().getPluginManager().registerEvents(new QuitListener(this, nameManager, rankManager, tablistManager), this);
        getServer().getPluginManager().registerEvents(new ServerListPingListener(this, motdManager), this);
        getServer().getPluginManager().registerEvents(new PunishmentListener(this, rankManager, punishmentManager, nameManager), this);
        getServer().getPluginManager().registerEvents(new WatchdogListener(this, watchdogManager), this);
    }

    private void registerCommands() {
        //REGULAR COMMANDS
        getCommand("help").setExecutor(new HelpCommand(this, rankManager));
        getCommand("list").setExecutor(new ListCommand(this, rankManager, watchdogManager));
        getCommand("msg").setExecutor(new MessageCommand(this, rankManager));

        //STAFF COMMANDS
        getCommand("broadcast").setExecutor(new BroadcastCommand(this, rankManager, permissionsManager));
        getCommand("history").setExecutor(new HistoryCommand(this, punishmentManager, rankManager, nameManager));
        getCommand("staff").setExecutor(new StaffCommand(this, rankManager));
        getCommand("watchdog").setExecutor(new WatchdogCommand(this, watchdogManager, rankManager, permissionsManager));

        //PUNISHMENT COMMANDS
        getCommand("ban").setExecutor(new BanCommand(this, rankManager, nameManager, permissionsManager, punishmentManager));
        getCommand("tempban").setExecutor(new TempbanCommand(this, rankManager, nameManager, punishmentManager));
        getCommand("kick").setExecutor(new KickCommand(this, rankManager, nameManager, permissionsManager, punishmentManager));
        getCommand("mute").setExecutor(new MuteCommand(this, rankManager, nameManager, permissionsManager, punishmentManager));
        getCommand("tempmute").setExecutor(new TempmuteCommand(this, rankManager, nameManager, punishmentManager));
        getCommand("warn").setExecutor(new WarnCommand(this, rankManager, nameManager, permissionsManager, punishmentManager));

        //ADMIN COMMANDS
        getCommand("clearhistory").setExecutor(new ClearHistoryCommand(this, punishmentManager, rankManager, nameManager));
        getCommand("rank").setExecutor(new RankCommand(this, rankManager, nameManager, permissionsManager));
        getCommand("unban").setExecutor(new UnbanCommand(this, rankManager, nameManager, permissionsManager, punishmentManager));
        getCommand("unmute").setExecutor(new UnmuteCommand(this, rankManager, nameManager, permissionsManager, punishmentManager));

        //PREMIUM COMMANDS
        getCommand("nick").setExecutor(new NickCommand(nameManager, rankManager, permissionsManager));
        getCommand("fly").setExecutor(new FlyCommand(this, rankManager, permissionsManager));

        //OPERATOR COMMANDS
        getCommand("clear").setExecutor(new ClearCommand(this, rankManager, permissionsManager));
        getCommand("gamemode").setExecutor(new GamemodeCommand(this, rankManager, permissionsManager));
        getCommand("gmc").setExecutor(new GamemodeCommand(this, rankManager, permissionsManager));
        getCommand("gms").setExecutor(new GamemodeCommand(this, rankManager, permissionsManager));
        getCommand("godmode").setExecutor(new GodmodeCommand(this, rankManager, permissionsManager));
        getCommand("heal").setExecutor(new HealCommand(this, rankManager, permissionsManager));
        getCommand("kill").setExecutor(new KillCommand(this, rankManager, permissionsManager));
        getCommand("tp").setExecutor(new TeleportCommand(this, rankManager, permissionsManager));
        getCommand("setmotd").setExecutor(new SetMOTDCommand(this, rankManager, nameManager, motdManager));
    }

    /**
     * 🔄 Automatically checks if the plugin file has been updated and reloads if needed.
     */
    private void startUpdateChecker() {
        pluginFile = new File("plugins/" + PLUGIN_NAME + ".jar");

        if (!pluginFile.exists()) {
            getLogger().warning("⛔ Plugin file not found, skipping update check.");
            return;
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                FileTime fileTime = Files.getLastModifiedTime(pluginFile.toPath());
                long newModified = fileTime.toMillis();

                if (lastModified > 0 && newModified > lastModified) {
                    getLogger().info("⚡ Detected plugin update, reloading...");
                    Bukkit.getScheduler().runTask(this, () ->
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "reload confirm")
                    );
                }
                lastModified = newModified;
            } catch (Exception e) {
                getLogger().severe("❌ Failed to check plugin update timestamp: " + e.getMessage());
            }
        }, 0L, 200L); // Runs every 5 seconds (200 ticks)
    }
}
