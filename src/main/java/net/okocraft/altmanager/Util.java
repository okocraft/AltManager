package net.okocraft.altmanager;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class Util {
    private Util() {
    }

    public static String toUniqueId(String playerName) {
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        return offlinePlayer.hasPlayedBefore() ? offlinePlayer.getUniqueId().toString() : ""; 
    }

    public static String toPlayerName(String uniqueId) {
        OfflinePlayer p = getOfflinePlayer(uniqueId);
        if (p == null) {
            return "";
        }

        String name = p.getName();
        return name == null ? "" : name;
    }

    public static OfflinePlayer getOfflinePlayer(String uuidString) {
        try {
            OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(uuidString));
            return p.getName() == null ? null : p;
        } catch (IllegalArgumentException e) {
            return null;
        }  
    }
}