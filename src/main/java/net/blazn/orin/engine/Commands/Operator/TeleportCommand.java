package net.blazn.orin.engine.Commands.Operator;

import net.blazn.orin.engine.Managers.PermissionsManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class TeleportCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final PermissionsManager permissionsManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = Arrays.asList("OWNER", "DEVELOPER", "ADMIN", "SRMOD", "MOD");

    public TeleportCommand(JavaPlugin plugin, RankManager rankManager, PermissionsManager permissionsManager) {
        this.rankManager = rankManager;
        this.permissionsManager = permissionsManager;
        this.plugin = plugin;
        plugin.getCommand("tp").setExecutor(this);
        plugin.getCommand("teleport").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Ensure sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.onlyPlayers);
            return true;
        }

        Player player = (Player) sender;
        String playerRank = rankManager.getRank(player);

        // Permission check
        if (!allowedRanks.contains(playerRank)) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }

        if (args.length == 1) {
            // Teleport to another player
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                player.sendMessage(ChatUtil.notOnline);
                return true;
            }

            String targetRank = rankManager.getRank(target);

            if (!permissionsManager.canTeleport(playerRank, targetRank, true)) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot teleport to this player.");
                return true;
            }

            player.teleport(target);
            player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " You teleported to " + ChatUtil.gold + target.getDisplayName());
            return true;
        }

        if (args.length == 2) {
            // Teleport another player to target
            Player target = Bukkit.getPlayer(args[0]);
            Player destination = Bukkit.getPlayer(args[1]);

            if (target == null || destination == null) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " One or both players are not online.");
                return true;
            }

            String targetRank = rankManager.getRank(target);
            String destinationRank = rankManager.getRank(destination);

            if (!permissionsManager.canTeleport(playerRank, targetRank, false)) {
                player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot teleport this player.");
                return true;
            }

            target.teleport(destination);
            target.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " You have been teleported to " + ChatUtil.gold + destination.getDisplayName());
            player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " You teleported " + ChatUtil.gold + target.getDisplayName() + ChatUtil.white + " to " + destination.getDisplayName());
            return true;
        }

        if (args.length == 3) {
            // Teleport to coordinates
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);

                Location location = new Location(player.getWorld(), x, y, z);
                player.teleport(location);
                player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " You teleported to coordinates: " + ChatUtil.gold + x + ", " + y + ", " + z);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatUtil.red + "Invalid coordinates! Use numbers like: /tp 100 64 -200");
            }
            return true;
        }

        // Invalid usage
        player.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /tp <player> OR /tp <player1> <player2> OR /tp <x> <y> <z>");
        return true;
    }
}
