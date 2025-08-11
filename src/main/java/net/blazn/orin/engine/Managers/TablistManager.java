package net.blazn.orin.engine.Managers;

import net.blazn.orin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TablistManager {

    private final JavaPlugin plugin;

    public TablistManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setTablist(Player player, String header, String footer) {
        header = ChatColor.translateAlternateColorCodes('&', header);
        footer = ChatColor.translateAlternateColorCodes('&', footer);

        player.setPlayerListHeaderFooter(header, footer);
    }

    public void setTablistForAll(String header, String footer) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setTablist(player, header, footer);
        }
    }
}
