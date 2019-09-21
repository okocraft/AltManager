package net.okocraft.playermanager;

import lombok.NonNull;
import net.okocraft.playermanager.database.PlayerTable;
import net.okocraft.playermanager.utilities.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerManagerAPI {

    private static final PlayerTable table = PlayerManager.getInstance().getDatabase().getPlayerTable();

    public static boolean addPlayer(@NonNull OfflinePlayer player, boolean showWarning) {
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        if (name == null) {
            name = "";
        }
        return table.addPlayer(uuid, name, showWarning);
    }

    public static boolean removePlayer(@NonNull OfflinePlayer player) {
        return table.removePlayer(player.getUniqueId().toString());
    }

    public static boolean existPlayer(@NonNull OfflinePlayer player) {
        return table.existPlayer(player.getUniqueId().toString());
    }

    public static String getPreviousName(OfflinePlayer player) {
        return table.getPlayerData("previous", player.getUniqueId().toString());
    }

    public static LocalDateTime getLastRenameLogonDate(OfflinePlayer player) {
        String date = table.getPlayerData("renamelogondate", player.getUniqueId().toString());
        return LocalDateTime.parse(date, InventoryUtil.getFormat());
    }

    private static String getAddress(OfflinePlayer player) {
        return table.getPlayerData("renamelogondate", player.getUniqueId().toString());
    }

    public static Set<OfflinePlayer> getAuthorizedAlts(OfflinePlayer player) {
        return table.getAuthorizedAlts(player.getUniqueId().toString()).stream()
                .map(uuidStr -> Bukkit.getOfflinePlayer(UUID.fromString(uuidStr))).collect(Collectors.toSet());
    }

    public static Set<OfflinePlayer> getAlts(OfflinePlayer player, boolean ignoreAuthorized) {
        Set<String> alts = new HashSet<>(PlayerManager.getInstance().getDatabase().get(table.getPlayerTableName(),
                "uuid", "address", getAddress(player)));
        if (ignoreAuthorized) {
            Set<String> ignored = table.getAuthorizedAlts(player.getUniqueId().toString());
            alts.removeAll(ignored);
        }
        return alts.stream().map(uuidStr -> Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)))
                .collect(Collectors.toSet());
    }
}