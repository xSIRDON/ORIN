package net.blazn.orin.engine.Listeners;

import net.blazn.orin.engine.Managers.MOTDManager;
import net.blazn.orin.engine.Utils.CenterUtil;
import net.blazn.orin.engine.Utils.ChatUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ServerListPingListener implements Listener {

    private final MOTDManager motdManager;
    private final JavaPlugin plugin;

    public ServerListPingListener(JavaPlugin plugin, MOTDManager motdManager) {
        this.plugin = plugin;
        this.motdManager = motdManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        String motd = motdManager.getMotd();

        if (motd == null || motd.isEmpty()) return;

        String[] lines = motd.split("\n");
        List<Integer> offsets = motdManager.getOffsets();

        StringBuilder finalMotd = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = ChatUtil.swapAmp(lines[i]);
            int offset = (i < offsets.size()) ? offsets.get(i) : 0;

            if (motdManager.shouldCenter()) {
                finalMotd.append(CenterUtil.center(line, offset)).append("\n");
            } else {
                finalMotd.append(line).append("\n");
            }
        }
        event.setMotd(finalMotd.toString().trim());
    }
}
