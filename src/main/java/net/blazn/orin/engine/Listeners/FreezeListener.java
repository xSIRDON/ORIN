package net.blazn.orin.engine.Listeners;

import net.blazn.orin.engine.Commands.Operator.FreezeCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;

public class FreezeListener implements Listener {

    private final FreezeCommand freezeCommand;
    private final JavaPlugin plugin;

    public FreezeListener(JavaPlugin plugin, FreezeCommand freezeCommand) {
        this.plugin = plugin;
        this.freezeCommand = freezeCommand;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Set<UUID> frozenPlayers = freezeCommand.getFrozenPlayers();

        if (frozenPlayers.contains(player.getUniqueId())) {
            // Completely cancel movement: x, y, z
            event.setTo(event.getFrom());
        }
    }
}
