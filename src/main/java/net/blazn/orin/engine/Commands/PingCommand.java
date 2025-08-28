package net.blazn.orin.engine.Commands;

import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PingCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public PingCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("ping").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.red + "❌ Only players can use this command.");
            return true;
        }

        // Get player ping (Bukkit 1.20+ exposes it via getPing)
        int ping = player.getPing();

        // Format message
        String formatted = ChatUtil.bgold + "\uD83C\uDF10 " + ChatUtil.white + "Ping: " +
                ChatUtil.green + ping + "ms";

        player.sendMessage(formatted);
        return true;
    }
}