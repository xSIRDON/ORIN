package net.blazn.orin.engine.Commands.Staff;

import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StaffCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final JavaPlugin plugin;
    private final List<String> allowedRanks = List.of("OWNER", "DEVELOPER", "ADMIN", "SRMOD", "MOD");

    private static final Set<Player> staffChatEnabled = new HashSet<>();

    public StaffCommand(JavaPlugin plugin, RankManager rankManager) {
        this.rankManager = rankManager;
        this.plugin = plugin;
        plugin.getCommand("staff").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.red + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        String playerRank = rankManager.getRank(player);

        // Check if player has permission
        if (!allowedRanks.contains(playerRank)) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }
        // Toggle staff chat mode
        if (staffChatEnabled.contains(player)) {
            staffChatEnabled.remove(player);
            player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.white + " Staff chat" + ChatUtil.gray + ": " +
                    ChatUtil.bred + "DISABLED");
        } else {
            staffChatEnabled.add(player);
            player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Staff chat" + ChatUtil.gray + ": " +
                    ChatUtil.bgreen + "ENABLED");
        }

        return true;
    }

    /**
     * Checks if a player has staff chat enabled.
     */
    public static boolean isStaffChatEnabled(Player player) {
        return staffChatEnabled.contains(player);
    }
}
