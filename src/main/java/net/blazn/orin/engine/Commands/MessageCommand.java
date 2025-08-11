package net.blazn.orin.engine.Commands;

import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MessageCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;

    public MessageCommand(JavaPlugin plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        plugin.getCommand("msg").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Ensure sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.red + "Only players can use this command.");
            return true;
        }

        Player senderPlayer = (Player) sender;

        // Validate arguments
        if (args.length < 2) {
            senderPlayer.sendMessage(ChatUtil.red + "Usage" +
                    ChatUtil.darkGray + ":" + ChatUtil.white + " /msg <player> <message>");
            return true;
        }

        // Find target player
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            senderPlayer.sendMessage(ChatUtil.notOnline);
            return true;
        }

        // Prevent messaging self
        if (targetPlayer.equals(senderPlayer)) {
            senderPlayer.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You can't message yourself.");
            return true;
        }

        // Build message
        String message = String.join(" ", args).substring(args[0].length()).trim();

        // Send messages
        if(rankManager.shouldUseChatPrefixes()) {
            String senderrank = rankManager.getRank(senderPlayer);
            String sendername = senderPlayer.getDisplayName();
            String senderprefix = rankManager.getRankPrefix(senderrank);
            String targetrank = rankManager.getRank(targetPlayer);
            String targetname = targetPlayer.getDisplayName();
            String targetprefix = rankManager.getRankPrefix(targetrank);

            String updatedTrgtName = targetprefix + ChatUtil.stripSec(targetname);
            String updatedSndrName = senderprefix + ChatUtil.stripSec(sendername);

            senderPlayer.sendMessage(ChatUtil.gold + "✉" + ChatUtil.white + " To " + ChatUtil.white +
                    updatedTrgtName + ChatUtil.darkGray + ": " + ChatUtil.green + message);
            targetPlayer.sendMessage(ChatUtil.gold + "✉" + ChatUtil.white + " From " + ChatUtil.white +
                    updatedSndrName + ChatUtil.gray + ": " + ChatUtil.green + message);
        } else {
            senderPlayer.sendMessage(ChatUtil.gold + "✉" + ChatUtil.white + " To " + ChatUtil.white + targetPlayer.getDisplayName() + ChatUtil.darkGray + ": " + ChatUtil.green + message);
            targetPlayer.sendMessage(ChatUtil.gold + "✉" + ChatUtil.white + " From " + ChatUtil.white + senderPlayer.getDisplayName() + ChatUtil.gray + ": " + ChatUtil.green + message);
        }
        return true;
    }
}
