package net.blazn.orin.engine.Listeners.Staff;

import net.blazn.orin.engine.Managers.VanishManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class VanishListener implements Listener {

    private final VanishManager vanishManager;
    private final JavaPlugin plugin;

    public VanishListener(JavaPlugin plugin, VanishManager vanishManager) {
        this.vanishManager = vanishManager;
        this.plugin = plugin;
    }

    /**
     * ✅ Unvanish players automatically on quit.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        vanishManager.forceUnvanish(player);
    }

    /**
     * ✅ Unvanish players automatically if kicked.
     */
    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        vanishManager.forceUnvanish(player);
    }
}