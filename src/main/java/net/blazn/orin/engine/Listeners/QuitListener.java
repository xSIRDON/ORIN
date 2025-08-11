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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class QuitListener implements Listener {

    private final NameManager nameManager;
    private final RankManager rankManager;
    private final TablistManager tablistManager;
    private final JavaPlugin plugin;

    public QuitListener(JavaPlugin plugin, NameManager nameManager, RankManager rankManager, TablistManager tablistManager) {
        this.plugin = plugin;
        this.nameManager = nameManager;
        this.rankManager = rankManager;
        this.tablistManager = tablistManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setupConfigDefaults(); // Ensure messages exist in config
    }

    /**
     * Ensures default quit message settings exist in config.yml.
     */
    private void setupConfigDefaults() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("messages.quit.enabled")) {
            config.set("messages.quit.enabled", true);
        }
        if (!config.contains("messages.quit.format")) {
            config.set("messages.quit.format", "{displayname} &7has left.");
        }

        plugin.saveConfig();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FileConfiguration config = plugin.getConfig();
        Player player = event.getPlayer();

        // ✅ Handle custom quit messages
        if (!config.getBoolean("messages.quit.enabled")) {
            event.setQuitMessage(null); // Disable quit messages if set to false
            return;
        }

        String quitMessage = config.getString("messages.quit.format", "{displayname} &7has left.");

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

        if (quitMessage != null) {
            quitMessage = quitMessage.replace("{displayname}", displayName);
            event.setQuitMessage(ChatUtil.swapAmp(quitMessage)); // Apply formatted quit message
        }

        tablistManager.setTablistForAll("&b&lᴏʀɪɴ ɴᴇᴛᴡᴏʀᴋ\n&7ᴏɴʟɪɴᴇ ᴘʟᴀʏᴇʀѕ&8: &f" + Bukkit.getOnlinePlayers().size() + "\n ", " \n&eᴏʀɪɴ.ᴍᴏᴅᴅᴇᴅ.ꜰᴜɴ");
    }
}
