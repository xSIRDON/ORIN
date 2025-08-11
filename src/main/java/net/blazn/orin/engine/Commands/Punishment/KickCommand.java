package net.blazn.orin.engine.Commands.Punishment;

import net.blazn.orin.engine.Managers.*;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class KickCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PermissionsManager permissionsManager;
    private final PunishmentManager punishmentManager;

    public KickCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PermissionsManager permissionsManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.permissionsManager = permissionsManager;
        this.punishmentManager = punishmentManager;
        plugin.getCommand("kick").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // ✅ Console always has permission
        if (!(sender instanceof Player)) {
            if (args.length < 2) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + " /kick <player> <reason>");
                return true;
            }

            String targetName = args[0];
            if (!nameManager.playerExists(targetName)) {
                sender.sendMessage(ChatUtil.doesNotExist);
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID targetUUID = target.getUniqueId();
            String reason = String.join(" ", args).replace(targetName, "").trim();
            executeKick(target, "CONSOLE", reason);

            String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));
            String display = nameManager.getDisplayName(targetUUID);
            sender.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Kicked " + rankPrefix + display + ChatUtil.white + " for" + ChatUtil.darkGray + ": " + ChatUtil.yellow + reason);
            return true;
        }

        // ✅ Player execution
        Player player = (Player) sender;
        String senderRank = rankManager.getRank(player);

        // ⛔ Permission check FIRST for players
        if (permissionsManager.getRankLevel(senderRank) < permissionsManager.getRankLevel("SRMOD")) {
            sender.sendMessage(ChatUtil.noPermission);
            return true;
        }

        // ✅ Now we check arguments
        if (args.length < 1) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" + ChatUtil.white + " /kick <player>");
            return true;
        }

        String targetName = args[0];
        if (!nameManager.playerExists(targetName)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        if (!target.isOnline()) {
            sender.sendMessage(ChatUtil.notOnline);
            return true;
        }

        String targetRank = rankManager.getRank(target.getPlayer());
        if (!permissionsManager.canModify(senderRank, targetRank)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot kick this player.");
            return true;
        }

        openKickMenu(player, targetUUID);
        return true;
    }


    private void executeKick(OfflinePlayer target, String staffName, String reason) {
        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            punishmentManager.kickPlayer(targetPlayer, staffName, reason);
        }

        Bukkit.broadcastMessage(ChatUtil.darkRed + "⚠ " + ChatUtil.gold + target.getName() +
                ChatUtil.white + " has been kicked for" + ChatUtil.darkGray + ": " + ChatUtil.yellow + reason);
    }

    private void openKickMenu(Player player, UUID targetUUID) {
        punishmentManager.setPendingPunishment(PunishmentManager.PunishmentType.KICK, targetUUID, "PENDING");

        Inventory kickMenu = Bukkit.createInventory(null, 45, ChatUtil.bred + "Select Kick Reason");

        FileConfiguration config = plugin.getConfig();
        List<String> offenses = config.getStringList("punishments.kick.reasons"); // ✅ Fetch offenses from config.yml

        int[] offenseSlots = {11, 15, 22, 29, 33};
        ItemStack pane = createItem(Material.RED_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                kickMenu.setItem(i, pane);
            }
        }

        for (int i = 0; i < Math.min(offenses.size(), offenseSlots.length); i++) {
            kickMenu.setItem(offenseSlots[i], createItem(Material.PAPER, ChatUtil.byellow + offenses.get(i), ChatUtil.gray + "Click to select kick reason."));
        }

        player.openInventory(kickMenu);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
