package net.blazn.orin.engine.Commands.Punishment;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.PunishmentManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempmuteCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PunishmentManager punishmentManager;

    public TempmuteCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.punishmentManager = punishmentManager;
        plugin.getCommand("tempmute").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof org.bukkit.entity.Player) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" +
                    ChatUtil.white + " /mute <player>");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" +
                    ChatUtil.white + " /tempmute <player> <duration> <reason>");
            return true;
        }

        String targetName = args[0];
        String durationString = args[1];
        String reason = String.join(" ", args).replace(targetName, "").replace(durationString, "").trim();

        if (!nameManager.playerExists(targetName)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();
        String targetDisplay = nameManager.getDisplayName(targetUUID);
        String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));

        long durationMillis = parseDuration(durationString);

        if (durationMillis <= 0) {
            sender.sendMessage("§fInvalid duration! §cUse" + ChatUtil.gray + ":" +
                    ChatUtil.green + " m §8(§fminutes§8)§7, §ah §8(§fhours§8)§7, §ad §8(§fdays§8)§7, " +
                    "§aw §8(§fweeks§8)§7, §amo §8(§fmonths§8)§7, §ay §8(§fyears§8)");
            return true;
        }

        if (punishmentManager.isMuted(targetUUID)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " That player is already muted.");
            return true;
        }

        if (punishmentManager.isTempMuted(targetUUID)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " That player is already muted.");
            return true;
        }

        punishmentManager.tempMutePlayer(target.getName(), "CONSOLE", reason, durationMillis);

        sender.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Temporarily muted " + rankPrefix + targetDisplay +
                ChatUtil.white + " for " + ChatUtil.bred + durationString.toUpperCase() + ChatUtil.white + " due to" + ChatUtil.darkGray + ": " + ChatUtil.yellow + reason);

        return true;
    }

    /**
     * ✅ Parses duration into milliseconds
     */
    private long parseDuration(String input) {
        Pattern pattern = Pattern.compile("(\\d+)([mhdwMy])");
        Matcher matcher = pattern.matcher(input);

        long totalMillis = 0;

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "m": totalMillis += value * 60 * 1000L; break;       // Minutes
                case "h": totalMillis += value * 60 * 60 * 1000L; break;  // Hours
                case "d": totalMillis += value * 24 * 60 * 60 * 1000L; break; // Days
                case "w": totalMillis += value * 7 * 24 * 60 * 60 * 1000L; break; // Weeks
                case "M": totalMillis += value * 30 * 24 * 60 * 60 * 1000L; break; // Months (approx.)
                case "y": totalMillis += value * 365 * 24 * 60 * 60 * 1000L; break; // Years (approx.)
                default: return -1;
            }
        }
        return totalMillis;
    }
}
