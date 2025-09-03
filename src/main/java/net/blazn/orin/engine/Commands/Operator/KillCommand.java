package net.blazn.orin.engine.Commands.Operator;

import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class KillCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN");

    public KillCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;
        plugin.getCommand("kill").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Console usage must specify a player
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /kill <player>");
            return true;
        }

        // Player executing the command
        Player player = (sender instanceof Player) ? (Player) sender : null;
        String playerRank = (player != null) ? rankManager.getRank(player) : "OWNER"; // Console has max rank

        // If no arguments, kill self
        if (args.length == 0 && player != null) {
            player.setHealth(0);
            player.sendMessage(ChatUtil.darkRed + "☠" + ChatUtil.white + " You have been killed.");
            return true;
        }

        // Kill another player
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(ChatUtil.notOnline);
            return true;
        }

        // Prevent lower-ranked players from killing higher-ranked ones
        String targetRank = rankManager.getRank(target);
        if (!permissionsManager.canModify(playerRank, targetRank)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.white + " You cannot kill this player.");
            return true;
        }

        // Kill the target
        target.setHealth(0);
        target.sendMessage(ChatUtil.red + "☠ You have been killed!");
        sender.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " You have killed" + ChatUtil.darkGray + ": " + target.getDisplayName());

        return true;
    }
}
