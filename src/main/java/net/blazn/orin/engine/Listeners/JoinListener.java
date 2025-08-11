package net.blazn.orin.engine.Listeners;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Managers.TablistManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class JoinListener implements Listener {

    private final NameManager nameManager;
    private final RankManager rankManager;
    private final TablistManager tablistManager;
    private final JavaPlugin plugin;

    public JoinListener(JavaPlugin plugin, NameManager nameManager, RankManager rankManager, TablistManager tablistManager) {
        this.plugin = plugin;
        this.nameManager = nameManager;
        this.rankManager = rankManager;
        this.tablistManager = tablistManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setupConfigDefaults(); // Ensure messages exist in config
    }

    /**
     * Ensures default join message settings exist in config.yml.
     */
    private void setupConfigDefaults() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("messages.join.enabled")) {
            config.set("messages.join.enabled", true);
        }
        if (!config.contains("messages.join.format")) {
            config.set("messages.join.format", "{player} &7has joined.");
        }

        plugin.saveConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("messages.join.enabled")) {
            event.setJoinMessage(null); // Disable join messages if set to false
            return;
        }

        Player player = event.getPlayer();
        String joinMessage = config.getString("messages.join.format", "{player} &7has joined.");

        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        FileConfiguration ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        boolean usePrefixes = rankManager.shouldUseChatPrefixes();

        String displayName;
        if (usePrefixes) {
            String prefix = ranksConfig.getString("rank-prefixes." + rankManager.getRank(player).toUpperCase(), "");
            String name = player.getDisplayName();
            displayName = ChatUtil.swapAmp(prefix) + ChatUtil.white + ChatUtil.stripSec(name);
        } else {
            displayName = nameManager.getDisplayName(player);
        }

        if (joinMessage != null) {
            joinMessage = joinMessage.replace("{displayname}", displayName);
            event.setJoinMessage(ChatUtil.swapAmp(joinMessage)); // Apply formatted join message
        }

        tablistManager.setTablistForAll("&b&lᴏʀɪɴ ɴᴇᴛᴡᴏʀᴋ\n&7ᴏɴʟɪɴᴇ ᴘʟᴀʏᴇʀѕ&8: &f" + Bukkit.getOnlinePlayers().size() + "\n ", " \n&eᴏʀɪɴ.ᴍᴏᴅᴅᴇᴅ.ꜰᴜɴ");
    }
}
