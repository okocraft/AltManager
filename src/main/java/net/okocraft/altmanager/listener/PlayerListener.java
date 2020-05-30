package net.okocraft.altmanager.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import net.okocraft.altmanager.AltManager;
import net.okocraft.altmanager.config.Config;
import net.okocraft.altmanager.config.Messages;
import net.okocraft.altmanager.database.Database;
import net.okocraft.altmanager.event.PlayerRenameLoginEvent;

public class PlayerListener implements Listener {
    
    private static final PlayerListener INSTANCE = new PlayerListener();

    private final Database database = AltManager.getInstance().getDatabase();
    private final Config config = Config.getInstance();

    private PlayerListener() {
    }

    public static void start() {
        stop();
        Bukkit.getPluginManager().registerEvents(INSTANCE, AltManager.getInstance());
    }

    public static void stop() {
        HandlerList.unregisterAll(INSTANCE);
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        int count = 0;
        String hostName = event.getAddress().getHostName();
        for (Player player : new HashSet<>(Bukkit.getOnlinePlayers())) {
            if (player.getAddress().getHostName().equals(hostName)) {
                count++;
            }
        }
        if (count > config.getMaxAccounts()) {
            event.setLoginResult(Result.KICK_OTHER);
            event.setKickMessage(ChatColor.RED + "Too many alts.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        handleIP(event);

        if (config.notifyPreviousName()) {
            notifyPreviousName(event);
        }
    }

    private void handleIP(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String address = player.getAddress().getAddress().getHostAddress();
        String uuid = player.getUniqueId().toString();
        String joinedPlayerName = player.getName();
        String beforePlayerName = database.getPlayerData("player", uuid);

        if (!database.existPlayer(uuid)) {
            database.addPlayer(uuid, joinedPlayerName);
            database.setPlayerData("address", uuid, address);
        } else if (!joinedPlayerName.equalsIgnoreCase(beforePlayerName)) {
            onNameChanged(player, joinedPlayerName, beforePlayerName);
        }

        String oldAddress = database.getPlayerData("address", uuid);
        if (!oldAddress.equals(address)) {
            database.updateAddress(oldAddress, address);
        }

        Collection<String> accounts = database.getAlts(address).values();
        if (accounts.size() > 1) {
            StringBuilder sb = new StringBuilder();

            accounts.forEach(alt -> {
                if (!alt.equalsIgnoreCase(joinedPlayerName)) {
                    sb.append(alt + ", ");
                }
            });


            Bukkit.getOnlinePlayers().stream()
                    .filter(onlinePlayer -> onlinePlayer.hasPermission("altmanager.alt.search"))
                    .forEach(onlinePlayer -> Messages.getInstance()
                            .sendAlts(onlinePlayer, joinedPlayerName, sb.substring(0, sb.length() - 2).toString()));
        }
    }

    private void notifyPreviousName(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        String renameLogOnDate = database.getPlayerData("renamelogondate", uuid);

        try {
            LocalDateTime term = LocalDateTime.parse(renameLogOnDate, AltManager.getTimeFormat())
                    .plusDays(config.getNotificationPreviousNameDays());
            if (term.compareTo(now) >= 0) {
                Bukkit.getOnlinePlayers().forEach(online -> 
                        Messages.getInstance().sendPreviousName(
                                online,
                                event.getPlayer().getName(),
                                database.getPlayerData("previous", uuid)
                        )
                );
            }
        } catch (DateTimeParseException exception) {
            database.setPlayerData("renamelogondate", uuid, LocalDateTime.MIN.format(AltManager.getTimeFormat()));
        }
    }

    private void onNameChanged(Player player, String newName, String oldName) {
        Bukkit.getPluginManager().callEvent(new PlayerRenameLoginEvent(player, oldName));
        String uuid = player.getUniqueId().toString();
        database.updateName(oldName, newName);

        if (config.isLoggingRename()) {
            logNameChange(uuid, newName, oldName);
        }
        
        if (config.notifyRename()) {
            Bukkit.getOnlinePlayers().forEach(online -> 
                    Messages.getInstance().sendRenameNotification(online, oldName, newName));
        }

        config.getCommandsOnRename().forEach(command -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command)
                    .replaceAll("%old-name%", oldName).replaceAll("%new-name%", newName));
        });
    }

    private void logNameChange(String uuid, String newName, String oldName) {
        try {
            Path log = AltManager.getInstance().getDataFolder().toPath().resolve("rename.log");
            Files.write(log,
                    (uuid + "\n" + String.format("%-16s", oldName) + " -> " + newName + "\n")
                            .getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}