package net.blazn.orin.engine.Utils;

import org.bukkit.ChatColor;

public class CenterUtil {

    private static final int MOTD_WIDTH = 52; // Estimated width in fixed-space characters
    private static final String SPACE = "\u2007"; // Unicode figure space

    public static String center(String message) {
        return center(message, 0);
    }

    public static String center(String message, int offset) {
        if (message == null || message.isEmpty()) return "";

        message = ChatColor.translateAlternateColorCodes('&', message);
        String stripped = ChatColor.stripColor(message);

        int visibleLength = stripped.length();

        if (visibleLength < 10) visibleLength += 1;

        int padding = ((MOTD_WIDTH - visibleLength) / 2) + offset;
        if (padding <= 0) return message;

        return SPACE.repeat(padding) + message;
    }
}
