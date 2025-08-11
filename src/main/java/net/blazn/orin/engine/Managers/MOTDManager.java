package net.blazn.orin.engine.Managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class MOTDManager {

    private final JavaPlugin plugin;
    private String motd;
    private boolean center;
    private List<Integer> offsets;

    public MOTDManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMotdFromConfig();
    }

    private void loadMotdFromConfig() {
        FileConfiguration config = plugin.getConfig();
        this.motd = config.getString("motd.message", "&b&lORIN NETWORK\n&eGreatness awaits...");
        this.center = config.getBoolean("motd.center", true);
        this.offsets = config.getIntegerList("motd.offsets");
        if (this.offsets.isEmpty()) {
            this.offsets = List.of(0, 0); // fallback offsets
        }
    }

    public String getMotd() {
        return motd;
    }

    public boolean shouldCenter() {
        return center;
    }

    public List<Integer> getOffsets() {
        return offsets;
    }

    public void setMotd(String motd) {
        this.motd = motd;
        plugin.getConfig().set("motd.message", motd);
        plugin.saveConfig();
    }
}
