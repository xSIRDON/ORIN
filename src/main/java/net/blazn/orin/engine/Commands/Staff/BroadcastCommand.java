package net.blazn.orin.engine.Commands.Staff;

import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class BroadcastCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD");

    public BroadcastCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;

        plugin.getCommand("broadcast").setExecutor(this);
        plugin.getCommand("bc").setExecutor(this); // Alias
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ": " + ChatUtil.white + "/" + label + " <message>");
            return true;
        }

        // Rank check if sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String rank = rankManager.getRank(player);

            if (!allowedRanks.contains(rank)) {
                player.sendMessage(ChatUtil.noPermission);
                return true;
            }
        }

        // Combine arguments into a single message
        String message = String.join(" ", args);

        // Format message
        String formatted = /*ChatUtil.bgold + "📢"+ */ ChatUtil.bdarkRed + "BROADCAST " + ChatUtil.darkGray + "| " + ChatUtil.white + message;

        // Send chat message to all players
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(formatted);

            // Show title in center of screen
            p.sendTitle(/*ChatUtil.bgold + "📢"+ */ ChatUtil.bdarkRed + "ʙʀᴏᴀᴅᴄᴀѕᴛ", ChatUtil.white + message, 10, 70, 20);

            // Play a notification sound
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        });

        // Also send to console
        Bukkit.getConsoleSender().sendMessage(formatted);

        return true;
    }
}
