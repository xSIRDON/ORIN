package net.blazn.orin.engine.Listeners.Staff;

import net.blazn.orin.engine.Managers.NameManager;
import net.blazn.orin.engine.Managers.RankManager;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class RankListener implements Listener {

    private final RankManager rankManager;
    private final NameManager nameManager;
    private final JavaPlugin plugin;

    public RankListener(JavaPlugin plugin, RankManager rankManager, NameManager nameManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.nameManager = nameManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Handles rank assignment via the rank selection GUI.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Ensure it's the correct inventory
        if (event.getCurrentItem() == null ||
                !event.getView().getTitle().equals(ChatUtil.bgold + "Select a Rank")) return;

        // ✅ Cancel the event to prevent multiple clicks
        event.setCancelled(true);

        // Ensure it's a left-click (prevents spam from shift-clicks, right-clicks)
        if (!event.isLeftClick()) return;

        ItemStack clickedItem = event.getCurrentItem();
        String selectedRank = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).toUpperCase();

        // Ensure rank is valid
        if (!rankManager.getRanks().contains(selectedRank)) return;

        player.closeInventory();

        // Retrieve stored target UUID from RankManager
        UUID targetUUID = rankManager.getStoredTargetUUID(player);

        if (targetUUID == null) {
            //player.sendMessage(ChatUtil.red +
            //        "❌ No target found! Ensure you opened the rank menu correctly.");
            return;
        }

        // ✅ Prevent double-clicks by immediately removing the UUID after retrieval
        rankManager.removeStoredTargetUUID(player);

        // Check if the player is online
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            // ✅ Apply the rainbow effect for DEVELOPER
            String formattedRank = selectedRank.equalsIgnoreCase("DEVELOPER")
                    ? ChatUtil.rainbowBold("DEVELOPER")
                    : rankManager.getRankColor(selectedRank) + selectedRank;
            String displayName = nameManager.getDisplayName(targetUUID);
            player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Successfully set " + displayName + "'s"
                    + ChatUtil.white + " rank to" + ChatUtil.darkGray + ": " + formattedRank);
            target.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Your rank has been set to"
                    + ChatUtil.darkGray + ": " + formattedRank);
            rankManager.setRank(target, selectedRank);
            return;
        }

        // Handle offline players asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // ✅ Apply the rainbow effect for DEVELOPER
            String formattedRank = selectedRank.equalsIgnoreCase("DEVELOPER")
                    ? ChatUtil.rainbowBold("DEVELOPER")
                    : rankManager.getRankColor(selectedRank) + selectedRank;
            String displayName = nameManager.getDisplayName(targetUUID);
            player.sendMessage(ChatUtil.green + "✔" + ChatUtil.white + " Successfully set " + displayName + "'s"
                    + ChatUtil.white + " rank to" + ChatUtil.darkGray + ": " + formattedRank);
            rankManager.setRank(targetUUID, selectedRank);
        });
    }

    /**
     * Handles player rank setup on join.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // ✅ Ensure player is registered in the database
        if (!nameManager.playerExists(uuid)) {
            nameManager.registerPlayer(player);
            plugin.getLogger().info("✅ Registered new player: " + player.getName());
        }

        // ✅ Ensure rank is set (only if missing)
        String rank = rankManager.getRank(uuid);
        if (rank == null || rank.isEmpty()) {
            rankManager.setRank(uuid, "MEMBER");
            plugin.getLogger().info("✅ Assigned default rank (MEMBER) to " + player.getName());
        }

        // ✅ Retrieve the stored nickname from database
        String storedNickname = nameManager.getStoredNickname(uuid);

        // ✅ Use nickname if it exists; otherwise, use real name
        String displayName = (storedNickname != null && !storedNickname.isEmpty()) ? storedNickname : player.getName();

        // ✅ Apply display name & scoreboard
        player.setDisplayName(displayName);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        // ✅ Update display name & nametag immediately
        rankManager.updateDisplayName(player, rank);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        //nameManager.removeNickname(uuid);
    }
}
