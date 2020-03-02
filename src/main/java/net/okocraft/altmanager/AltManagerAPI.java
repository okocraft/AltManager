package net.okocraft.altmanager;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.okocraft.altmanager.database.Database;

public class AltManagerAPI {

    private static final Database database = AltManager.getInstance().getDatabase();

    public static boolean addPlayer(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        if (name == null) {
            name = "";
        }
        return database.addPlayer(uuid, name);
    }

    public static boolean removePlayer(OfflinePlayer player) {
        return database.removePlayer(player.getUniqueId().toString());
    }

    public static boolean existPlayer(OfflinePlayer player) {
        return database.existPlayer(player.getUniqueId().toString());
    }

    public static String getPreviousName(OfflinePlayer player) {
        return database.getPlayerData("previous", player.getUniqueId().toString());
    }

    public static LocalDateTime getLastRenameLogonDate(OfflinePlayer player) {
        String date = database.getPlayerData("renamelogondate", player.getUniqueId().toString());
        return LocalDateTime.parse(date, AltManager.getTimeFormat());
    }

    public static String getAddress(OfflinePlayer player) {
        return database.getPlayerData("renamelogondate", player.getUniqueId().toString());
    }

    public static Set<OfflinePlayer> getAuthorizedAlts(OfflinePlayer player) {
        return database.getAuthorizedAlts(player.getUniqueId().toString()).stream()
                .map(uuidStr -> Bukkit.getOfflinePlayer(UUID.fromString(uuidStr))).collect(Collectors.toSet());
    }

    public static Set<OfflinePlayer> getAlts(OfflinePlayer player, boolean ignoreAuthorized) {
        Set<String> alts = database.getAlts(getAddress(player)).keySet();
        if (ignoreAuthorized) {
            Set<String> ignored = database.getAuthorizedAlts(player.getUniqueId().toString());
            alts.removeAll(ignored);    
        }
        return alts.stream().map(Util::getOfflinePlayer).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}