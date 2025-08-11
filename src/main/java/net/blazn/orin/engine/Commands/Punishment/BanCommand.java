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

public class BanCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final NameManager nameManager;
    private final PermissionsManager permissionsManager;
    private final PunishmentManager punishmentManager;

    public BanCommand(JavaPlugin plugin, RankManager rankManager, NameManager nameManager, PermissionsManager permissionsManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        this.permissionsManager = permissionsManager;
        this.punishmentManager = punishmentManager;
        plugin.getCommand("ban").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // ✅ Console Execution - Instantly applies a permanent ban
        if (!(sender instanceof Player)) {
            if (args.length < 2) {
                sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" +
                        ChatUtil.white + " /ban <player> <reason>");
                return true;
            }

            String targetName = args[0];
            if (!nameManager.playerExists(targetName)) {
                sender.sendMessage(ChatUtil.doesNotExist);
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID targetUUID = target.getUniqueId();
            String targetDisplay = nameManager.getDisplayName(targetUUID);
            String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));

            String reason = String.join(" ", args).replace(targetName, "").trim(); // ✅ Combine reason

            punishmentManager.banPlayer(target.getName(), "CONSOLE", reason);

            sender.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Banned " + rankPrefix +
                    targetDisplay + ChatUtil.white + " for" + ChatUtil.darkGray + ": " + ChatUtil.yellow + reason);
            return true;
        }

        // ✅ Player Execution - Ensure they have permission **BEFORE** usage check
        Player player = (Player) sender;
        String senderRank = rankManager.getRank(player);

        if (permissionsManager.getRankLevel(senderRank) < permissionsManager.getRankLevel("SRMOD")) {
            sender.sendMessage(ChatUtil.noPermission);
            return true;
        }

        // ✅ Ensure correct usage **only if they pass the permission check**
        if (args.length < 1) {
            sender.sendMessage(ChatUtil.red + "Usage" + ChatUtil.darkGray + ":" +
                    ChatUtil.white + " /ban <player>");
            return true;
        }

        String targetName = args[0];
        if (!nameManager.playerExists(targetName)) {
            sender.sendMessage(ChatUtil.doesNotExist);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();
        String targetDisplay = nameManager.getDisplayName(targetUUID);
        String rankPrefix = rankManager.getRankColor(rankManager.getRank(targetUUID));

        String targetRank = (target.isOnline()) ? rankManager.getRank(target.getPlayer()) : "UNKNOWN";

        if (!permissionsManager.canBan(senderRank, targetRank)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " You cannot ban this player.");
            return true;
        }

        if (punishmentManager.isBanned(targetUUID)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " That player is already banned.");
            return true;
        }

        if (punishmentManager.isTempBanned(targetUUID)) {
            sender.sendMessage(ChatUtil.darkRed + "❌" + ChatUtil.red + " That player is already temp-banned.");
            return true;
        }

        openBanMenu(player, targetUUID);
        return true;
    }

    private void openBanMenu(Player player, UUID targetUUID) {
        punishmentManager.setPendingPunishment(PunishmentManager.PunishmentType.BAN, targetUUID, "PENDING");

        Inventory banMenu = Bukkit.createInventory(null, 45, ChatUtil.bred + "Select Ban Reason");

        FileConfiguration config = plugin.getConfig();
        List<String> offenses = config.getStringList("punishments.ban.reasons"); // ✅ Fetch offenses from config.yml

        int[] offenseSlots = {11, 15, 22, 29, 33};
        ItemStack pane = createItem(Material.RED_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                banMenu.setItem(i, pane);
            }
        }

        for (int i = 0; i < Math.min(offenses.size(), offenseSlots.length); i++) {
            banMenu.setItem(offenseSlots[i], createItem(Material.PAPER, ChatUtil.byellow + offenses.get(i), ChatUtil.gray + "Click to select ban reason."));
        }

        player.openInventory(banMenu);
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
