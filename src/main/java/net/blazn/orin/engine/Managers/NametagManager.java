package net.blazn.orin.engine.Managers;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NametagManager {

    private final NameManager nameManager;
    private final JavaPlugin plugin;

    // Store player tags here
    private final Map<UUID, String> playerTags = new HashMap<>();

    public NametagManager(JavaPlugin plugin, NameManager nameManager) {
        this.nameManager = nameManager;
        this.plugin = plugin;
    }

    /**
     * Returns the map of player tags
     */
    public Map<UUID, String> getPlayerTags() {
        return playerTags;
    }

    /**
     * Add or update a player's tag
     */
    public void setPlayerTag(UUID uuid, String tag) {
        playerTags.put(uuid, tag);
    }

    /**
     * Get a player's tag (or null if not set)
     */
    public String getPlayerTag(UUID uuid) {
        return playerTags.get(uuid);
    }

    /**
     * Remove a player's tag
     */
    public void removePlayerTag(UUID uuid) {
        playerTags.remove(uuid);
    }
}
