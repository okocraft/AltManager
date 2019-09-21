package net.okocraft.playermanager.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import net.okocraft.playermanager.PlayerManager;
import net.okocraft.playermanager.database.Database;
import net.okocraft.playermanager.database.PlayerTable;
import net.okocraft.playermanager.utilities.ConfigManager;
import net.okocraft.playermanager.utilities.InventoryUtil;

public class PlayerJoin implements Listener {

    private final Database database;
    private final PlayerTable playerTable;
    private final ConfigManager config;

    private String nameChangeMsg;
    private List<String> commands;

    public PlayerJoin(Plugin plugin, Database database) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.database = database;
        this.playerTable = database.getPlayerTable();
        this.config = ((PlayerManager) plugin).getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        String address = player.getAddress() == null ? "UNKNOWN" : player.getAddress().getAddress().getHostAddress();
        String uuid = player.getUniqueId().toString();
        String joinedPlayerName = player.getName();
        String beforePlayerName = playerTable.getPlayerData("player", uuid);

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        if (!playerTable.existPlayer(uuid)) {
            database.insert(playerTable.getPlayerTableName(), new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                {
                    put("uuid", uuid);
                    put("player", joinedPlayerName);
                    put("address", address);
                }
            });
        } else if (!joinedPlayerName.equalsIgnoreCase(beforePlayerName)) {
            onNameChanged(player, joinedPlayerName, beforePlayerName);
        }

        String oldAddress = playerTable.getPlayerData("address", uuid);
        if (!oldAddress.equals(address))
            database.set(playerTable.getPlayerTableName(), "address", address, "address", oldAddress);

        playerTable.setPlayerData("address", uuid, address);
        List<String> accounts = database.get(playerTable.getPlayerTableName(), "player", "address", address);
        if (accounts.size() > 1) {
            StringBuilder sb = new StringBuilder();

            accounts.forEach(alt -> {
                if (!alt.equalsIgnoreCase(joinedPlayerName)) {
                    sb.append(alt).append(", ");
                }
            });

            Bukkit.getOnlinePlayers().stream()
                    .filter(onlinePlayer -> onlinePlayer.hasPermission("playermanager.alt.search"))
                    .forEach(onlinePlayer -> onlinePlayer
                            .sendMessage(config.getShowAltsOnJoinMsg().replaceAll("%player%", joinedPlayerName)
                                    .replaceAll("%alts%", sb.substring(0, sb.length() - 2))));
        }

        String renameLogOnDate = playerTable.getPlayerData("renamelogondate", uuid);

        try {
            LocalDateTime term = LocalDateTime.parse(renameLogOnDate, InventoryUtil.getFormat())
                    .plusDays(config.getNotifyPreviousNameTerm());
            if (config.isNotifyPreviousNameEnabled() && term.compareTo(now) >= 0) {
                Bukkit.broadcastMessage(config.getNotifyPreviousNameMsg()
                        .replaceAll("%oldname%", playerTable.getPlayerData("previous", uuid))
                        .replaceAll("%player%", joinedPlayerName));
            }
        } catch (DateTimeParseException exception) {
            playerTable.setPlayerData("renamelogondate", uuid, LocalDateTime.MIN.format(InventoryUtil.getFormat()));
        }

    }

    private void onNameChanged(Player player, String newName, String oldName) {
        String uuid = player.getUniqueId().toString();
        Map<String, String> newValues = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("previous", oldName);
                put("player", newName);
                put("renamelogondate", LocalDateTime.now(ZoneId.systemDefault()).format(InventoryUtil.getFormat()));
            }
        };
        playerTable.setPlayerDataMultiValue(newValues, uuid);

        if (config.isLoggingEnabled())
            logNameChange(uuid, newName, oldName);
        if (config.isNotificationEnabled())
            notifyNameChange(newName, oldName);
        if (config.isCommandExecutionEnabled())
            executeCommand(newName, oldName);
        if (config.isScoreMigrationEnabled())
            migrateScores(newName, oldName);
    }

    private void logNameChange(String uuid, String newName, String oldName) {
        try {
            Path log = PlayerManager.getInstance().getDataFolder().toPath().resolve("rename.log");
            Files.createFile(log);
            Files.write(log,
                    (uuid + "\n" + String.format("%-16s", oldName) + " -> " + newName + "\n")
                            .getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyNameChange(String newName, String oldName) {
        nameChangeMsg = config.getNameChangeMsg().replaceAll("%oldname%", oldName).replaceAll("%newname%", newName);
        Bukkit.broadcastMessage(nameChangeMsg);
    }

    private void executeCommand(String newName, String oldName) {
        commands = config.getCommandList();

        commands.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("&([a-f0-9])", "§$1")
                .replaceAll("%oldname%", oldName).replaceAll("%newname%", newName)));
    }

    private void migrateScores(String newName, String oldName) {
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Set<Objective> Objectives = mainScoreboard.getObjectives();

        Objectives.forEach(objective -> {
            Score score = objective.getScore(oldName);
            if (score.isScoreSet())
                objective.getScore(newName).setScore(score.getScore());
        });

        if (config.isOldScoreResetEnabled())
            mainScoreboard.resetScores(oldName);
    }
}