package net.okocraft.playermanager.listener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
        String address = player.getAddress().getAddress().getHostName();
        String Uuid = player.getUniqueId().toString();
        String joinedPlayerName = player.getName();
        String beforePlayerName = playerTable.getPlayerData("player", Uuid);
        String oldAddress = playerTable.getPlayerData("address", Uuid);

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        if (!playerTable.existPlayer(Uuid)) {
            database.insert(playerTable.getPlayerTableName(), new HashMap<String, String>(){
                private static final long serialVersionUID = 1L;
                {
                    put("uuid", Uuid);
                    put("player", joinedPlayerName);
                    put("address", address);
                }
            });
        } else if (!joinedPlayerName.equalsIgnoreCase(beforePlayerName)) {
            onNameChanged(player, joinedPlayerName, beforePlayerName);
        }

        if (!oldAddress.equals(address))
            database.set(playerTable.getPlayerTableName(), "address", address, "address", oldAddress);

        playerTable.setPlayerData("address", Uuid, address);
        List<String> accounts = database.get(playerTable.getPlayerTableName(), "player", "address", address);
        if (accounts.size() > 1) {
            StringBuilder sb = new StringBuilder();

            accounts.forEach(alt -> {
                if (!alt.equalsIgnoreCase(joinedPlayerName)) {
                    sb.append(alt + ", ");
                }
            });

            Bukkit.getOnlinePlayers().stream()
                    .filter(onlinePlayer -> onlinePlayer.hasPermission("playermanager.alt.search"))
                    .forEach(onlinePlayer -> onlinePlayer
                            .sendMessage(config.getShowAltsOnJoinMsg().replaceAll("%player%", joinedPlayerName)
                                    .replaceAll("%alts%", sb.substring(0, sb.length() - 2).toString())));
        }

        String renameLogOnDate = playerTable.getPlayerData("renamelogondate", Uuid);

        try {
            LocalDateTime term = LocalDateTime.parse(renameLogOnDate, InventoryUtil.getFormat())
                    .plusDays(config.getNotifyPreviousNameTerm());
            if (config.isNotifyPreviousNameEnabled() && term.compareTo(now) >= 0) {
                Bukkit.broadcastMessage(config.getNotifyPreviousNameMsg()
                        .replaceAll("%oldname%", playerTable.getPlayerData("previous", Uuid))
                        .replaceAll("%player%", joinedPlayerName));
            }
        } catch (DateTimeParseException exception) {
            playerTable.setPlayerData("renamelogondate", Uuid, LocalDateTime.MIN.format(InventoryUtil.getFormat()));
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
            notifyNameChange(uuid, newName, oldName);
        if (config.isCommandExecutionEnabled())
            executeCommand(uuid, newName, oldName);
        if (config.isScoreMigrationEnabled())
            migrateScores(uuid, newName, oldName);
    }

    private void logNameChange(String uuid, String newName, String oldName) {
        try {
            File logFile = new File(PlayerManager.getInstance().getDataFolder().getPath() + "/log.txt");
            if (!logFile.exists())
                logFile.createNewFile();
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(logFile, true), "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            bw.write(uuid);
            bw.newLine();
            bw.write("    " + oldName + "->" + newName);
            bw.newLine();

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyNameChange(String uuid, String newName, String oldName) {
        nameChangeMsg = config.getNameChangeMsg().replaceAll("%oldname%", oldName).replaceAll("%newname%", newName);
        Bukkit.broadcastMessage(nameChangeMsg);
    }

    private void executeCommand(String uuid, String newName, String oldName) {
        commands = config.getCommandList();

        commands.forEach(command -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("&([a-f0-9])", "§$1")
                    .replaceAll("%oldname%", oldName).replaceAll("%newname%", newName));
        });
    }

    private void migrateScores(String uuid, String newName, String oldName) {
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