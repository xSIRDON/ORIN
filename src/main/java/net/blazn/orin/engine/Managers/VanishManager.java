package net.blazn.orin.engine.Managers;

import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final JavaPlugin plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * ✅ Toggles vanish for a player.
     */
    public void toggleVanish(Player player) {
        if (!player.isOnline()) return; // Prevent toggling for offline players

        if (isVanished(player)) {
            unvanish(player);
        } else {
            vanish(player);
        }
    }

    /**
     * ✅ Vanishes a player.
     */
    public void vanish(Player player) {
        if (!player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        if (vanishedPlayers.add(uuid)) {
            // Apply invisibility potion effect
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false, false));
            player.sendMessage(ChatUtil.gold + "👁" + ChatUtil.white + " Vanish" + ChatUtil.darkGray + ": " + ChatUtil.bgreen + "ENABLED");
        }
    }

    /**
     * ✅ Unvanishes a player.
     */
    public void unvanish(Player player) {
        if (!player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        if (vanishedPlayers.remove(uuid)) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.sendMessage(ChatUtil.gold + "👁" + ChatUtil.white + " Vanish" + ChatUtil.darkGray + ": " + ChatUtil.bred + "DISABLED");
        }
    }

    /**
     * ✅ Checks if a player is vanished.
     */
    public boolean isVanished(Player player) {
        return player.isOnline() && vanishedPlayers.contains(player.getUniqueId());
    }

    /**
     * ✅ Force unvanish (used on logout or watchdog conflict)
     */
    public void forceUnvanish(Player player) {
        if (player.isOnline() && isVanished(player)) {
            vanishedPlayers.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    /**
     * ✅ Returns all vanished players who are currently online.
     */
    public Set<Player> getAllVanished() {
        Set<Player> onlineVanished = new HashSet<>();
        vanishedPlayers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null); // Clean up offline UUIDs
        for (UUID uuid : vanishedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) onlineVanished.add(player);
        }
        return onlineVanished;
    }
}
