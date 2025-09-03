package net.blazn.orin.engine.Commands.Premium;

import net.blazn.orin.engine.Managers.DisguiseManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class DisguiseCommand implements CommandExecutor {

    private final RankManager rankManager;
    private final DisguiseManager disguiseManager;
    private final JavaPlugin plugin;

    // Only ELITE+ ranks can disguise
    private final List<String> allowedRanks = List.of("OWNER", "DEVELOPER", "ADMIN", "MOD", "HELPER", "YOUTUBE", "VIP", "MVP");

    public DisguiseCommand(JavaPlugin plugin, RankManager rankManager, DisguiseManager disguiseManager) {
        this.rankManager = rankManager;
        this.disguiseManager = disguiseManager;
        this.plugin = plugin;

        plugin.getCommand("disguise").setExecutor(this);
        plugin.getCommand("undisguise").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.onlyPlayers);
            return true;
        }

        String playerRank = rankManager.getRank(player);

        // Check permission
        if (!allowedRanks.contains(playerRank)) {
            player.sendMessage(ChatUtil.noPermission);
            return true;
        }

        String command = cmd.getName().toLowerCase();

        switch (command) {
            case "disguise" -> disguiseManager.disguise(player);
            case "undisguise" -> {
                // ✅ Check if player is actually disguised
                if (!disguiseManager.isDisguised(player.getUniqueId())) {
                    player.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.white + " You are not disguised.");
                    return true;
                }
                disguiseManager.undisguise(player);
            }
            default -> player.sendMessage(ChatUtil.red + "Unknown command.");
        }

        return true;
    }
}
