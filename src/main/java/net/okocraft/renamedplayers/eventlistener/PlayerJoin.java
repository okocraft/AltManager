package net.okocraft.renamedplayers.eventlistener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import net.okocraft.renamedplayers.RenamedPlayerConfig;
import net.okocraft.renamedplayers.RenamedPlayers;
import net.okocraft.renamedplayers.database.Database;

public class PlayerJoin implements Listener {

    Database database;
    RenamedPlayerConfig config;

    private String messageNameChangeNotification;
    private List<String> commands;

    public PlayerJoin(Plugin plugin, Database database) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.database = database;
        this.config = new RenamedPlayerConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent playerjoinevent) {

        UUID joinedPlayerUUID = playerjoinevent.getPlayer().getUniqueId();
        String joinedPlayerName = playerjoinevent.getPlayer().getName();
        String beforePlayerName = database.readRecord(joinedPlayerUUID.toString(), "player");

        if (!database.hasRecord(joinedPlayerUUID.toString())) {
            database.addRecord(joinedPlayerUUID, joinedPlayerName);
        } else if (!joinedPlayerName.equalsIgnoreCase(beforePlayerName)) {
            onNameChanged(joinedPlayerUUID.toString(), joinedPlayerName, beforePlayerName);
        }
    }

    private void onNameChanged(String uuid, String newName, String oldName) {
        database.setRecord(uuid, "player", newName);

        if (config.isLoggingEnabled()) logNameChange(uuid, newName, oldName);

        if (config.isNotificationEnabled()) notifyNameChange(uuid, newName, oldName);

        if (config.isCommandExecutionEnabled()) executeCommand(uuid, newName, oldName);

        if (config.isScoreMigrationEnabled()) migrateScores(uuid, newName, oldName);

    }

    private void logNameChange(String uuid, String newName, String oldName) {
        try {
            File logFile = new File(RenamedPlayers.getInstance().getDataFolder().getPath() +"/log.txt");
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
            System.out.println(e);
        }
    }

    private void notifyNameChange(String uuid, String newName, String oldName) {

        messageNameChangeNotification = config.getMessageNameChangeNotification().replaceAll("%OLDNAME%", oldName).replaceAll("%NEWNAME%", newName);
        Bukkit.getOnlinePlayers().forEach(onlineplayer -> {
            onlineplayer.sendMessage(messageNameChangeNotification);
        });
    }

    private void executeCommand(String uuid, String newName, String oldName) {
        commands = config.getCommandList();

        commands.forEach(command -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    command.replaceAll("&", "§").replaceAll("%OLDNAME%", oldName).replaceAll("%NEWNAME%", newName));
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

        if (config.isOldScoreResetEnabled()) mainScoreboard.resetScores(oldName);
    }
}